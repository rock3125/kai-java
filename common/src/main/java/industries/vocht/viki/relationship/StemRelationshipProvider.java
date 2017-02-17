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

package industries.vocht.viki.relationship;

import industries.vocht.viki.lexicon.Lexicon;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.TokenizerConstants;
import industries.vocht.viki.tokenizer.Tokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 12/08/16.
 *
 * a relationship provider based on lexicon stemming
 *
 */
public class StemRelationshipProvider extends AbstractRelationshipProvider {

    @Autowired
    private Lexicon lexicon;

    public StemRelationshipProvider() {
    }

    /**
     * return related words using stemming
     * @param word the word to process
     * @return a list of plain related words
     */
    @Override
    public List<RelatedWord> getRelationships(String word) {
        List<RelatedWord> relatedWords = new ArrayList<>();
        if ( word != null ) {

            String wordLwr = word.trim().toLowerCase();
            String stem = lexicon.getStem(wordLwr);
            List<String> stemRelatedList = lexicon.getStemmedList(stem);

            relatedWords.add( new RelatedWord(stem, 1.0f));
            if ( stemRelatedList != null ) {
                for ( String stemmed : stemRelatedList ) {
                    if ( !stemmed.equals(stem) ) {
                        relatedWords.add( new RelatedWord(stemmed, 1.0f));
                    }
                }
            }

        }
        return relatedWords;
    }

    @Override
    public String getStem(String word) {
        return lexicon.getStem(word);
    }

    @Override
    public Lexicon getLexicon() {
        return lexicon;
    }


}
