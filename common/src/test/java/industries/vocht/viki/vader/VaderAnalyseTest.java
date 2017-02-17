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

package industries.vocht.viki.vader;

import industries.vocht.viki.training_sets.TwitterSentimentItem;
import industries.vocht.viki.training_sets.TwitterSentimentTrainingSet;
import industries.vocht.viki.vader.VScore;
import industries.vocht.viki.vader.Vader;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Created by peter on 12/04/16.
 *
 * vader cucumber steps
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test-context.xml"})
public class VaderAnalyseTest {

    @Autowired
    private Vader vader;

    @Autowired
    private TwitterSentimentTrainingSet twitterSentimentTrainingSet;

    @Ignore
    @Test
    public void testVader() {

        int correctCount = 0; // number correct
        int failCount = 0; // number correct

        List<TwitterSentimentItem> twitterSentimentItemList = twitterSentimentTrainingSet.getTwitterSentimentItemList();
        Assert.notNull(twitterSentimentItemList);
        Assert.isTrue( twitterSentimentItemList.size() > 2000 );

        for ( int i = 0; i < 2000; i++ ) {
            TwitterSentimentItem sentimentItem = twitterSentimentItemList.get(i);
            VScore vScore = vader.analyseSentence(sentimentItem.getText());

            // negative vader and negative sentiment => correct
            if ( vScore.getCompound() < 0.0 && sentimentItem.getSentiment() == TwitterSentimentItem.Sentiment.Negative ) {
                correctCount = correctCount + 1;
                // negative vader and positive sentiment => fail
            } else if ( vScore.getCompound() < 0.0 && sentimentItem.getSentiment() == TwitterSentimentItem.Sentiment.Positive ) {
                failCount = failCount + 1;
                // positive vader and positive sentiment => correct
            } else if ( vScore.getCompound() > 0.0 && sentimentItem.getSentiment() == TwitterSentimentItem.Sentiment.Positive ) {
                correctCount = correctCount + 1;
                // positive vader and negative sentiment => fail
            } else if ( vScore.getCompound() > 0.0 && sentimentItem.getSentiment() == TwitterSentimentItem.Sentiment.Negative ) {
                failCount = failCount + 1;
            }
        }

        // test the ration is the right value
        double ratio = (double)correctCount / (double)failCount;
        Assert.isTrue( ratio >= 2.0 );
    }


}

