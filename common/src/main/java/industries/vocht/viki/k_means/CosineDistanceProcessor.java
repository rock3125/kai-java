/*
 * Copyright (c) 2016 by Peter de Vocht
 *
 * All rights reserved. No part of this publication may be reproduced, distributed, or
 * transmitted in any form or by any means, including photocopying, recording, or other
 * electronic or mechanical methods, without the prior written permission of the publisher,
 * except in the case of brief quotations embodied in critical reviews and certain other
 * noncommercial uses permitted by copyright law.
 *
 */

package industries.vocht.viki.k_means;

import com.google.common.util.concurrent.RateLimiter;
import industries.vocht.viki.IDao;
import industries.vocht.viki.dao.DocumentDao;
import industries.vocht.viki.model.*;
import industries.vocht.viki.model.k_means.kMeansCluster;
import industries.vocht.viki.model.k_means.kMeansValue;
import industries.vocht.viki.model.similar.SimilarDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 17/04/16.
 *
 * is it time to process the vectors?
 *
 */
@Component
public class CosineDistanceProcessor implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(CosineDistanceProcessor.class);

    @Value("${system.cosine.vectorizer.active}")
    private boolean cosineProcessorActive;

    @Autowired
    private IDao dao;

    @Value("${cosine.threshold:0.1}")
    private double threshold;

    @Value("${cosine.run.every.x.minutes:60}")
    private int runEveryXMinutes;

    @Value("${cosine.allowed.per.second:100.0}")
    private double rateCalculationsPerSecond;

    @Value("${kmeans.cluster.size:20}")
    private int kClusterSize;

    public CosineDistanceProcessor() {
    }

    /**
     * setup the thread to run forever
     */
    public void init() {
        if ( cosineProcessorActive ) {
            logger.info("cosine vectorizer activated on this node");
            Thread thread = new Thread(this);
            thread.setName("cosine vectorizer");
            thread.start();
        } else {
            logger.info("cosine vectorizer not activated on this node");
        }
    }

    @Override
    public void run() {

        while ( true ) {

            try {

                // setup a rate limiter
                final RateLimiter rateLimiter = RateLimiter.create(rateCalculationsPerSecond);

                // log parameters
                logger.info( "cosine vectoriser rate     : " +  rateCalculationsPerSecond + " calculations per second");
                logger.info( "cosine vectoriser threshold: " +  threshold);
                logger.info( "cosine vectoriser wait-time: " +  runEveryXMinutes + " minutes");

                // for each organisation
                List<Organisation> organisationList = dao.getOrganisationDao().getOrganisationList();
                for (Organisation organisation : organisationList) {

                    logger.info( "staring cosine vectoriser for organisation \"" + organisation.getName() + "\"");

                    // time to do so for this organisation?
                    long lastClustered = dao.getClusterDao().getClusterLastClustered(organisation.getId());
                    long lastCosined = dao.getClusterDao().getCosineLastChange(organisation.getId());
                    if ( lastClustered > 0 && lastClustered > lastCosined ) {

                        // go through the documents that need processing
                        UUID organisation_id = organisation.getId();
                        DocumentDao documentDao = dao.getDocumentDao();

                        // get all the clusters
                        for (int i = 1; i <= kClusterSize; i++) {

                            kMeansCluster cluster = dao.getClusterDao().loadFullClusterItem(organisation_id, i);
                            if (cluster != null && cluster.getClusterContents() != null) {

                                logger.debug("cosine vectoriser kMeans cluster " + i + " for " + cluster.getClusterContents().size() + " urls");
                                HashSet<String> alreadyProcessed = new HashSet<>();

                                for (kMeansValue item1 : cluster.getClusterContents()) {

                                    List<SimilarDocument> similarDocumentList = new ArrayList<>();

                                    for (kMeansValue item2 : cluster.getClusterContents()) {

                                        // don't process the document against itself
                                        if ( !item1.getUrl().equals(item2.getUrl()) && !alreadyProcessed.contains(item1.getUrl()) &&
                                             !alreadyProcessed.contains(item2.getUrl()) ) {

                                            // limit the processing rate
                                            rateLimiter.acquire();

                                            // do the cosine thing
                                            double distance = calculateDocumentDistance(organisation_id, item1.getUrl(), item2.getUrl());
                                            if (distance <= threshold) { // worth keeping?
                                                similarDocumentList.add(new SimilarDocument(item1.getUrl(), item2.getUrl(), distance));
                                            }

                                        } // if items not equal urls


                                    } // for item2

                                    // done item1
                                    alreadyProcessed.add( item1.getUrl() );

                                    if (similarDocumentList.size() > 0) {
                                        logger.debug("cosine vectoriser: \"" + item1.getUrl() + "\" has " + similarDocumentList.size() + " similar document(s)");
                                        documentDao.saveDocumentSimilarityList(organisation_id, similarDocumentList);

                                        // keep track of what we've already processed - they can be skipped from now on
                                        for ( SimilarDocument document : similarDocumentList ) {
                                            alreadyProcessed.add( document.getUrl1() );
                                            alreadyProcessed.add( document.getUrl2() );
                                        }

                                    }

                                } // for item1


                            } // if cluster valid

                        } // for each cluster

                        // set the up-to-date for this cluster
                        dao.getClusterDao().setCosineLastChange( organisation_id, System.currentTimeMillis() );

                    } // if this organisation needs cosine processing

                } // for each organisation


            } catch (Exception ex) {
                logger.error("cosine vectorizer", ex);
            }

            // wait x minutes
            try {
                logger.debug("cosine vectoriser sleeping");
                Thread.sleep(runEveryXMinutes * 60_000); // converted to ms
            } catch (InterruptedException ex ){
                break;
            }

        }

    }



    /**
     * calculate the similarity between all documents
     * @param organisation_id the organisation owner of the documents
     * @param url1 the first url
     * @param url2 the other url
     * @return the similarity between two documents expressed as a cosine distance value
     * @throws IOException
     */
    public double calculateDocumentDistance(UUID organisation_id, String url1, String url2) throws IOException {

        CompressedVector vector1 = dao.getDocumentDao().loadDocumentHistogram(organisation_id, url1 );
        CompressedVector vector2 = dao.getDocumentDao().loadDocumentHistogram(organisation_id, url2 );

        if ( vector1 != null && vector2 != null ) {
            return calcCosineDistance( vector1.convert(), vector2.convert() );
        }

        // hopeless
        return Double.MAX_VALUE;
    }

    /**
     * distance between two vectors
     * @param vector1 first vector
     * @param vector2 second vector
     * @return distance
     */
    protected double calcCosineDistance(Vector vector1, Vector vector2) {
        return 1 - vector1.innerProduct(vector2) / vector1.norm() / vector2.norm();
    }

    /**
     * distance between two vectors
     * @param vector1 first vector
     * @param vector2 second vector
     * @return distance
     */
    protected double calcJaccardDistance(Vector vector1, Vector vector2) {
        double innerProduct = vector1.innerProduct(vector2);
        return Math.abs(1 - innerProduct / (vector1.norm() + vector2.norm() - innerProduct));
    }


}


