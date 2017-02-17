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

package industries.vocht.viki.pca;

/**
 * Created by peter on 22/04/16.
 *
 */

import Jama.Matrix;
import industries.vocht.viki.pca.covmatrixevd.EVDResult;

/**
 * Version of the eigenvalue decomposition where values of standard deviations
 * (i.e. square roots of the eigenvalues) below a certain threshold are omitted.
 */
public class EVDWithThreshold {
    /** Double machine precision in the R environment
     * (i.e. in the R environment: {@code .Machine$double.eps}) */
    public static final double precision = 2.220446e-16;

    private final EVDResult evd;
    private final double threshold;

    /**
     * The tol parameter of the method assumes a default value equal to
     * {@code sqrt(.Machine$double.eps)} from the R environment. In the help
     * page of the R environment {@code prcomp} function
     * (see the paragraph on {@code tol} parameter) it is written that
     * in such setting we will "omit essentially constant components". */
    public EVDWithThreshold(EVDResult evd){
        this(evd, Math.sqrt(precision));
    }
    /**
     * @param tol threshold parameter of the method - the same parameter
     * as used in R environment's `prcomp` function (see the paragraph on
     * {@code tol} parameter). */
    public EVDWithThreshold(EVDResult evd, double tol){
        this.evd = evd;
        this.threshold = firstComponentSD(evd)*tol;
    }

    private static double firstComponentSD(EVDResult evd){
        return Math.sqrt(evd.d.get(0, 0));
    }

    /** Magnitude below which components should be omitted. This parameter
     * corresponds to the one in the `prcomp` function from the R
     * environment (see the paragraph on {@code tol} parameter).
     * The components are omitted if their standard deviations are less than
     * or equal to {@code tol} (a parameter given in the constructor) times
     * the standard deviation of the first component.
     */
    public double getThreshold(){
        return threshold;
    }

    public Matrix getDAboveThreshold(){
        int aboveThresholdElemsNo = getElementsNoAboveThreshold();
        Matrix newD = evd.d.getMatrix(0, aboveThresholdElemsNo-1,
                0, aboveThresholdElemsNo-1);
        return newD;
    }

    public Matrix getVAboveThreshold(){
        return evd.v.getMatrix(0, evd.v.getRowDimension()-1,
                0, getElementsNoAboveThreshold()-1);
    }

    public Matrix getVBelowThreshold(){
        return evd.v.getMatrix(0, evd.v.getRowDimension()-1,
                getElementsNoAboveThreshold(), evd.v.getColumnDimension()-1);
    }

    private int getElementsNoAboveThreshold(){
        for(int i = 0; i < evd.d.getColumnDimension(); i++){
            double val = Math.sqrt(evd.d.get(i, i));
            if(!(val > threshold)) return i;
        }
        return evd.d.getColumnDimension();
    }
}


