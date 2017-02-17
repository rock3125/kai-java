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
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 * Created by peter on 23/05/16.
 *
 * support vector machine
 *
 */
public class SvmPredict {

    // the size of the word vector, dimension
    private static final int DEFAULT_NUM_FEATURES = 2000;

    // the svm model
    private svm_model model;

    public SvmPredict() {
    }

    /**
     * save a model to file
     * @param filename the file to write it to
     * @throws IOException
     */
    public void save( String filename ) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String modelStr = mapper.writeValueAsString(model);
        try ( PrintWriter writer = new PrintWriter(filename) ) {
            writer.write(modelStr);
        }
    }

    /**
     * load a model from file
     * @param filename the file to read it from
     * @throws IOException
     */
    public void load( String filename ) throws IOException {
        String modelStr = new String(Files.readAllBytes(Paths.get(filename)));
        ObjectMapper mapper = new ObjectMapper();
        model = mapper.readValue( modelStr, svm_model.class );
    }

    /**
     * helper - convert a set of words with sample data
     * @param set the set to convert
     * @return a vector of the set
     */
    public double[] setToVector( HashMap<String, Integer> set ) {
        // setup x-data arrays
        double[] xTrain = new double[DEFAULT_NUM_FEATURES];
        for ( String key : set.keySet() ) {
            double count = (double)set.get(key);
            int index = hashWord(key.toLowerCase());
            xTrain[index] = xTrain[index] + count;
        } // for each word in the vector
        return xTrain;
    }

    /**
     * predict the label for a single sample
     * @param xtest the sample
     * @return the predicted label
     */
    public double svmPredict( double[] xtest )
    {
        svm_node[] nodes = new svm_node[xtest.length];
        for (int i = 0; i < xtest.length; i++)
        {
            svm_node node = new svm_node();
            node.index = i;
            node.value = xtest[i];
            nodes[i] = node;
        }

        int[] labels = new int[model.nr_class];
        svm.svm_get_labels(model,labels);

        double[] prob_estimates = new double[model.nr_class];
        return svm.svm_predict_probability(model, nodes, prob_estimates);
    }


    /**
     * helper - turn the word into an index
     * @param word the word
     * @return the index
     */
    private int hashWord(String word) {
        return Math.abs(word.toLowerCase().hashCode()) % DEFAULT_NUM_FEATURES;
    }


}


