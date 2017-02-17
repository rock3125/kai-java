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

package industries.vocht.viki.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by peter on 30/03/16.
 *
 * take a sparse vector and represent it as a
 * compressed vector - a vector with its "0.0" values left out
 *
 */
public class CompressedVector {

    private int dimenson;
    private double[] value;
    private int[] offset;

    public CompressedVector() {
    }

    // convert a vector to a compressed vector
    public CompressedVector(Vector vector) {

        dimenson = vector.size();

        // count the non-zeros
        int count = 0;
        for ( int i = 0; i < dimenson; i++ ) {
            if ( vector.get(i) != 0.0 ) {
                count = count + 1;
            }
        }

        value = new double[count];
        offset = new int[count];
        int index = 0;
        for ( int i = 0; i < dimenson; i++ ) {
            double v = vector.get(i);
            if ( v != 0.0 ) {
                value[index] = v;
                offset[index] = i;
                index = index + 1;
            }
        }
    }

    // convert this entity back to a vector
    public Vector convert() {
        Vector vector = new Vector(dimenson);
        for ( int i = 0; i < value.length; i++ ) {
            vector.set(offset[i], value[i]);
        }
        return vector;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return value == null || value.length == 0;
    }

    public int getDimenson() {
        return dimenson;
    }

    public void setDimenson(int dimenson) {
        this.dimenson = dimenson;
    }

    public double[] getValue() {
        return value;
    }

    public void setValue(double[] value) {
        this.value = value;
    }

    public int[] getOffset() {
        return offset;
    }

    public void setOffset(int[] offset) {
        this.offset = offset;
    }

}


