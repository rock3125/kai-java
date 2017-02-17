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

package industries.vocht.viki.bayes;

import org.junit.Test;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.Collection;

public class BayesTest {

    @Test
    public void testBayesClassifier() {

        /*
         * Create a new classifier instance. The context features are
         * Strings and the context will be classified with a String according
         * to the featureset of the context.
         */
        final Classifier<String, String> bayes = new BayesClassifier<String, String>();

        /*
         * Please note, that this particular classifier implementation will
         * "forget" learned classifications after a few learning sessions. The
         * number of learning sessions it will record can be set as follows:
         */
        bayes.setMemoryCapacity(500); // remember the last 500 learned classifications

        /*
         * The classifier can learn from classifications that are handed over
         * to the learn methods. Imagin a tokenized text as follows. The tokens
         * are the text's features. The category of the text will either be
         * positive or negative.
         */
        final String[] positiveText = "today is a sunny day".split("\\s");
        bayes.learn("positive", Arrays.asList(positiveText));

        final String[] negativeText = "today is a rainy day".split("\\s");
        bayes.learn("negative", Arrays.asList(negativeText));

        final String[] neutralText = "today is an ok day".split("\\s");
        bayes.learn("neutral", Arrays.asList(neutralText));

        /*
         * Now that the classifier has "learned" two classifications, it will
         * be able to classify similar sentences. The classify method returns
         * a Classification Object, that contains the given featureset,
         * classification probability and resulting category.
         */
        final String[] unknownText1 = "it was rainy yesterday".split("\\s");
        final String[] unknownText2 = "it was sunny yesterday".split("\\s");
        final String[] unknownText3 = "yesterday was ok".split("\\s");

        ClassificationResult<String, String> result1 = bayes.classify(Arrays.asList(unknownText1));
        Assert.notNull(result1);
        Assert.isTrue( result1.getCategory() != null && result1.getCategory().equals("negative") );

        ClassificationResult<String, String> result2 = bayes.classify(Arrays.asList(unknownText2));
        Assert.notNull(result2);
        Assert.isTrue( result2.getCategory() != null && result2.getCategory().equals("positive") );

        ClassificationResult<String, String> result3 = bayes.classify(Arrays.asList(unknownText3));
        Assert.notNull(result3);
        Assert.isTrue( result3.getCategory() != null && result3.getCategory().equals("neutral") );

        /*
         * The BayesClassifier extends the abstract Classifier and provides
         * detailed classification results that can be retrieved by calling
         * the classifyDetailed Method.
         *
         * The classification with the highest probability is the resulting
         * classification. The returned List will look like this.
         * [
         *   Classification [
         *     category=negative,
         *     probability=0.0078125,
         *     featureset=[today, is, a, sunny, day]
         *   ],
         *   Classification [
         *     category=positive,
         *     probability=0.0234375,
         *     featureset=[today, is, a, sunny, day]
         *   ]
         * ]
         */
        Collection<ClassificationResult<String, String>> detailedResult = ((BayesClassifier<String, String>) bayes).classifyDetailed(
                Arrays.asList(unknownText1));
        Assert.isTrue(detailedResult != null && detailedResult.size() == 3);

    }

}

