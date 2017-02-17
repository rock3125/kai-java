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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 25/03/16.
 *
 * just split words around spaces - nothing else
 * "straight forward" relationships
 *
 */
@Component
public class WordSplitterRelationshipProvider extends AbstractRelationshipProvider {

    @Value("${max.word.length:20}")
    private int MaxWordLength;

    public WordSplitterRelationshipProvider() {
    }

    /**
     * use a simple token parser to split the word into parts (if applicable)
     * and add the parts and the word itself
     * @param word the word to process
     * @return a list of plain related words
     */
    @Override
    public List<RelatedWord> getRelationships(String word) {
        List<RelatedWord> relatedWords = new ArrayList<>();
        if ( word != null ) {
            Tokenizer tokenizer = new Tokenizer();
            List<Token> tokenList = tokenizer.tokenize(word);
            if ( tokenList != null ) {
                boolean originalIncluded = false;
                for ( Token token : tokenList ) {
                    if ( token.getType() == TokenizerConstants.Type.Number ) {
                        if ( token.getText().length() < MaxWordLength ) {
                            if ( token.getText().equals(word) ) {
                                originalIncluded = true;
                            }
                            relatedWords.add(new RelatedWord(token.getText(), 1.0f));
                        }
                    } else if ( token.getType() == TokenizerConstants.Type.Text ) {
                        if ( token.getText().length() > 1 && token.getText().length() < MaxWordLength ) {
                            if ( token.getText().equals(word) ) {
                                originalIncluded = true;
                            }
                            relatedWords.add( new RelatedWord(token.getText(), 1.0f));
                        }
                    }
                } // for each token
                if (!originalIncluded) {
                    relatedWords.add( new RelatedWord(word, 1.0f) );
                }
            }
        }
        return relatedWords;
    }

    @Override
    public String getStem(String word) {
        return word;
    }

    @Override
    public Lexicon getLexicon() {
        return null;
    }

}

