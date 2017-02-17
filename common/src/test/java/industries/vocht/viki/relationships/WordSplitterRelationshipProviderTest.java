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

package industries.vocht.viki.relationships;

import industries.vocht.viki.relationship.RelatedWord;
import industries.vocht.viki.relationship.WordSplitterRelationshipProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

/**
 * Created by peter on 13/06/16.
 *
 * test the straight relationship provider
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test-context.xml"})
public class WordSplitterRelationshipProviderTest {

    @Autowired
    private WordSplitterRelationshipProvider wordSplitterRelationshipProvider;

    @Test
    public void testRelationships1() {
        List<RelatedWord> relatedWordList = wordSplitterRelationshipProvider.getRelationships("http://www.peter.co.nz/12345");
        Assert.notNull(relatedWordList);
        Assert.isTrue(relatedWordList.size() == 7);
        Assert.isTrue(contains(relatedWordList, "http", 1.0f));
        Assert.isTrue(contains(relatedWordList, "www", 1.0f));
        Assert.isTrue(contains(relatedWordList, "peter", 1.0f));
        Assert.isTrue(contains(relatedWordList, "co", 1.0f));
        Assert.isTrue(contains(relatedWordList, "nz", 1.0f));
        Assert.isTrue(contains(relatedWordList, "12345", 1.0f));
        Assert.isTrue(contains(relatedWordList, "http://www.peter.co.nz/12345", 1.0f));
    }


    @Test
    public void testRelationships2() {
        List<RelatedWord> relatedWordList = wordSplitterRelationshipProvider.getRelationships("abcdefghijklmnopqrstuvwxyz");
        Assert.notNull(relatedWordList);
        Assert.isTrue(relatedWordList.size() == 1);
        Assert.isTrue(contains(relatedWordList, "abcdefghijklmnopqrstuvwxyz", 1.0f));
    }

    @Test
    public void testRelationships3() {
        List<RelatedWord> relatedWordList = wordSplitterRelationshipProvider.getRelationships("abcdefghijklmnopqrstuvwxyz-abcdefghijklmnopqrstuvwxyz");
        Assert.notNull(relatedWordList);
        Assert.isTrue(relatedWordList.size() == 1);
        Assert.isTrue(contains(relatedWordList, "abcdefghijklmnopqrstuvwxyz-abcdefghijklmnopqrstuvwxyz", 1.0f));
    }


    @Test
    public void testRelationships4() {
        List<RelatedWord> relatedWordList = wordSplitterRelationshipProvider.getRelationships("012345678901234567890123456789-012345678901234567890123456789");
        Assert.notNull(relatedWordList);
        Assert.isTrue(relatedWordList.size() == 1);
        Assert.isTrue(contains(relatedWordList, "012345678901234567890123456789-012345678901234567890123456789", 1.0f));
    }

    @Test
    public void testRelationships5() {
        List<RelatedWord> relatedWordList = wordSplitterRelationshipProvider.getRelationships("012345678901234567890123456789");
        Assert.notNull(relatedWordList);
        Assert.isTrue(relatedWordList.size() == 1);
        Assert.isTrue(contains(relatedWordList, "012345678901234567890123456789", 1.0f));
    }



    // helper fn for testing list
    private boolean contains( List<RelatedWord> list, String word, float score ) {
        if ( list != null && word != null ) {
            for ( RelatedWord relatedWord : list ) {
                if ( relatedWord.getWord().equals(word) && relatedWord.getRelationshipStrength() == score ) {
                    return true;
                }
            }
        }
        return false;
    }


}

