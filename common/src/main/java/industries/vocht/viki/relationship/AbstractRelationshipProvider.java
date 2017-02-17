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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

/*
 * Created by peter on 6/02/15.
 *
 * provider of relationship information
 *
 */
public abstract class AbstractRelationshipProvider
{
    private HashMap<String, Float> relatednessMap;

    private static Logger logger = LoggerFactory.getLogger(AbstractRelationshipProvider.class);

    // return a list of words related to "word" and their relationship strengths
    abstract public List<RelatedWord> getRelationships( String word );

    // return the stem of a word - or the word itself if n/a
    abstract public String getStem( String word );

    // access the lexicon
    abstract public Lexicon getLexicon();

    // return how related two words are for this relationship provider
    public float getRelatedness( String word1, String word2 ) {
        String key = getWordKey(word1, word2);
        if ( key != null ) {
            Float value = relatednessMap.get(key);
            if ( value != null )
                return value;
            return 0.0f;
        } else
            return 1.0f; // same word
    }

    // setup the relatedness map
    public void init() {
        if ( getLexicon() != null ) { // do we have a lexicon?
            this.relatednessMap = new HashMap<>(getLexicon().getLexicon().size() * 2);

            // create the relatedness map for relationship lookup
            for (Token item : getLexicon().getLexicon()) {
                List<RelatedWord> relatedWordList = getRelationships(item.getText());
                if (relatedWordList != null) {
                    for (RelatedWord relatedWord : relatedWordList) {
                        String key = getWordKey(item.getText(), relatedWord.getWord());
                        if (key != null) {
                            relatednessMap.put(key, relatedWord.getRelationshipStrength());
                        }
                    }
                }
            }
            logger.info("relationship map setup " + relatednessMap.size() + " word relationships");
        } else {
            this.relatednessMap = new HashMap<>(); // just an empty map
        }
    }

    // get a key for indexing words (or null if they're equal)
    private String getWordKey( String word1, String word2 ) {
        String w1 = word1.toLowerCase();
        String w2 = word2.toLowerCase();
        int cf = w1.compareTo(w2);

        String key = null;
        if ( cf > 0 )
            key =  w2 + ":" + w1;
        else if ( cf < 0)
            key =  w1 + ":" + w2;
        return key;
    }

}

