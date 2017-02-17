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

package industries.vocht.viki.pca.covmatrixevd;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Eigenvalue decomposition with eigenvectors sorted according to corresponding
 * eigenvalues in a decreasing order. The eigenvalues in matrix {@code d} are
 * also sorted in the same way. This is the way the eigendecomposition
 * works in the R environment.
 * @author Mateusz Kobos
 */
public class EVD implements Serializable{
	private static final long serialVersionUID = 1L;

	public final Matrix d;
	public final Matrix v;
	
	public EVD(Matrix m){
		EigenvalueDecomposition evd = m.eig();

		double[] diagonal = getDiagonal(evd.getD());
		PermutationResult result= 
			calculateNondecreasingPermutation(diagonal);
		int[] permutation = result.permutation;
		double[] newDiagonal = result.values;
		this.v = permutateColumns(evd.getV(), permutation);
		this.d = createDiagonalMatrix(newDiagonal);
		assert eigenvaluesAreNonIncreasing(this.d);
	}
	
	private static Matrix createDiagonalMatrix(double[] diagonal){
		Matrix m = new Matrix(diagonal.length, diagonal.length);
		for(int i = 0; i < diagonal.length; i++) m.set(i, i, diagonal[i]);
		return m;
	}

	private static double[] getDiagonal(Matrix m){
		assert m.getRowDimension()==m.getColumnDimension();
		double[] diag = new double[m.getRowDimension()];
		for(int i = 0; i < m.getRowDimension(); i++)
			diag[i] = m.get(i, i);
		return diag;
	}

	private static PermutationResult calculateNondecreasingPermutation(
			double[] vals){
		ArrayList<ValuePlace> list = new ArrayList<ValuePlace>();
		for(int i = 0; i < vals.length; i++) 
			list.add(new ValuePlace(vals[i], i));
		Collections.sort(list);
		double[] newVals = new double[vals.length];
		int[] permutation = new int[vals.length];
		for(int i = 0; i < vals.length; i++){
			newVals[i] = list.get(i).value;
			permutation[i] = list.get(i).place;
		}
		return new PermutationResult(permutation, newVals);
	}

	private static Matrix permutateColumns(Matrix m, int[] permutation){
		assert m.getColumnDimension()==permutation.length;
		
		Matrix newM = new Matrix(m.getRowDimension(), m.getColumnDimension());
		for(int c = 0; c < newM.getColumnDimension(); c++){
			int copyFrom = permutation[c];
			for(int r = 0; r < newM.getRowDimension(); r++){
				newM.set(r, c, m.get(r, copyFrom));
			}
		}
		return newM;
	}
	
	private static boolean eigenvaluesAreNonIncreasing(Matrix d){
		for(int i = 0; i < d.getRowDimension()-1; i++)
			if(d.get(i, i) < d.get(i+1, i+1)) return false;
		return true;
	}
}

