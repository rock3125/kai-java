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

/**
 * Created by peter on 22/04/16.
 */
public class PermutationResult {
    public int[] permutation;
    public double[] values;

    public PermutationResult(int[] permutation, double[] values){
        this.permutation = permutation;
        this.values = values;
    }
}
