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

package industries.vocht.viki.srl_parser;

import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.semantics.Tuple;
import industries.vocht.viki.parser.NLParser;
import industries.vocht.viki.tokenizer.Tokenizer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by peter on 29/07/16.
 *
 * test the dependecy parser and tuple creation system
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test-context.xml"})
public class SRLParserTest {

    @Autowired
    private NLParser parser;

    @Test
    public void test1() throws Exception {

        String[] sentenceList = new String[] {
                "Most of the summer I spent in a stupor, sitting either in my once or in new restaurants, in my apartment watching videotapes or in the backs of cabs, in nightclubs that just opened or in movie theaters, at the building in Hell’s Kitchen or in new restaurants.",
                "There were four major air disasters this summer, the majority of them captured on videotape, almost as if these events had been planned, and repeated on television endlessly.",
                "The planes kept crashing in slow motion, followed by countless roaming shots of the wreckage and the same random views af the burned, bloody carnage, weeping rescue workers retrieving body parts.",
                "I started using Oscar de la Renta men’s deodorant, which gave me a slight rash.",
                "A movie about a small talking bug was released to great fanfare and grossed over two hundred million dollars.",
                "The Mets were doing badly.",
                "Beggars and homeless seemed to have multiplied in August and the ranks of the unfortunate, weak and aged lined the streets everywhere.",
                "I found myself asking too many summer associates at too many dinners in flashy new restaurants before taking them to Les Mis'erables if anyone had seen The Toolbox Murders on HBO and silent tables would stare back at me, before I would cough politely and summon the waiter over for the check, or I’d ask for sorbet or, if this was earlier in the dinner, for another bottle of San Pellegrino, and then I’d ask the summer associates, “No?” and assure them, “It was quite good.”",
                "My platinum American Express card had gone through so much use that it snapped in half, self-destructed, at one of those dinners, when I took two summer associates to Restless and Young, the new Pablo Lester restaurant in midtown, but I had enough cash in my gazelleskin wallet to pay for the meal.",
                "The Patty Winters Show s were all repeats.",
                "Life remained a blank canvas, a clich'e, a soap opera. I felt lethal, on the verge of frenzy.",
                "My nightly bloodlust overflowed into my days and I had to leave the city.",
                "My mask of sanity was a victim of impending slippage.",
                "This was the bone season for me and I needed a vacation.",
                "I needed to go to the Hamptons."
        };

        int numTuples = 0;
        for ( String sentence : sentenceList ) {
            List<Sentence> list = parser.parseText(sentence);
            List<Tuple> caseList = new ArrayList<>();
            for ( Sentence s : list ) {
                if ( s.getTuple() != null ) {
                    caseList.add(s.getTuple());
                }
            }
            //System.out.println("------------------------------");
            for ( Tuple tuple : caseList ) {
                numTuples = numTuples + 1;
                //System.out.println(tuple.toString());
            }
        }
        Assert.isTrue( numTuples == 16 );

    }


    @Test
    public void test2() throws Exception {

        String[] sentenceList = new String[] {
                "I needed to go to the Hamptons."
        };


        for ( String sentence : sentenceList ) {
            List<Sentence> list = parser.parseText(sentence);
            List<Tuple> tupleList = new ArrayList<>();
            for ( Sentence s : list ) {
                if ( s.getTuple() != null ) {
                    tupleList.add(s.getTuple());
                }
            }
            Assert.notNull(tupleList);
            Assert.isTrue(tupleList.size() == 1);
            Tuple tuple = tupleList.get(0);

            // needed(nsubj= I|xcomp=to go to the Hamptons)
            Map<String, String> parts1 = new HashMap<>();
            parts1.put("nsubj", "I");
            parts1.put("xcomp", "to go to the Hamptons");
            Assert.isTrue( equals(tuple, "needed", parts1) );
        }

    }

    /**
     * check a tuple matches the parameters and verb
     * @param tuple the tuple to check
     * @param verb the expected verb of the tuple
     * @param parts the parts of the tuple
     * @return true if it all matches fine
     */
    private boolean equals( Tuple tuple, String verb, Map<String, String> parts ) {
        if ( tuple != null && parts != null ) {
            if ( tuple.getRoot() != null && tuple.getRoot().toString().equals(verb) ) {
                for ( String param : parts.keySet() ) {
                    boolean found = false;
                    List<Token> tokenList = tuple.getRoot().findSrlParameter(param);
                    if ( tokenList != null ) {
                        String tokenStr = new Tokenizer().toString(tokenList).trim();
                        if (tokenStr.equals(parts.get(param))) {
                            found = true;
                        }
                    }
                    if (!found) {
                        return false;
                    }
                }
                return true; // all matched
            }
        }
        return false;
    }


}

