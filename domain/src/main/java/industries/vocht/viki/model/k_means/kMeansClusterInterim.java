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

package industries.vocht.viki.model.k_means;


import industries.vocht.viki.model.CompressedVector;
import industries.vocht.viki.model.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by peter on 28/04/16.
 *
 * a simple k-means cluster, centroid with grouped items
 *
 */
public class kMeansClusterInterim {

    public final static int VECTOR_SIZE = 10_000;

    private Vector centroid; // current centroid
    private Vector tempCentroid; // accumulator
    private List<kMeansValue> clusterContents; // the members and their distance to the centroid
    private List<kMeansValue> currentClusterContents; // the members and their distance to the centroid
    private List<String> clusterDescription; // list of words describing this cluster

    public kMeansClusterInterim() {
        this.centroid = new Vector(VECTOR_SIZE);
        this.tempCentroid = new Vector(VECTOR_SIZE);
        this.clusterContents = new ArrayList<>();
        this.clusterDescription = new ArrayList<>();
        this.currentClusterContents = new ArrayList<>();
    }

    /**
     * convert this interim cluster form to the final "to save and load" cluster form
     * @return the converted stable cluster form
     */
    public kMeansCluster convert() {
        kMeansCluster cluster = new kMeansCluster();
        cluster.setCentroid( new CompressedVector(centroid) );
        if ( this.getClusterContents() != null ) {
            for ( kMeansValue cluster1 : this.getClusterContents() ) {
                cluster.getClusterContents().add( new kMeansValue(cluster1.getUrl(),
                        Math.sqrt(cluster1.getDistance()), cluster1.getX(), cluster1.getY() ) );
            }
        }
        cluster.getClusterDescription().addAll( this.getClusterDescription() );
        return cluster;
    }

    /**
     * @return access the centroid vector
     */
    public Vector getCentroid() {
        return centroid;
    }

    /**
     * re-setup the centroid's centre
     * @return return true if there were any changes - otherwise false
     */
    public boolean reEstablishCentroid() {

        // zero old centroid
        centroid.clear();

        // any changes?  take the previous set of collected urls and compare them with the current set
        boolean changed = false;
        if ( clusterContents.size() == currentClusterContents.size() ) {
            HashSet<String> detectionSet = clusterContents.stream().map(kMeansValue::getUrl).collect(Collectors.toCollection(HashSet::new));
            for ( kMeansValue value : currentClusterContents ) {
                if ( !detectionSet.contains(value.getUrl() ) ) {
                    changed = true;
                    break;
                }
            }
        } else {
            changed = true;
        }

        // calculate new centroid
        currentClusterContents.clear();
        if ( clusterContents.size() > 0 ) {
            tempCentroid.divideThis( (double)clusterContents.size() );
            centroid.copyValues(tempCentroid);
            currentClusterContents.addAll( clusterContents ); // copy
            clusterContents.clear();
        }

        // zero next accumulator
        tempCentroid.clear();

        return changed;
    }

    /**
     * after the system stabalises setup the centroid
     */
    public void finishCentroid() {
        clusterContents.clear();
        clusterContents.addAll( currentClusterContents ); // copy
    }

    /**
     * add a vector to the temp-centroid for creating the next cluster
     * @param vector the vector to add
     */
    public void addToNextCentroid( Vector vector ) {
        this.tempCentroid.add(vector);
    }

    /**
     * the center of the cluster
     * @param centroid the center of the cluster
     */
    public void setCentroid(Vector centroid) {
        this.centroid = centroid;
    }

    /**
     * add a new url / distance to the cluster as part of the cluster
     * @param url the url of the document part of this cluster
     * @param distance the distance of this url to the cluster centroid
     */
    public void addDistanceUrl( String url, double distance ) {
        clusterContents.add( new kMeansValue(url, distance, 0.0, 0.0) );
    }

    /**
     * @return access the cluster contents
     */
    public List<kMeansValue> getClusterContents() {
        return clusterContents;
    }

    /**
     * @param clusterContents set the contents
     */
    public void setClusterContents(List<kMeansValue> clusterContents) {
        this.clusterContents = clusterContents;
    }

    /**
     * calculate the distance between THIS and the vector passed in
     * @param other vector to get distance for
     * @return the distance between the two vectors
     */
    public double distance( Vector other ) {
        double distance = 0.0;
        for ( int i = 0; i < other.size(); i++ ) {
            double v1 = other.get(i);
            double v2 = centroid.get(i);
            distance = distance + (v1-v2) * (v1-v2);
        }
        return distance;
    }

    /**
     * add word to the cluster description
     * @param word the word to add
     */
    public void addSummaryKeyword( String word ) {
        clusterDescription.add( word );
    }

    /**
     * @return return the cluster contents
     */
    public List<String> getClusterDescription() {
        return clusterDescription;
    }

    /**
     * @param clusterDescription set the contents of the description
     */
    public void setClusterDescription(List<String> clusterDescription) {
        this.clusterDescription = clusterDescription;
    }


}



