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

package industries.vocht.viki.lexicon;

import industries.vocht.viki.dao.PennType;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by peter on 15/06/16.
 *
 * set of pennTypes that are valid for vectorization
 *
 */
public class ValidPennTypes {

    // valid penn types for vectorization
    private HashSet<PennType> validPennTypes;

    // set of valid penn-types
    private static PennType[] validTypes = new PennType[] {
            PennType.NNP, PennType.NNPS, PennType.NN, PennType.NNS,
            PennType.JJ, PennType.JJR, PennType.JJS, PennType.RB,
            PennType.RBR, PennType.RBS, PennType.VB, PennType.VBD,
            PennType.VBG, PennType.VBN, PennType.VBP, PennType.VBZ,
            PennType.JJR, PennType.JJS
    };

    public ValidPennTypes() {
        validPennTypes = new HashSet<>();
        validPennTypes.addAll(Arrays.asList(validTypes) );
    }

    /**
     * return true if pennType is in the validPennTypes set
     * @param pennType the set
     * @return true if it is valid
     */
    public boolean isValidPennType( PennType pennType ) {
        return validPennTypes.contains(pennType);
    }

}

