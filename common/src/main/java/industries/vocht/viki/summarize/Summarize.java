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

package industries.vocht.viki.summarize;

import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.model.Token;

import java.util.*;

/**
 * Created by peter on 13/04/16.
 *
 * summarize a piece of text in terms of sentences of that text
 * this can get the top x records from a piece of text summarized
 * and the set of features used for scoring the text
 *
 */
public class Summarize {

    // the set of sentence to summarize
    private List<SummarySentence> sentenceList;

    // the title - the first sentence of the set if not supplied separately
    private SummarySentence title;

    // optional a thematic set of words
    private HashSet<String> thematicWordSet;

    /**
     * apply all feature calculations to a set of sentences for summarisation
     * @param sentenceList the list of sentence to summarize
     * @param thematicWordSet a set of thematic words to focus on (can be null to ignore)
     */
    public Summarize(List<Sentence> sentenceList, HashSet<String> thematicWordSet) {
        this.title = new SummarySentence(sentenceList.get(0));
        this.sentenceList = SummarySentence.convert(sentenceList.subList(1, sentenceList.size()));
        this.thematicWordSet = thematicWordSet;
    }

    /**
     * same as above, except allows for a separate title to be passed in
     * @param title the title for title scoring
     * @param sentenceList the text of the document
     * @param thematicWordSet a set of thematic words to focus on (can be null to ignore)
     */
    public Summarize(Sentence title, List<Sentence> sentenceList, HashSet<String> thematicWordSet) {
        this.title = new SummarySentence(title);
        this.sentenceList = SummarySentence.convert(sentenceList);
        this.thematicWordSet = thematicWordSet;
    }

    /**
     * return the top x sentence for a summarisation
     * @param x the number of items to return
     * @return a list of the top summary sentences
     */
    public List<Sentence> calculateTopX(int x) {

        if ( x > 0 ) {

            // perform all the features
            calculateSentenceLengthFeature();
            calculateNumericalDataFeature();
            calculateProperNounFeature();
            calculateSentencePositionFeature();
            calculateTermWeightFeature();
            calculateThematicWordFeature(thematicWordSet);
            calculateTitleFeature();

            // combine all features
            for ( SummarySentence sentence : sentenceList ) {
                sentence.combineFeatures();
            }

            // sort by most important first
            Collections.sort(sentenceList);

            // clip the top x if needed
            int count = x;
            if ( sentenceList.size() < count ) {
                count = sentenceList.size();
            }

            // collect the top x result
            List<Sentence> resultList = new ArrayList<>();
            for ( int i = 0; i < count; i++ ) {
                resultList.add( new Sentence( sentenceList.get(i).getTokenList() ) );
            }
            return resultList;

        }
        return null;
    }


    /**
     * calculate scores based on the SummarySentence length
     */
    private void calculateSentenceLengthFeature() {

        double max = 0.0;
        for ( SummarySentence SummarySentence : sentenceList ) {
            max = Math.max(max, (double)SummarySentence.size() );
        }

        if ( max > 0.0 ) {
            for ( SummarySentence SummarySentence : sentenceList ) {
                SummarySentence.setLengthFeature( (double)SummarySentence.size() / max );
            }
        }
    }

    /**
     * sentence position feature - earlier sentences are valued higher than later
     */
    private void calculateSentencePositionFeature() {
        int index = 0;
        for ( SummarySentence SummarySentence : sentenceList ) {
            if ( SummarySentence.size() > 0 ) {
                double value = 1.0 - ((double) index) / (double) sentenceList.size();
                SummarySentence.setPositionFeature(value);
            }
            index = index + 1;
        }
    }

    /**
     * calculate score based on a title match
     */
    private void calculateTitleFeature() {
        double titleLength = title.size();
        for ( SummarySentence text : sentenceList ) {
            double numMatches = text.matchCount( title );
            text.setTitleFeature( numMatches / titleLength );
        }
    }

    /**
     * calculate a score based on the number of proper nouns in a sentence
     */
    private void calculateProperNounFeature() {
        for ( SummarySentence sentence : sentenceList ) {
            int size = sentence.size();
            if ( size > 0 ) {
                int nnpCount = sentence.getTagNNPTagCount();
                sentence.setProperNounFeature( (double)nnpCount / (double)size );
            }
        }
    }

    /**
     * calculate the numeric data features (number of CDs)
     */
    private void calculateNumericalDataFeature() {
        for ( SummarySentence sentence : sentenceList ) {
            int size = sentence.size();
            if ( size > 0 ) {
                int cdCount = sentence.getCDTagCount();
                sentence.setNumericalDataFeature( (double)cdCount / (double)size );
            }
        }
    }

    /**
     * calculate the thematic word set for a sentence set
     * @param thematicWordSet a set of focus words on a theme or set of themes
     */
    private void calculateThematicWordFeature( HashSet<String> thematicWordSet ) {

        if (thematicWordSet != null) {

            double max = 0.0;
            for (SummarySentence sentence : sentenceList) {
                int count = sentence.calculateThematicWordCount(thematicWordSet);
                if (count > max) {
                    max = count;
                }
                sentence.setThematicWordCount(sentence.getThematicWordCount() + count);
            }

            // normalize
            if (max > 0.0) {
                for (SummarySentence sentence : sentenceList) {
                    sentence.setThematicWordCount(sentence.getThematicWordCount() / max);
                }
            }

        }

    }

    /**
     * calculate the term frequency features of a document
     */
    private void calculateTermWeightFeature() {

        // collect frequencies
        Map<String, Integer> tokenFrequencyMap = new HashMap<>();
        for ( SummarySentence SummarySentence : sentenceList ) {
            for ( Token token : SummarySentence.getTokenList() ) {

                if ( token.isValidPosTag() ) {

                    String tokenStr = token.getText().toLowerCase();
                    Integer count = tokenFrequencyMap.get(tokenStr);
                    if ( count == null ) {
                        tokenFrequencyMap.put(tokenStr, 1);
                    } else {
                        tokenFrequencyMap.put(tokenStr, count + 1);
                    }
                }
            }
        }

        // get the max count
        int max = 0;
        for ( SummarySentence SummarySentence : sentenceList ) {
            for (Token token : SummarySentence.getTokenList()) {
                if ( token.isValidPosTag() ) {
                    String tokenStr = token.getText().toLowerCase();
                    Integer value = tokenFrequencyMap.get(tokenStr);
                    if ( value != null && value > max ) {
                        max = value;
                    }
                }
            }
        }

        // calculate a term score for each SummarySentence
        for ( SummarySentence SummarySentence : sentenceList ) {
            double score = 0.0;
            double hits = 0.0;
            for (Token token : SummarySentence.getTokenList()) {
                if (token.isValidPosTag()) {
                    String tokenStr = token.getText().toLowerCase();
                    Integer value = tokenFrequencyMap.get(tokenStr);
                    if ( value != null ) {
                        score = score + (double)value;
                        hits = hits + 1.0;
                    }
                }
            }

            if ( hits > 0.0 ) {
                score = score / hits;
                score = score / max;
                SummarySentence.setTermFrequencyFeature(score);
            }

        }

    }




}
