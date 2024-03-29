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

import industries.vocht.viki.pca.Assume;
import Jama.Matrix;

/**
 * Basic covariance matrix eigenvalue decomposition
 * @author Mateusz Kobos
 *
 */
public class EVDBased implements CovarianceMatrixEVDCalculator {

	@Override
	public EVDResult run(Matrix centeredData) {
		Matrix cov = calculateCovarianceMatrixOfCenteredData(centeredData);
		EVD evd = new EVD(cov);
		return new EVDResult(evd.d, evd.v);
	}
	
	/**
	 * Calculate covariance matrix with an assumption that data matrix is
	 * centered i.e. for each column i: x_i' = x_i - E(x_i) 
	 */
	public static Matrix calculateCovarianceMatrixOfCenteredData(Matrix data){
		Assume.assume(data.getRowDimension()>1, "Number of data samples is "+
				data.getRowDimension()+", but it has to be >1 to compute "+
				"covariances");
		int dimsNo = data.getColumnDimension();
		int samplesNo = data.getRowDimension();
		Matrix m = new Matrix(dimsNo, dimsNo);
		for(int r = 0; r < dimsNo; r++)
			for(int c = r; c < dimsNo; c++){
				double sum = 0;
				for(int i = 0; i < samplesNo; i++)
					 sum += data.get(i, r)*data.get(i, c);
				m.set(r, c, sum/(samplesNo-1));
			}
		for(int r = 0; r < dimsNo; r++)
			for(int c = 0; c < r; c++) m.set(r, c, m.get(c, r));
		return m;		
	}
}
