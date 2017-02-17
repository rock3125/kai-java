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

/** Calculates eigenvalue decomposition of the covariance matrix of the 
 * given data 
 * @author Mateusz Kobos
 * */
public interface CovarianceMatrixEVDCalculator {
	/** Calculate covariance matrix of the given data
	 * @param centeredData data matrix where rows are the instances/samples and 
	 * columns are dimensions. It has to be centered.
	 */
	public EVDResult run(Matrix centeredData);
}
