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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 30/04/16.
 *
 * based from kMeansClusterInterim
 *
 */
public class kMeansCluster implements Comparable<kMeansCluster> {

    private int cluster_id;
    private CompressedVector centroid; // the k-means centroid for this cluster item
    private List<kMeansValue> clusterContents; // the members and their distance to the centroid
    private int numEntries; // how many / the size of the cluster contents
    private List<String> clusterDescription; // list of words describing this cluster

    public kMeansCluster() {
        this.clusterContents = new ArrayList<>();
        this.clusterDescription = new ArrayList<>();
    }


    public CompressedVector getCentroid() {
        return centroid;
    }

    public void setCentroid(CompressedVector centroid) {
        this.centroid = centroid;
    }

    public List<kMeansValue> getClusterContents() {
        return clusterContents;
    }

    public void setClusterContents(List<kMeansValue> clusterContents) {
        this.clusterContents = clusterContents;
    }

    public List<String> getClusterDescription() {
        return clusterDescription;
    }

    public void setClusterDescription(List<String> clusterDescription) {
        this.clusterDescription = clusterDescription;
    }

    public int getCluster_id() {
        return cluster_id;
    }

    public void setCluster_id(int cluster_id) {
        this.cluster_id = cluster_id;
    }

    public int getNumEntries() {
        return numEntries;
    }

    public void setNumEntries(int numEntries) {
        this.numEntries = numEntries;
    }


    @Override
    public int compareTo(kMeansCluster other) {
        if ( numEntries < other.numEntries ) return 1;
        if ( numEntries > other.numEntries ) return -1;
        return 0;
    }


}



