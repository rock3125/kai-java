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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/*
 * Created by peter on 6/02/15.
 *
 * relationships provided directly from the lexicon
 *
 */
@Component
public class SynonymRelationshipProvider extends AbstractRelationshipProvider
{
    public static final float STEM_STRENGTH = 1.0f;
    public static final float SYNONYM_STRENGTH = 0.5f;
    public static final float ASSOCIATION_STRENGTH = 0.4f;

    @Autowired
    private Lexicon lexicon;

    public SynonymRelationshipProvider() {
    }

    /**
     * return all relationships for a word;  synonyms, stemmed version
     * and loosely associated values
     * @param word the word to check
     * @return return relationships if it has any, can be an empty list but never null
     */
    @Override
    public List<RelatedWord> getRelationships(String word) {

        List<RelatedWord> relatedWordList = new ArrayList<>();

        List<String> synonymList = lexicon.getSynonymList( word );
        if ( synonymList != null )
            for ( String synonym: synonymList )
                relatedWordList.add( new RelatedWord(synonym, SYNONYM_STRENGTH) );

        String wordStem = getStem(word);
        if ( wordStem.compareToIgnoreCase(word) != 0 )
            relatedWordList.add( new RelatedWord(wordStem, STEM_STRENGTH) );

        List<String> stemList = lexicon.getStemmedList(word);
        if ( stemList != null )
            for ( String stemmed: stemList )
                if ( stemmed.compareToIgnoreCase(word) != 0 )
                    relatedWordList.add( new RelatedWord(stemmed, STEM_STRENGTH) );

        List<String> associationList = lexicon.getAssociationList( word );
        if ( associationList != null )
            for ( String association: associationList )
                relatedWordList.add( new RelatedWord(association, ASSOCIATION_STRENGTH) );

        return relatedWordList;
    }

    @Override
    public String getStem(String word) {
        return lexicon.getStem(word);
    }

    public Lexicon getLexicon() {
        return lexicon;
    }

    public void setLexicon(Lexicon lexicon) {
        this.lexicon = lexicon;
    }
}

