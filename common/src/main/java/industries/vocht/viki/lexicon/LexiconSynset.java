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

package industries.vocht.viki.lexicon;

import industries.vocht.viki.model.search.UISynset;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by peter on 23/05/16.
 *
 * a synset of the ambiguous lexicon
 *
 */
public class LexiconSynset {

    private String uniqueWord;
    private int synsetId;
    private HashSet<String> relationshipSet;

    public LexiconSynset() {
    }

    /**
     * convert this synset to a search object compatible return object
     * @param list the set of items to convert to UI synset items
     * @return the information needed by the UI
     */
    public static List<UISynset> convert( List<LexiconSynset> list ) {
        List<UISynset> synsetList = new ArrayList<>();
        for ( LexiconSynset item : list ) {
            synsetList.add( new UISynset(item.getUniqueWord(), item.getSynsetId()) );
        }
        return synsetList;
    }

    public LexiconSynset(String uniqueWord, int synsetId, HashSet<String> relationshipSet ) {
        this.uniqueWord = uniqueWord;
        this.synsetId = synsetId;
        this.relationshipSet = relationshipSet;
    }

    public String getUniqueWord() {
        return uniqueWord;
    }

    public void setUniqueWord(String uniqueWord) {
        this.uniqueWord = uniqueWord;
    }

    public int getSynsetId() {
        return synsetId;
    }

    public void setSynsetId(int synsetId) {
        this.synsetId = synsetId;
    }

    public HashSet<String> getRelationshipSet() {
        return relationshipSet;
    }

    public void setRelationshipSet(HashSet<String> relationshipSet) {
        this.relationshipSet = relationshipSet;
    }
}

