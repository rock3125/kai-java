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

/**
 * Created by peter on 28/04/16.
 *
 * the url and distance recorded together
 *
 */
public class kMeansValue implements Comparable<kMeansValue> {

    private double distance;
    private String url;

    // pca x,y
    private double x;
    private double y;

    public kMeansValue() {
    }

    public kMeansValue( String url, double distance, double x, double y ) {
        this.url = url;
        this.distance = distance;
        this.x = x;
        this.y = y;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    // sort by closest cosine distance first
    @Override
    public int compareTo(kMeansValue o) {
        if ( distance < o.distance ) return -1;
        if ( distance > o.distance ) return 1;
        return 0;
    }


}


