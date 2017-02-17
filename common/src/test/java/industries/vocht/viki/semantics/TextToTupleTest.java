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

package industries.vocht.viki.semantics;

import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.semantics.Tuple;
import industries.vocht.viki.parser.NLParser;
import industries.vocht.viki.tokenizer.Tokenizer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by peter on 10/06/16.
 *
 * test how various sentences convert from raw text to different
 * case tuples and their correctness
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test-context.xml"})
public class TextToTupleTest {

    @Autowired
    private NLParser parser;


    @Test
    public void testCaseGenerator1() throws Exception {

        List<Sentence> sentenceList = parser.parseText("the quick dog jumped over the slow brown fox.");
        Assert.isTrue(sentenceList != null && sentenceList.size() == 1);
        List<Tuple> caseList = new ArrayList<>();
        for ( Sentence sentence : sentenceList ) {
            caseList.add(sentence.getTuple());
        }
        Assert.notNull(caseList);
        Assert.isTrue(caseList.size() == 1 );
        // jumped(nsubj=the quick dog|prep=over the slow brown fox)
        testCase( caseList.get(0), "jumped", "nsubj=the quick dog", "pobj=over the slow brown fox");
    }

    @Test
    public void testCaseGenerator2() throws Exception {
        List<Sentence> sentenceList = parser.parseText("Peter washed his car and then flew to Dallas.");
        Assert.isTrue(sentenceList != null && sentenceList.size() == 1);
        List<Tuple> caseList = new ArrayList<>();
        for ( Sentence sentence : sentenceList ) {
            caseList.add(sentence.getTuple());
        }
        Assert.notNull(caseList);
        Assert.isTrue(caseList.size() == 1 );
        // washed(nsubj=Peter|dobj=his car|cc=and|conj=then flew to Dallas)
        testCase( caseList.get(0), "washed", "nsubj=Peter", "dobj=his car", "pobj=then flew to Dallas");
    }

    @Test
    public void testTupleCreation3() throws Exception {
        List<Sentence> sentenceList = parser.parseText("John was second in his regiment.");
        Assert.isTrue(sentenceList != null && sentenceList.size() == 1);
        List<Tuple> caseList = new ArrayList<>();
        for ( Sentence sentence : sentenceList ) {
            caseList.add(sentence.getTuple());
        }
        Assert.notNull(caseList);
        Assert.isTrue(caseList.size() == 1 );
        // was(nsubj=John|acomp=second in his regiment)
        testCase( caseList.get(0), "was", "nsubj=John", "acomp=second in his regiment");
    }

    // test some from second variety 1
    @Test
    public void testStructure3() throws IOException {
        String sentenceArray = "The claws were bad enough in the first place — nasty, " +
                "crawling little death-robots. But when they began to imitate their creators, " +
                "it was time for the human race to make peace—if it could!";
        List<Sentence> sentenceList = parser.parseText(sentenceArray);
        Assert.isTrue(sentenceList != null && sentenceList.size() > 0 );
        List<Tuple> caseList = new ArrayList<>();
        for ( Sentence sentence : sentenceList ) {
            caseList.add(sentence.getTuple());
        }
        Assert.notNull(caseList);
        Assert.isTrue(caseList.size() == 2);
        // were(nsubj=The claws|acomp=bad enough|prep=in the first place - nasty|advcl=crawling little death - robots)
        testCase( caseList.get(0), "were", "nsubj=The claws");
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    // helper functions

    // test a case has the required components - pass null for a component to ignore it
    private void testCase(Tuple case1, String verb, String... parts) {
        Assert.notNull(case1);
        if ( verb != null ) {
            Assert.isTrue( case1.getRoot() != null && case1.getRoot().toString().equals(verb) );
        }
        // check parts
        Tokenizer tokenizer = new Tokenizer();
        if ( parts != null && parts.length > 0 ) {
            for ( String part : parts ) {
                String[] items = part.split("=");
                Assert.isTrue(items.length == 2);
                List<Token> tokenList = case1.getRoot().findSrlParameter(items[0]);
                Assert.notNull(tokenList);
                Assert.isTrue(tokenList.size() > 0);
                String str = tokenizer.toString(tokenList).trim();
                Assert.notNull(str);
                Assert.isTrue(str.compareToIgnoreCase(items[1]) == 0);
            }
        }
    }

}


