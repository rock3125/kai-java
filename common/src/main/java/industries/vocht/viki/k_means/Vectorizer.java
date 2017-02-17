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

import industries.vocht.viki.IDao;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.lexicon.Lexicon;
import industries.vocht.viki.lexicon.ValidPennTypes;
import industries.vocht.viki.model.CompressedVector;
import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.Vector;
import industries.vocht.viki.model.k_means.kMeansCluster;
import industries.vocht.viki.model.k_means.kMeansClusterInterim;
import industries.vocht.viki.model.k_means.kMeansValue;
import industries.vocht.viki.utility.SentenceFromBinary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * Created by peter on 15/06/16.
 *
 * helper utilities for vectorization
 *
 */
@Component
public class Vectorizer {

    @Autowired
    private Lexicon lexicon;

    @Autowired
    private IDao dao;

    @Value("${kmeans.cluster.size:20}")
    private int kClusterSize;

    // valid penn type detector
    private static ValidPennTypes validPennTypes = new ValidPennTypes();


    public Vectorizer() {
    }

    /**
     * calculate a compressed vector for a given document content
     * @param sentenceList the list of sentence to calculate a vector for
     * @return the compressed vector for this document (or null if dne)
     * @throws IOException
     */
    public CompressedVector getCompressedVector( List<Sentence> sentenceList ) throws IOException {
        return vectorizeDocument( sentenceList );
    }

    /**
     * calculate a compressed vector for a given document content
     * @param organisation_id the organisation
     * @param url the url of the document
     * @return the compressed vector for this document (or null if dne)
     * @throws IOException
     */
    public CompressedVector getCompressedVector( UUID organisation_id, String url ) throws IOException {
        SentenceFromBinary sentenceFromBinary = new SentenceFromBinary();
        Map<String, byte[]> documentMap = dao.getDocumentDao().getDocumentParseTreeMap(organisation_id, url);
        if ( documentMap != null && documentMap.containsKey(Document.META_BODY) ) {
            List<Sentence> sentenceList = sentenceFromBinary.convert(documentMap.get(Document.META_BODY));
            return vectorizeDocument( sentenceList );
        }
        return null;
    }

    /**
     * calculate a vector for a given list of sentence
     * @param sentenceList the document as a list of sentences
     * @return the vector for this document (or null if dne)
     * @throws IOException
     */
    public Vector getFullVector( List<Sentence> sentenceList ) throws IOException {
        return calcHistogramForTree( sentenceList );
    }

    /**
     * calculate a vector for a given document content
     * @param organisation_id the organisation
     * @param url the url of the document
     * @return the vector for this document (or null if dne)
     * @throws IOException
     */
    public Vector getFullVector( UUID organisation_id, String url ) throws IOException {
        SentenceFromBinary sentenceFromBinary = new SentenceFromBinary();
        Map<String, byte[]> documentMap = dao.getDocumentDao().getDocumentParseTreeMap(organisation_id, url);
        if ( documentMap != null && documentMap.containsKey(Document.META_BODY) ) {
            List<Sentence> sentenceList = sentenceFromBinary.convert(documentMap.get(Document.META_BODY));
            return calcHistogramForTree( sentenceList );
        }
        return null;
    }

    /**
     * return the compressed histogram vector for a document
     * @param sentenceList the parse-tree list of the document
     * @return the compressed-vector
     * @throws IOException
     */
    private CompressedVector vectorizeDocument(List<Sentence> sentenceList) throws IOException {
        if ( sentenceList != null ) {
            // calculate vectors for this parse-tree list
            Vector historgram = calcHistogramForTree(sentenceList);
            if ( historgram != null ) {
                return new CompressedVector(historgram);
            }
        }
        return null;
    }

    /**
     * distance between two vectors
     * @param vector1 first vector
     * @param vector2 second vector
     * @return distance
     */
    public double calcCosineDistance(Vector vector1, Vector vector2) {
        if (vector1 != null && vector2 != null ) {
            return 1 - vector1.innerProduct(vector2) / vector1.norm() / vector2.norm();
        }
        return -1.0;
    }

    /**
     * get a list of document closest to the vector using the clustering system
     * @param organisation_id the organisation ID
     * @param vector the vector of the document to look for
     * @return a list of closest documents
     * @throws IOException
     */
    public List<kMeansValue> getClosestDocumentThroughCluster( UUID organisation_id, Vector vector ) throws IOException {
        kMeansCluster closest = getClosestKMeansCluster( organisation_id, vector );
        List<kMeansValue> closestDocumentList = new ArrayList<>();
        SentenceFromBinary sentenceFromBinary = new SentenceFromBinary();
        if ( closest != null && closest.getClusterContents() != null ) {
            for ( kMeansValue kMeansValue : closest.getClusterContents() ) {
                Map<String, byte[]> document = dao.getDocumentDao().getDocumentParseTreeMap(organisation_id, kMeansValue.getUrl() );
                List<Sentence> sentenceList2 = sentenceFromBinary.convert(document.get(Document.META_BODY));
                Vector vector2 = calcHistogramForTree(sentenceList2);
                double dist = calcCosineDistance(vector, vector2);
                // filter by threshold
                closestDocumentList.add(new kMeansValue(kMeansValue.getUrl(), dist, 0.0, 0.0));
            }
        }
        if ( closestDocumentList.size() > 0 ) {
            Collections.sort(closestDocumentList);
        }
        return closestDocumentList;
    }

    /**
     * get the closest k-means cluster set
     * @param organisation_id the organisation to get the set for
     * @return the set of clusters for this organisation sorted by frequency desc.
     * @throws IOException
     */
    private kMeansCluster getClosestKMeansCluster(UUID organisation_id, Vector vector ) throws IOException {
        kMeansCluster closest = null;
        if ( organisation_id != null && vector != null ) {
            // get all the clusters and return them
            double closestDistance = Double.MAX_VALUE;
            for (int i = 1; i <= kClusterSize; i++) {
                kMeansCluster cluster = dao.getClusterDao().loadFullClusterItem(organisation_id, i);
                if (cluster != null && cluster.getCentroid() != null) {
                    double dist = calcCosineDistance( cluster.getCentroid().convert(), vector );
                    if ( dist < closestDistance ) {
                        closestDistance = dist;
                        closest = cluster;
                    }
                }
            }
        }
        return closest;
    }

    /**
     * calculate a histogram (frequencies) for a parsed document
     * @param sentenceList the parsed document
     * @return a histogram of / for this document parse-tree
     */
    private Vector calcHistogramForTree(List<Sentence> sentenceList) {
        if ( sentenceList != null ) {
            // go through each item and add to the histogram
            Vector histogram = new Vector(kMeansClusterInterim.VECTOR_SIZE);
            processHistogram( sentenceList, histogram);
            return histogram;
        }
        return null;
    }

    /**
     * turn the parse-tree into a histogram
     * @param sentenceList the tree to convert
     * @param histogram the destination / histogram vector
     */
    private void processHistogram(List<Sentence> sentenceList, Vector histogram) {
        for (Sentence sentence : sentenceList) {
            List<Token> tokenList = sentence.getTokenList();
            for (Token item : tokenList) {
                if ( item.getGrammarRuleName() == null && validPennTypes.isValidPennType(item.getPennType()) ) {
                    String stemmed = lexicon.getStem(item.getText());
                    histogram.increment(hashWord(stemmed.toLowerCase()));
                }
            }
        }
    }

    /**
     * Hash word into integer between 0 and numFeatures - 1. Used to form document feature vector.
     */
    private int hashWord(String word) {
        return Math.abs(word.toLowerCase().hashCode()) % kMeansClusterInterim.VECTOR_SIZE;
    }



}
