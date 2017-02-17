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

package industries.vocht.viki.parser;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import industries.vocht.viki.dao.PennType;
import industries.vocht.viki.lexicon.Lexicon;
import industries.vocht.viki.model.Entity;
import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.model.SpacyTokenList;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.knowledge_base.KBEntry;
import industries.vocht.viki.model.semantics.Tuple;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.*;

/**
 * Created by peter on 31/05/16.
 *
 * test several features of the text parser
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test-context.xml"})
public class TextParserTest {

    @Autowired
    private Lexicon lexicon;

    @Autowired
    private NLParser parser;

    // test we can connect to a spacy parser locally on port 8000 and do a parse
    @Test
    public void spacyParserTest1() throws IOException {
        SpacyTokenList sDocument = spacyPeterWasHere(); // "Peter de Vocht was here.  He then moved to London."
        Assert.isTrue(sDocument != null && sDocument.getNum_sentences() == 2);
        SpacyToTuple s2t = new SpacyToTuple(lexicon);
        List<Tuple> tupleList = s2t.spacyDocumentToTupleList(sDocument);
        Assert.notNull(tupleList != null);
        Assert.isTrue(tupleList.size() == 2);
        Assert.isTrue(tupleList.get(0).toString().equals("Peter de Vocht was here ."));
        Assert.isTrue(tupleList.get(1).toString().equals("He then moved to London"));
    }

    @Test
    public void testTextParser1() throws IOException {

        UUID organisation_id = UUID.randomUUID();
        UUID eid = UUID.randomUUID();

        // setup a person semantic in the lexicon
        KBEntry entity = create(organisation_id, eid, "Peter de Vocht", "person");
        List<KBEntry> entityList = new ArrayList<>();
        entityList.add( entity );
        lexicon.addSemanticEntities(entityList, true);

        // parse
        List<Sentence> sentenceList = parser.parseText("Peter de Vocht is a person.");
        Assert.notNull(sentenceList);
        Assert.isTrue(sentenceList.size() == 1);
        Sentence sentence = sentenceList.get(0);
        Assert.notNull(sentence);
        Assert.isTrue(sentence.getTokenList().size() == 5);

        Token t1 = sentence.getTokenList().get(0);
        Assert.notNull(t1);
        Assert.notNull(t1.getPennType());
        Assert.notNull(t1.getText());
        Assert.notNull(t1.getSemantic());
        Assert.isTrue(t1.getText().equals("Peter de Vocht") && t1.getPennType() == PennType.NNP && t1.getSemantic().equals("person") );
    }

    @Test
    public void testTextParser2() throws Exception {
        List<Sentence> sentenceList = parser.parseText("2016-08-07 18:48:47");
        Assert.notNull(sentenceList);
        Assert.isTrue(sentenceList.size() == 1);
        Sentence sentence = sentenceList.get(0);
        Assert.notNull(sentence);
        Assert.isTrue(sentence.getTokenList().size() == 1);
        Token token1 = sentence.getTokenList().get(0);
        Assert.notNull(token1);
        Assert.isTrue(token1.getPennType() == PennType.CD && token1.getText().equals("2016-08-07 18:48:47"));
    }

    @Test
    public void testTextParser3() throws Exception {
        List<Sentence> sentenceList = parser.parseText("2016/08/07 18:48:47");
        Assert.notNull(sentenceList);
        Assert.isTrue(sentenceList.size() == 1);
        Sentence sentence = sentenceList.get(0);
        Assert.notNull(sentence);
        Assert.isTrue(sentence.getTokenList().size() == 1);
        Token token = sentence.getTokenList().get(0);
        Assert.isTrue(token != null && token.getPennType() == PennType.CD && token.getText().equals("2016/08/07 18:48:47"));
    }

    @Test
    public void testTextParser4() throws Exception {
        List<Sentence> sentenceList = parser.parseText("2001-09-14 02:05:43");
        Assert.notNull(sentenceList);
        Assert.isTrue(sentenceList.size() == 1);
        Sentence sentence = sentenceList.get(0);
        Assert.notNull(sentence);
        Assert.isTrue(sentence.getTokenList().size() == 1);
        Token token = sentence.getTokenList().get(0);
        Assert.isTrue(token != null && token.getPennType() == PennType.CD && token.getText().equals("2001-09-14 02:05:43"));
    }


    // do a spacy return parse, based on the output of the actual parser without running it
    // original text: "Peter de Vocht was here.  He then moved to London."
    private SpacyTokenList spacyPeterWasHere() throws IOException {
        String jsonStr = "{\"sentence_list\": [[{\"dep\": \"compound\", \"tag\": \"NNP\", \"index\": 0, \"text\": \"Peter\", \"list\": [\"2\", \"3\"]}, {\"dep\": \"compound\", \"tag\": \"FW\", \"index\": 1, \"text\": \"de\", \"list\": [\"2\", \"3\"]}, {\"dep\": \"nsubj\", \"tag\": \"NNP\", \"index\": 2, \"text\": \"Vocht\", \"list\": [\"3\"]}, {\"dep\": \"ROOT\", \"tag\": \"VBD\", \"index\": 3, \"text\": \"was\", \"list\": []}, {\"dep\": \"advmod\", \"tag\": \"RB\", \"index\": 4, \"text\": \"here\", \"list\": [\"3\"]}, {\"dep\": \"punct\", \"tag\": \".\", \"index\": 5, \"text\": \".\", \"list\": [\"3\"]}, {\"dep\": \"\", \"tag\": \"SP\", \"index\": 6, \"text\": \" \", \"list\": [\"5\", \"3\"]}], [{\"dep\": \"nsubj\", \"tag\": \"PRP\", \"index\": 7, \"text\": \"He\", \"list\": [\"9\"]}, {\"dep\": \"advmod\", \"tag\": \"RB\", \"index\": 8, \"text\": \"then\", \"list\": [\"9\"]}, {\"dep\": \"ROOT\", \"tag\": \"VBD\", \"index\": 9, \"text\": \"moved\", \"list\": []}, {\"dep\": \"prep\", \"tag\": \"IN\", \"index\": 10, \"text\": \"to\", \"list\": [\"9\"]}, {\"dep\": \"pobj\", \"tag\": \"NNP\", \"index\": 11, \"text\": \"London\", \"list\": [\"10\", \"9\"]}, {\"dep\": \"punct\", \"tag\": \".\", \"index\": 12, \"text\": \".\", \"list\": [\"9\"]}]], \"processing_time\": 0, \"num_tokens\": 13, \"num_sentences\": 2}";
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.readValue(jsonStr, SpacyTokenList.class);
    }

    // create a KBEntry object of type "entity" for the lexicon
    private KBEntry create(UUID organisation_id, UUID id, String name, String isa) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Entity entity = new Entity();
        entity.setName(name);
        entity.setIsa(isa);
        return new KBEntry(organisation_id, UUID.randomUUID(), "entity",
                "unit-test", mapper.writeValueAsString(entity));
    }


}
