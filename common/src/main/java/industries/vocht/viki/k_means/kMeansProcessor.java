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

import Jama.Matrix;
import com.carrotsearch.hppc.IntHashSet;
import com.google.common.util.concurrent.RateLimiter;
import industries.vocht.viki.IDao;
import industries.vocht.viki.lexicon.Undesirables;
import industries.vocht.viki.model.*;
import industries.vocht.viki.model.Vector;
import industries.vocht.viki.model.k_means.kMeansClusterInterim;
import industries.vocht.viki.model.k_means.kMeansValue;
import industries.vocht.viki.model.summary.SummarisationFragment;
import industries.vocht.viki.model.summary.SummarisationSet;
import industries.vocht.viki.pca.PCA;
import industries.vocht.viki.tokenizer.Tokenizer;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by peter on 28/04/16.
 *
 */
public class kMeansProcessor implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(kMeansProcessor.class);

    @Autowired
    private IDao dao;

    @Value("${system.kmeans.active}")
    private boolean kMeansActive;

    @Value("${kmeans.threshold:0.3}")
    private double threshold;

    @Value("${kmeans.run.every.x.minutes:60}")
    private int runEveryXMinutes;

    @Value("${kmeans.page.size:100}")
    private int pageSize;

    @Value("${kmeans.cluster.size:5}")
    private int kClusterSize;

    @Value("${kmeans.cluster.pca.init.size:200}")
    private int kClusterPCAInitSize;

    @Value("${kmeans.cluster.min.anomaly.size:2}")
    private int kMinClusterSizeForAnonalies;

    @Value("${kmeans.iterations:100}")
    private int iterations;

    // 68–95–99.7 rule  (1 - 2 - 3 standard devations)
    @Value("${kmeans.anomaly.standard.deviation.multiplier:3}")
    private double numDeviations;

    @Value("${kmeans.allowed.per.second:100.0}")
    private double rateCalculationsPerSecond;

    // how many top description items to keep
    @Value("${kmeans.word.description.size:20}")
    private int clusterSummaryWordCount;

    // how many summary items to read
    @Value("${kmeans.summary.item.count:20}")
    private int clusterSummaryItemCount;

    // rnd generator
    private Random rnd;


    public kMeansProcessor() {
        rnd = new Random();
    }

    /**
     * setup the thread to run forever
     */
    public void init() {
        if ( kMeansActive ) {
            logger.info("k-means system activated on this node");
            Thread thread = new Thread(this);
            thread.setName("k-means vectorizer");
            thread.start();
        } else {
            logger.info("k-means system not activated on this node");
        }
    }

    @Override
    public void run() {

        while ( true ) {

            try {

                // setup a rate limiter
                final RateLimiter rateLimiter = RateLimiter.create(rateCalculationsPerSecond);

                // log parameters
                logger.info( "k-means rate          : " +  rateCalculationsPerSecond + " calculations per second");
                logger.info( "k-means threshold     : " +  threshold);
                logger.info( "k-means wait-time     : " +  runEveryXMinutes + " minutes");
                logger.info( "k-means page-size     : " +  pageSize);
                logger.info( "k-means summary-items : " + clusterSummaryItemCount);
                logger.info( "k-means summary-size  : " + clusterSummaryWordCount);

                // for each organisation
                List<Organisation> organisationList = dao.getOrganisationDao().getOrganisationList();
                for (Organisation organisation : organisationList) {
                    clusterDataForOrganisation( organisation, rateLimiter );
                } // for each organisation


            } catch (Exception ex) {
                logger.error("k-means vectorizer", ex);
            }

            // wait x minutes
            try {
                logger.debug("k-means sleeping for " + runEveryXMinutes + " minutes");
                Thread.sleep(runEveryXMinutes * 60_000); // converted to ms
            } catch (InterruptedException ex ){
                break;
            }

        }

    }

    /**
     * cluster all data for a single organisation
     * @param organisation the organisation whose data to cluster
     * @param rateLimiter a rate limiter to make sure we don't go too fast
     */
    private void clusterDataForOrganisation( Organisation organisation, RateLimiter rateLimiter ) throws IOException {

        logger.info( "staring k-means for organisation \"" + organisation.getName() + "\"");

        if ( !isUpToDate(organisation) ) {

            logger.info("k-means, loading all document urls");
            List<String> urlList = loadAllUrls(organisation.getName(), organisation.getId());

            // skip if the system doesn't have enough documents
            if (urlList == null || urlList.size() < kClusterSize ) {
                logger.info("no enough documents for k-Means, organisation \"" + organisation.getName() + "\"");
            } else {

                kMeansClusterInterim[] clusterSet = performKMeans(organisation, rateLimiter, urlList );

                if ( clusterSet != null ) {

                    // work out the summary information for each cluster item and
                    // save the clusters that have summary information
                    saveKMeans( organisation, clusterSet );

                    // save any items that lie on the fringes of the cluster as anomalies
                    saveAnomalies( organisation, clusterSet );

                } // if completed cluster find successfully

            } // if has documents to cluster

        } // if not up-to-date
    }

    /**
     * check if an organisation needs re-clustering or not
     * @param organisation the organisation to check
     * @return true if the cluster is up-to-date, false if it needs re-clustering
     */
    private boolean isUpToDate(Organisation organisation) {
        // is this cluster already up-to-date?  then skip it
        long lastChange = dao.getClusterDao().getClusterLastChange(organisation.getId());
        long lastClustered = dao.getClusterDao().getClusterLastClustered(organisation.getId());
        if ( lastClustered > 0 && lastChange <= lastClustered  ) {
            if ( lastChange > 0 && lastClustered > 0 ) {
                Format format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String lastChangedDateTime = format.format(new DateTime(lastChange).toDate());
                String lastClusteredDateTime = format.format(new DateTime(lastClustered).toDate());
                logger.info("k-means organisation \"" + organisation.getName() + "\" last clustered on: " + lastClusteredDateTime + ", last changed on: " + lastChangedDateTime);
            }
            return true; // skip it
        }

        if ( lastClustered == 0L ) {
            logger.info("k-means organisation \"" + organisation.getName() + "\" clustering for the first time");
        } else {
            if ( lastChange > 0 && lastClustered > 0 ) {
                Format format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String lastChangedDateTime = format.format(new DateTime(lastChange).toDate());
                String lastClusteredDateTime = format.format(new DateTime(lastClustered).toDate());
                logger.info("k-means organisation \"" + organisation.getName() + "\" last clustered on: " + lastClusteredDateTime + ", then changed on: " + lastChangedDateTime);
            }
        }
        return false;
    }


    /**
     * perform a k-means clustering for an organisation
     * @param organisation the organisation to cluster
     * @param rateLimiter the rate limiter
     * @param urlList the list of URLs, all the documents of this organisation
     * @return a set of interim clusters
     */
    private kMeansClusterInterim[] performKMeans( Organisation organisation, RateLimiter rateLimiter, List<String> urlList) throws IOException {

        // part 1.  initialise k clusters
        IntHashSet chosenCentroids = new IntHashSet(kClusterSize);
        kMeansClusterInterim[] clusterSet = new kMeansClusterInterim[kClusterSize];
        int counter = kClusterSize - 1;
        int totalMisses = 0;
        while (counter >= 0) {
            clusterSet[counter] = new kMeansClusterInterim();
            int randomIndex = Math.abs(rnd.nextInt(urlList.size()));
            if (!chosenCentroids.contains(randomIndex)) {
                totalMisses = 0;
                chosenCentroids.add(randomIndex);
                String url = urlList.get(randomIndex);
                CompressedVector vector = dao.getDocumentDao().loadDocumentHistogram(organisation.getId(), url);
                if (vector != null && !vector.isEmpty()) {
                    clusterSet[counter].setCentroid(vector.convert());
                    counter = counter - 1;
                }
            } else {
                totalMisses = totalMisses + 1;
            }
            // can't find a free slot?
            if (totalMisses > kClusterSize) {
                break;
            }
        }

        // make sure that we did find all items / histograms of a cluster - otherwise we
        // can't complete this set (yet, until we get more data)
        if (totalMisses < kClusterSize) {

            int numCluserIterations = 0;
            for (int i = 0; i < iterations; i++) {

                numCluserIterations++;
                logger.debug("k-means iteration " + (i + 1));

                // part 2.  go through all the items and add them to clusters
                for (String url : urlList) {

                    rateLimiter.acquire(); // make sure we don't go too fast

                    // load the current url
                    CompressedVector vector = dao.getDocumentDao().loadDocumentHistogram(organisation.getId(), url);
                    if (vector != null && !vector.isEmpty()) {
                        Vector convertedVector = vector.convert();

                        int closest = -1;
                        double distance = Double.MAX_VALUE;
                        for (int cluster = 0; cluster < kClusterSize; cluster++) {
                            double d = clusterSet[cluster].distance(convertedVector);
                            if (d < distance) {
                                distance = d;
                                closest = cluster;
                            }
                        } // for each cluster


                        // add its centroid this cluster
                        clusterSet[closest].addDistanceUrl(url, distance);
                        clusterSet[closest].addToNextCentroid(convertedVector);

                    }

                } // for each url

                // part 3, re-evaluate the centroids - but not on the last iteration
                boolean changed = false;
                for (int cluster = 0; cluster < kClusterSize; cluster++) {
                    if (clusterSet[cluster].reEstablishCentroid()) {
                        changed = true;
                    }
                }

                // end of changes?
                if (!changed) {
                    break;
                }

            } // for each iteration

            logger.debug("k-means stabilized after " + numCluserIterations + " iterations");

            // re-establish each cluster's data
            for (kMeansClusterInterim cluster : clusterSet) {
                cluster.finishCentroid();
            }

            // do the PCA thing where we calculate the (x,y) of a cluster item
            logger.debug("k-means calculating PCA for clusters");
            for (int cluster = 0; cluster < kClusterSize; cluster++) {
                calculatePCACoordinates( organisation, rateLimiter, clusterSet[cluster].getClusterContents() );
            }

            return clusterSet;
        }
        return null;
    }

    /**
     * if possible, transform the urls for the clusters into (x,y) coordinates
     * inside the diagram using PCA
     * @param organisation the organisation
     * @param rateLimiter the rate limiter for fair speeds and resource use
     * @param clusterList the list of values that make this cluster
     */
    private void calculatePCACoordinates(Organisation organisation,
                                         RateLimiter rateLimiter,
                                         List<kMeansValue> clusterList ) throws IOException {

        // collect the urls to calculate the pca coordiantes from
        List<kMeansValue> clusterValuesToUse = new ArrayList<>();
        if ( clusterList.size() > (kClusterPCAInitSize/2) && clusterList.size() <= kClusterPCAInitSize ) {
            clusterValuesToUse.addAll(clusterList);
        } else if ( clusterList.size() > kClusterPCAInitSize ) {
            // pick a random set
            IntHashSet chosenCentroids = new IntHashSet(kClusterPCAInitSize);
            int counter = kClusterPCAInitSize;
            while (counter >= 0) {
                int randomIndex = Math.abs(rnd.nextInt(clusterList.size()));
                if (!chosenCentroids.contains(randomIndex)) {
                    chosenCentroids.add(randomIndex);
                    clusterValuesToUse.add( clusterList.get(randomIndex) );
                    counter = counter - 1;
                }
            }
        }

        // construct a PCA matrix
        List<CompressedVector> vectorList = new ArrayList<>();
        for ( kMeansValue value : clusterValuesToUse ) {
            rateLimiter.acquire();
            CompressedVector vector = dao.getDocumentDao().loadDocumentHistogram(organisation.getId(), value.getUrl());
            if (vector != null && !vector.isEmpty()) {
                vectorList.add( vector );
            }
        }

        // must at least have 50% of the required vectors
        if ( vectorList.size() > (kClusterPCAInitSize /2 ) ) {
            double[][] pcaInitMatrix = new double[vectorList.size()][];
            for ( int i = 0; i < vectorList.size(); i++ ) {
                pcaInitMatrix[i] = vectorList.get(i).convert().getAsArray();
            }
            Matrix trainingData = new Matrix(pcaInitMatrix);
            PCA pca = new PCA(trainingData);

            // transform each vector in this cluster using this pca
            for ( kMeansValue value : clusterList ) {
                rateLimiter.acquire();
                CompressedVector vector = dao.getDocumentDao().loadDocumentHistogram(organisation.getId(), value.getUrl());
                if (vector != null && !vector.isEmpty()) {
                    Matrix testData = new Matrix(new double[][] {vector.convert().getAsArray()});
                    Matrix transformedData = pca.transform(testData, PCA.TransformationType.WHITENING);
                    double x = transformedData.get(0, 0);
                    double y = transformedData.get(0, 1);
                    value.setX(x);
                    value.setY(y);
                }
            }

        } // if vectorList is of sufficient size

    }

    /**
     * save the k-means data to our data-store
     * @param organisation the organisation in question
     * @param clusterSet its clustered documents
     */
    private void saveKMeans( Organisation organisation, kMeansClusterInterim[] clusterSet) throws IOException {

        // count the number of values
        int numNonZeroValues = 0;
        for (int cluster = 0; cluster < kClusterSize; cluster++) {
            List<kMeansValue> valueList = clusterSet[cluster].getClusterContents();
            if (valueList != null && valueList.size() > 0) {
                numNonZeroValues = numNonZeroValues + 1;
            }
        }

        // get some summary data from the cluster to get some words representing the urls in each cluster
        // and save the cluster
        logger.debug("k-means saving " + numNonZeroValues + " non-zero clusters, fetching summarisation data");
        for (int cluster = 0; cluster < kClusterSize; cluster++) {

            summarizeCluster( organisation, cluster, clusterSet );

            // save the cluster if it has some description (to be useful)
            if ( clusterSet[cluster].getClusterDescription().size() > 0 ) {
                dao.getClusterDao().saveCluster(organisation.getId(), cluster, clusterSet[cluster].convert());
                dao.getClusterDao().setClusterLastClustered(organisation.getId(), System.currentTimeMillis());
            }

        } // for each cluster
    }

    /**
     * perform a text summary of the cluster by taking its top items, working out frequencies
     * on their summary data and providing this as a list of words for the current cluster item
     * @param organisation the organisation owner
     * @param cluster the cluster index, current cluster to process
     * @param clusterSet the set of cluster items
     */
    private void summarizeCluster( Organisation organisation, int cluster, kMeansClusterInterim[] clusterSet ) throws IOException {

        Undesirables undesirables = new Undesirables();

        // sort by distance from centroid
        Map<String, Integer> summarySet = new HashMap<>();
        List<kMeansValue> valueList = clusterSet[cluster].getClusterContents();
        Collections.sort(valueList);

        // get a half-way point between the closest and the furthrest away
        double halfDistance = (valueList.get(0).getDistance() + valueList.get(valueList.size() - 1).getDistance()) * 0.5;
        int numItemsProcessed = 0;
        int size = valueList.size();
        for (int i = 0; i < size; i++) {
            kMeansValue value = valueList.get((size - 1) - i);
            if (value.getDistance() <= halfDistance) {
                String url = value.getUrl();
                Sentence sentence = dao.getDocumentDao().loadDocumentSummarizationSentenceSet(organisation.getId(), url);
                if (sentence != null) {
                    numItemsProcessed = numItemsProcessed + 1;
                    for (Token token : sentence.getTokenList()) {
                        String str = token.getText();
                        if (!undesirables.isUndesirable(str)) {
                            Integer countValue = summarySet.get(str);
                            if (countValue == null) {
                                summarySet.put(str, 1);
                            } else {
                                summarySet.put(str, countValue + 1);
                            }
                        } // if desirable
                    } // for each fragment
                } // if set != null

                if (numItemsProcessed > clusterSummaryItemCount) {
                    break;
                }
            }
        } // for each item in the cluster

        // sory summary data by frequency - temporary list only
        List<kMeansValue> newValueList = new ArrayList<>();
        for (String key : summarySet.keySet()) {
            newValueList.add(new kMeansValue(key, summarySet.get(key), 0.0, 0.0));
        }
        Collections.sort(newValueList);

        // make sure we don't exceed the maximum allowed words for a summary
        if (newValueList.size() > clusterSummaryWordCount) {
            newValueList = newValueList.subList(0, clusterSummaryWordCount);
        }
        // set the summary items into the cluster information
        for (kMeansValue value : newValueList) {
            clusterSet[cluster].addSummaryKeyword(value.getUrl());
        }
    }

    /**
     * look for outliers in a cluster set and save them as anomalies to the cluster
     * @param organisation the organisation in question
     * @param clusterSet the cluster set to analyze for anomalies
     */
    private void saveAnomalies(Organisation organisation, kMeansClusterInterim[] clusterSet) {
        // next - for each cluster - get the anomalous urls - the ones that lie outside the
        // norms, and clusters smaller than a certain size
        List<String> anomalyList = new ArrayList<>();
        for (int cluster = 0; cluster < kClusterSize; cluster++) {
            List<kMeansValue> valueList = clusterSet[cluster].getClusterContents();
            // any cluster below a pre-defined size
            if (valueList.size() <= kMinClusterSizeForAnonalies) {
                for (kMeansValue value : valueList) {
                    anomalyList.add(value.getUrl());
                }
            } else {
                SummaryStatistics summaryStatistics = new SummaryStatistics();
                for (kMeansValue value : valueList) {
                    summaryStatistics.addValue(value.getDistance());
                }
                double mean = summaryStatistics.getMean();
                double stdCutoff = summaryStatistics.getStandardDeviation() * numDeviations;
                for (kMeansValue value : valueList) {
                    double v = value.getDistance();
                    if (v > mean) {
                        double delta = Math.sqrt((mean - v) * (mean - v));
                        if (delta >= stdCutoff) {
                            anomalyList.add(value.getUrl());
                        }
                    }
                } // for each value in the k-means set

            } // else

        } // for each cluster

        // save the anomalous values
        Collections.sort(anomalyList);
        dao.getStatisticsDao().saveDocumentAnomalies(organisation.getId(), anomalyList);
    }

    /**
     * return a list of ALL the urls in the system
     * @param organisation_id the organisation to do it for
     * @return a list of all urls in that organsiation
     */
    private List<String> loadAllUrls( String name, UUID organisation_id ) throws IOException {
        List<String> urlList = new ArrayList<>();
        logger.debug("loading organisation document urls for " + name );
        String prevDocUrl;
        List<String> otherDocumentList = dao.getDocumentDao().getDocumentUrlList(organisation_id, null, pageSize);
        while (otherDocumentList != null && otherDocumentList.size() > 0) {
            urlList.addAll( otherDocumentList );
            prevDocUrl = otherDocumentList.get( otherDocumentList.size() - 1 );
            // did we find any similar documents?
            otherDocumentList = dao.getDocumentDao().getDocumentUrlList(organisation_id, prevDocUrl, pageSize); // next set

        } // while more main documents
        logger.debug("loaded " + urlList.size() + " urls for " + name );
        return urlList;
    }


}


