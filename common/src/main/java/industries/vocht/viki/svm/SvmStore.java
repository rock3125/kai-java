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

package industries.vocht.viki.svm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by peter on 23/05/16.
 *
 * the store of svm models - a smart cache
 *
 */
@Component
public class SvmStore {

    @Value("${svm.model.base.folder:/opt/kai/data/svm/models}")
    private String modelBase;

    private Map<String, SvmPredict> modelStore;

    public SvmStore() {
    }

    public void init() {
        modelStore = new HashMap<>();
    }

    /**
     * access items in the svm model cache
     * @param word the word to get a model for
     * @return the model of the ambiguous word
     * @throws IOException
     */
    public SvmPredict getModel(String word) throws IOException {
        String lower = word.toLowerCase().trim();
        SvmPredict svm = modelStore.get(lower);
        if ( svm == null ) {
            String filename = modelBase + "/" + lower + "-model.json";
            if ( new File(filename).exists() ) {
                svm = new SvmPredict();
                svm.load(filename);
                modelStore.put(lower, svm);
            } else {
                return null;
            }
        }
        return svm;
    }

}


