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

package industries.vocht.viki.training_sets;

import industries.vocht.viki.model.Token;
import industries.vocht.viki.tokenizer.Tokenizer;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 4/04/16.
 *
 * the complete twitter sentiment training set
 *
 */
@Component
public class TwitterSentimentTrainingSet {

    private final static Logger logger = LoggerFactory.getLogger(TwitterSentimentTrainingSet.class);

    @Value("${training.set.twitter.sentiment:/opt/kai/data/training-sets/twitter-sentiment/twitter-sentiment-analysis-trainingset.csv}")
    private String twitterTraininSetFilename;

    private List<TwitterSentimentItem> twitterSentimentItemList;

    public TwitterSentimentTrainingSet() {
        twitterSentimentItemList = new ArrayList<>();
    }

    /**
     * setup the twitter sentiment training set
     */
    public void init() throws IOException {
        logger.info("init(" + twitterTraininSetFilename + ")");
        // tokenize the text streams
        if ( new File(twitterTraininSetFilename).exists() ) {
            Tokenizer tokenizer = new Tokenizer();
            List<String> lineList = Files.readAllLines(Paths.get(twitterTraininSetFilename));
            for (int i = 0; i < lineList.size(); i++) {
                if (i > 0) { // skip header
                    String line = lineList.get(i);
                    String[] csv = splitCsv(line);
                    if (csv.length == 4) {
                        // id, {0,1}, source, text
                        int sent = Integer.valueOf(csv[1]);
                        TwitterSentimentItem.Sentiment sentiment = TwitterSentimentItem.Sentiment.Positive;
                        if (sent == 0) {
                            sentiment = TwitterSentimentItem.Sentiment.Negative;
                        }

                        // tokenize the text and remove redundant spaces
                        String str = csv[3].trim();
                        if (str.startsWith("\"")) {
                            str = str.substring(1);
                        }
                        if (str.endsWith("\"")) {
                            str = str.substring(0, str.length() - 1);
                            str = str.trim();
                        }
                        List<Token> tokenList = tokenizer.filterOutSpaces(tokenizer.tokenize(StringEscapeUtils.unescapeXml(str)));

                        if (tokenList != null && tokenList.size() > 0) {
                            twitterSentimentItemList.add(new TwitterSentimentItem(sentiment, tokenList));
                        }

                    }
                }
            }
        }
    }

    /**
     * access the sentiment training set
     * @return the sentiment twitter training set
     */
    public List<TwitterSentimentItem> getTwitterSentimentItemList() {
        return twitterSentimentItemList;
    }

    /**
     * simple CSV split taking int account the " and ,
     * @param line the line to split
     * @return an array of string split around commas
     */
    private String[] splitCsv(String line) {
        return line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
    }

}

