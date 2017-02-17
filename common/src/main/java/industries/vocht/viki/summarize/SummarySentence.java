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

import industries.vocht.viki.dao.PennType;
import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.model.Token;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by peter on 13/04/16.
 *
 * a sentence with summarization calculation information
 *
 */
public class SummarySentence extends Sentence implements Comparable<SummarySentence> {

    private double lengthFeature;
    private double positionFeature;
    private double titleFeature;
    private double properNounFeature;
    private double numericalDataFeature;
    private double thematicWordCount;
    private double termFrequencyFeature;

    private double combinedScore;

    public SummarySentence() {
        super();
    }

    public SummarySentence( Sentence sentence ) {
        super(sentence.getTokenList());
    }

    public SummarySentence( List<Token> tokenList ) {
        super(tokenList);
    }

    /**
     * convert a se tof sentences to a set of summary sentences
     * @param sentenceList the list of sentences
     * @return the list of sumamry sentences
     */
    public static List<SummarySentence> convert( List<Sentence> sentenceList ) {
        if ( sentenceList != null ) {
            List<SummarySentence> summarySentenceList = new ArrayList<>();
            for ( Sentence sentence : sentenceList ) {
                summarySentenceList.add( new SummarySentence(sentence.getTokenList()) );
            }
            return summarySentenceList;
        }
        return null;
    }

    /**
     * combine the calculated features in a normalized fashion
     */
    public void combineFeatures() {
        double total = lengthFeature * lengthFeature + positionFeature * positionFeature +
                titleFeature * titleFeature + properNounFeature * properNounFeature +
                termFrequencyFeature * termFrequencyFeature +
                numericalDataFeature * numericalDataFeature + thematicWordCount * thematicWordCount;
        combinedScore = Math.sqrt(total);
    }

    /**
     * return the count of the number of tags NNP/NNPS tags in this sentence
     * @return the count
     */
    public int getTagNNPTagCount() {
        int count = 0;
        for ( Token token : tokenList ) {
            if ( token.getPennType() == PennType.NNP || token.getPennType() == PennType.NNPS ) {
                count = count + 1;
            }
        }
        return count;
    }

    /**
     * return the count of the number of tags in this sentence that are CD type (cardinal numbers)
     * @return the count
     */
    public int getCDTagCount() {
        int count = 0;
        for ( Token token : tokenList ) {
            if ( token.getPennType() == PennType.CD ) {
                count = count + 1;
            }
        }
        return count;
    }

    // count how many words of this item match with the other item
    public int matchCount( Sentence other ) {
        int count = 0;
        for ( Token token : tokenList ) {
            if ( token.isValidPosTag() ) {
                for (Token otherToken : other.getTokenList()) {
                    if ( otherToken.isValidPosTag() ) {
                        if (otherToken.getText().compareToIgnoreCase(token.getText()) == 0 ) {
                            count = count + 1;
                        }
                    }
                }
            }
        }
        return count;
    }

    public int calculateThematicWordCount( HashSet<String> thematicWordSet ) {
        int count = 0;
        if ( thematicWordSet != null ) {
            for ( Token token : tokenList ) {
                if ( thematicWordSet.contains(token.getText().toLowerCase()) ) {
                    count = count + 1;
                }
            }
        }
        return count;
    }

    public double getLengthFeature() {
        return lengthFeature;
    }

    public void setLengthFeature(double lengthFeature) {
        this.lengthFeature = lengthFeature;
    }

    public double getPositionFeature() {
        return positionFeature;
    }

    public void setPositionFeature(double positionFeature) {
        this.positionFeature = positionFeature;
    }

    public double getTitleFeature() {
        return titleFeature;
    }

    public void setTitleFeature(double titleFeature) {
        this.titleFeature = titleFeature;
    }

    public double getProperNounFeature() {
        return properNounFeature;
    }

    public void setProperNounFeature(double properNounFeature) {
        this.properNounFeature = properNounFeature;
    }

    public double getNumericalDataFeature() {
        return numericalDataFeature;
    }

    public void setNumericalDataFeature(double numericalDataFeature) {
        this.numericalDataFeature = numericalDataFeature;
    }

    public double getThematicWordCount() {
        return thematicWordCount;
    }

    public void setThematicWordCount(double thematicWordCount) {
        this.thematicWordCount = thematicWordCount;
    }

    public double getCombinedScore() {
        return combinedScore;
    }

    public void setCombinedScore(double combinedScore) {
        this.combinedScore = combinedScore;
    }

    public double getTermFrequencyFeature() {
        return termFrequencyFeature;
    }

    public void setTermFrequencyFeature(double termFrequencyFeature) {
        this.termFrequencyFeature = termFrequencyFeature;
    }

    // sort by combined score
    @Override
    public int compareTo(SummarySentence other) {
        if ( combinedScore < other.combinedScore ) {
            return 1;
        }
        if ( combinedScore > other.combinedScore ) {
            return -1;
        }
        return 0;
    }


}


