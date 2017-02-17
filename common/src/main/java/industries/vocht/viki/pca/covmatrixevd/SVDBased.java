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

import Jama.Matrix;
import Jama.SingularValueDecomposition;


/**
 * SVD-based covariance matrix eigenvalue decomposition
 * @author Mateusz Kobos
 *
 */
public class SVDBased implements CovarianceMatrixEVDCalculator{
	@Override
	public EVDResult run(Matrix centeredData) {
		int m = centeredData.getRowDimension();
		int n = centeredData.getColumnDimension();
		SingularValueDecomposition svd = centeredData.svd();
		double[] singularValues = svd.getSingularValues();
		Matrix d = Matrix.identity(n, n);
		for(int i = 0; i < n; i++){
			/** TODO: This is true according to SVD properties in my notes*/
			double val;
			if(i < m) val = singularValues[i];
			else val = 0;
			
			d.set(i, i, 1.0/(m-1) * Math.pow(val, 2));
		}
		Matrix v = svd.getV();
		return new EVDResult(d, v);
	}
}
