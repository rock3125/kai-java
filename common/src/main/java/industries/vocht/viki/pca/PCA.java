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

import Jama.Matrix;
import industries.vocht.viki.pca.covmatrixevd.*;


/** The class responsible mainly for preparing the PCA transformation parameters 
 * based on training data and executing the actual transformation on test data.
 * @author Mateusz Kobos
 */
public final class PCA {
	/** Type of the possible data transformation.
	 * ROTATION: rotate the data matrix to get a diagonal covariance matrix. 
	 * This transformation is sometimes simply called PCA.
	 * WHITENING: rotate and scale the data matrix to get 
	 * the unit covariance matrix
	 */
	public enum TransformationType { ROTATION, WHITENING };
	
	/** Whether the input data matrix should be centered. */
	private final boolean centerMatrix;
	
	/** Number of input dimensions. */
	private final int inputDim;
	
	private final Matrix whiteningTransformation;
	private final Matrix pcaRotationTransformation;
	private final Matrix v;
	/** Part of the original SVD vector that is responsible for transforming the
	 * input data into a vector of zeros.*/
	private final Matrix zerosRotationTransformation; 
	private final Matrix d;
	private final double[] means;
	private final double threshold;
	
	/** Create the PCA transformation. Use the popular SVD method for internal
	 * calculations
	 * @param data data matrix used to compute the PCA transformation. 
	 * Rows of the matrix are the instances/samples, columns are dimensions.
	 * It is assumed that the matrix is already centered.
	 * */
	public PCA(Matrix data){
		this(data, new SVDBased(), true);
	}

	/** Create the PCA transformation. Use the popular SVD method for internal
	 * calculations
	 * @param data data matrix used to compute the PCA transformation. 
	 * Rows of the matrix are the instances/samples, columns are dimensions.
	 * @param center should the data matrix be centered before doing the
	 * calculations?
	 * */
	public PCA(Matrix data, boolean center){
		this(data, new SVDBased(), center);
	}
	
	/** Create the PCA transformation.
	 * @param data data matrix used to compute the PCA transformation.
	 * Rows of the matrix are the instances/samples, columns are dimensions.
	 * It is assumed that the matrix is already centered.
	 * @param evdCalc method of computing eigenvalue decomposition of data's
	 * covariance matrix
	 * */
	public PCA(Matrix data, CovarianceMatrixEVDCalculator evdCalc){
		this(data, evdCalc, true);
	}
	
	/** Create the PCA transformation
	 * @param data data matrix used to compute the PCA transformation. 
	 * Rows of the matrix are the instances/samples, columns are dimensions.
	 * @param evdCalc method of computing eigenvalue decomposition of data's
	 * covariance matrix
	 * @param center should the data matrix be centered before doing the
	 * calculations?
	 */
	public PCA(Matrix data, CovarianceMatrixEVDCalculator evdCalc, boolean center){
		/** Determine if input matrix should be centered */
		this.centerMatrix = center;
		/** Get the number of input dimensions. */
		this.inputDim = data.getColumnDimension();
		this.means = getColumnsMeans(data);
		
		Matrix centeredData = data;
		/** Center the data matrix columns about zero */
		if(centerMatrix){
			centeredData = shiftColumns(data, means);
		}
		//debugWrite(centeredData, "centeredData.csv");

		EVDResult evd = evdCalc.run(centeredData);
		EVDWithThreshold evdT = new EVDWithThreshold(evd);
		/** Get only the values of the matrices that correspond to 
		 * standard deviations above the threshold*/
		this.d = evdT.getDAboveThreshold();
		this.v = evdT.getVAboveThreshold();
		this.zerosRotationTransformation = evdT.getVBelowThreshold();
		/** A 3-sigma-like ad-hoc rule */
		this.threshold = 3*evdT.getThreshold();
		
		//debugWrite(this.evd.v, "eigen-v.csv");
		//debugWrite(this.evd.d, "eigen-d.csv");
		
		Matrix sqrtD = sqrtDiagonalMatrix(d);
		Matrix scaling = inverseDiagonalMatrix(sqrtD);
		//debugWrite(scaling, "scaling.csv");
		this.pcaRotationTransformation = v;
		this.whiteningTransformation = 
			this.pcaRotationTransformation.times(scaling);
	}
	
	/**
	 * @return matrix where eigenvectors are placed in columns
	 */
	public Matrix getEigenvectorsMatrix(){
		return v;
	}
	
	/**
	 * Get selected eigenvalue
	 * @param dimNo dimension number corresponding to given eigenvalue
	 */
	public double getEigenvalue(int dimNo){
		return d.get(dimNo, dimNo);
	}
	
	/**
	 * Get number of dimensions of the input vectors
	 */
	public int getInputDimsNo(){
		return inputDim;
	}
	
	/**
	 * Get number of dimensions of the output vectors
	 */
	public int getOutputDimsNo(){
		return v.getColumnDimension();
	}
	
	/**
	 * Execute selected transformation on given data.
	 * @param data data to transform. Rows of the matrix are the 
	 * instances/samples, columns are dimensions. 
	 * If the original PCA data matrix was set to be centered, this
	 * matrix will also be centered using the same parameters.
	 * @param type transformation to apply
	 * @return transformed data
	 */
	public Matrix transform(Matrix data, TransformationType type){
		Matrix centeredData = data;
		if(centerMatrix){
			centeredData = shiftColumns(data, means);
		}
		Matrix transformation = getTransformation(type); 
		return centeredData.times(transformation);
	}
	
	/**
	 * Check if given point lies in PCA-generated subspace. 
	 * If it does not, it means that the point doesn't belong 
	 * to the transformation domain i.e. it is an outlier.
	 * @param pt point. If the original PCA data matrix was set to be centered, 
	 * this point will also be centered using the same parameters.
	 * @return true iff the point lies on all principal axes
	 */
	public boolean belongsToGeneratedSubspace(Matrix pt){
		Assume.assume(pt.getRowDimension()==1);
		Matrix centeredPt = pt;
		if(centerMatrix){
			centeredPt = shiftColumns(pt, means);
		}
		Matrix zerosTransformedPt = centeredPt.times(zerosRotationTransformation);
		assert zerosTransformedPt.getRowDimension()==1;
		/** Check if all coordinates of the point were zeroed by the 
		 * transformation */
		for(int c = 0; c < zerosTransformedPt.getColumnDimension(); c++)
			if(Math.abs(zerosTransformedPt.get(0, c)) > threshold) {
				return false;
			}
		return true;
	}
	
	/**
	 * Function for JUnit testing purposes only 
	 * */
	public static Matrix calculateCovarianceMatrix(Matrix data){
		double[] means = getColumnsMeans(data);
		Matrix centeredData = shiftColumns(data, means);
		return EVDBased.calculateCovarianceMatrixOfCenteredData(
				centeredData);
	}
	
	private Matrix getTransformation(TransformationType type){
		switch(type){
		case ROTATION: return pcaRotationTransformation;
		case WHITENING: return  whiteningTransformation;
		default: throw new RuntimeException("Unknown enum type: "+type);
		}
	}
	
	private static Matrix shiftColumns(Matrix data, double[] shifts){
		Assume.assume(shifts.length==data.getColumnDimension());
		Matrix m = new Matrix(
				data.getRowDimension(), data.getColumnDimension());
		for(int c = 0; c < data.getColumnDimension(); c++)
			for(int r = 0; r < data.getRowDimension(); r++)
				m.set(r, c, data.get(r, c) - shifts[c]);
		return m;		
	}
	
	private static double[] getColumnsMeans(Matrix m){
		double[] means = new double[m.getColumnDimension()];
		for(int c = 0; c < m.getColumnDimension(); c++){
			double sum = 0;
			for(int r = 0; r < m.getRowDimension(); r++)
				sum += m.get(r, c);
			means[c] = sum/m.getRowDimension();
		}
		return means;
	}
	
	private static Matrix sqrtDiagonalMatrix(Matrix m){
		assert m.getRowDimension()==m.getColumnDimension();
		Matrix newM = new Matrix(m.getRowDimension(), m.getRowDimension());
		for(int i = 0; i < m.getRowDimension(); i++)
			newM.set(i, i, Math.sqrt(m.get(i, i)));
		return newM;
	}
	
	private static Matrix inverseDiagonalMatrix(Matrix m){
		assert m.getRowDimension()==m.getColumnDimension();
		Matrix newM = new Matrix(m.getRowDimension(), m.getRowDimension());
		for(int i = 0; i < m.getRowDimension(); i++)
			newM.set(i, i, 1/m.get(i, i));
		return newM;
	}
	
}

