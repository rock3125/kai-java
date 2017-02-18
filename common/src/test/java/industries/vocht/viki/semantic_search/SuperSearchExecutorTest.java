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

package industries.vocht.viki.semantic_search;

import com.fasterxml.jackson.databind.ObjectMapper;
import industries.vocht.viki.IDao;
import industries.vocht.viki.grammar.GrammarLibrary;
import industries.vocht.viki.indexer.Indexer;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.lexicon.Lexicon;
import industries.vocht.viki.model.Entity;
import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.indexes.DocumentIndexSet;
import industries.vocht.viki.model.knowledge_base.KBEntry;
import industries.vocht.viki.model.super_search.ISSearchItem;
import industries.vocht.viki.model.super_search.SSearchParser;
import industries.vocht.viki.model.super_search.SSearchParserException;
import industries.vocht.viki.parser.NLTimeResolver;
import industries.vocht.viki.parser.NLParser;
import industries.vocht.viki.utility.SentenceFromBinary;
import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.*;

/**F
 * Created by peter on 25/04/16.
 *
 * test the super search system works
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test-context.xml"})
public class SuperSearchExecutorTest {

    private final static int acl_hash = 1;

    @Autowired
    private Indexer indexer;

    @Autowired
    private IDao dao;

    @Autowired
    private Lexicon lexicon;

    @Autowired
    private GrammarLibrary grammarLibrary;

    @Autowired
    private NLParser parser;

    @Autowired
    private SSearchExecutor ssExecutor;


    @Test
    public void testSuperSearch1() throws Exception {

        UUID organisation_id = UUID.randomUUID();
        DateTime reference = new DateTime( 2016, 1, 1, 12, 40, 0);

        // index some keyword data
        indexDocument(organisation_id, reference, "http://1", "this is the car text to index", Document.META_BODY);
        indexDocument(organisation_id, reference, "http://2", "this is the banana text to index", Document.META_BODY);

        Map<String, DocumentIndexSet> results1 = doSearch(organisation_id, "body(index)");

        Assert.notNull(results1);
        Assert.isTrue(results1.size() == 2);
        contains(results1, "http://1");
        contains(results1, "http://2");


        Map<String, DocumentIndexSet> results2 = doSearch(organisation_id, "body(car)");
        Assert.notNull(results2);
        Assert.isTrue(results2.size() == 1);
        contains(results2, "http://1");
    }


    @Test
    public void testSuperSearch2() throws Exception {

        UUID organisation_id = UUID.randomUUID();
        DateTime reference = new DateTime( 2016, 1, 1, 12, 40, 0);

        // index some keyword data
        indexDocument(organisation_id, reference, "http://2", "Wait for father.", Document.META_BODY);
        indexDocument(organisation_id, reference, "http://1", "this is the first text to index", Document.META_BODY);
        indexDocument(organisation_id, reference, "http://2", "this is the second text to index", Document.META_BODY);

        Map<String, DocumentIndexSet> results1 = doSearch(organisation_id, "exact body(first)");
        Assert.notNull(results1);
        Assert.isTrue(results1.size() == 1);
        contains(results1, "http://1");
    }

    @Ignore
    @Test
    public void testSynsetIdWorks() throws Exception {

        UUID organisation_id = UUID.randomUUID();
        DateTime reference = new DateTime( 2016, 1, 1, 12, 40, 0);

        // index some keyword data
        indexDocument(organisation_id, reference, "http://12", "Wait for father.", Document.META_BODY);

        Map<String, DocumentIndexSet> results1 = doSearch(organisation_id, "exact body(father)");
        Assert.notNull(results1);
        Assert.isTrue(results1.size() == 1);
        contains(results1, "http://12");

        Map<String, byte[]> document = dao.getDocumentDao().getDocumentParseTreeMap(organisation_id, "http://12");
        Assert.notNull(document);
        Assert.isTrue(document.containsKey(Document.META_BODY));
        byte[] bodyBinary = document.get(Document.META_BODY);
        Assert.notNull(bodyBinary);
        Assert.isTrue(bodyBinary.length > 0);
        SentenceFromBinary sentenceFromBinary = new SentenceFromBinary();
        List<Sentence> sentenceList = sentenceFromBinary.convert(bodyBinary);
        Assert.notNull(sentenceList);
        Assert.isTrue(sentenceList.size() == 1);
        Sentence sentence = sentenceList.get(0);
        Assert.notNull(sentence);
        Assert.isTrue(sentence.getTokenList().size() == 4);
        Token token = sentence.getTokenList().get(2);
        Assert.notNull(token);
        Assert.isTrue(token.getText().equals("father") && token.getSynid() >= 0);
    }

    @Test
    public void testSuperSearch3() throws Exception {

        UUID organisation_id = UUID.randomUUID();
        DateTime reference = new DateTime( 2016, 1, 1, 12, 40, 0);

        // index some keyword data
        indexDocument(organisation_id, reference, "http://1", "this is the first text to index", Document.META_BODY);
        indexDocument(organisation_id, reference, "http://2", "this is the second position sequence place text to index", Document.META_BODY);

        Map<String, DocumentIndexSet> results1 = doSearch(organisation_id, "body(first)");

        // semantic relationship between first and second
        Assert.notNull(results1);
        Assert.isTrue(results1.size() == 2);
        contains(results1, "http://1");
        contains(results1, "http://2");

        // filter out the semantic relationship
        results1 = doSearch(organisation_id, "body(first) and not exact body(second)");

        Assert.notNull(results1);
        Assert.isTrue(results1.size() == 1);
        contains(results1, "http://1");
    }


    @Test
    public void testSuperSearch4() throws Exception {

        UUID organisation_id = UUID.randomUUID();
        DateTime reference = new DateTime( 2016, 1, 1, 12, 40, 0);

        // index some keyword data
        indexDocument(organisation_id, reference, "http://1", "My name is Peter and I am a person.", Document.META_BODY);

        Map<String, DocumentIndexSet> results1 = doSearch(organisation_id, "body(Peter,verb)");

        // not setup as a verb
        Assert.isTrue(results1 == null || results1.size() == 0);

        // filter should work
        results1 = doSearch(organisation_id, "body(Peter,nnp)");

        Assert.notNull(results1);
        Assert.isTrue(results1.size() == 1);
        Assert.isTrue(getZero(results1).getUrl().equals("http://1") );
    }

    private DocumentIndexSet getZero(Map<String, DocumentIndexSet> set) {
        DocumentIndexSet zero = null;
        if ( set != null ) {
            for ( DocumentIndexSet value : set.values() ) {
                zero = value;
                break;
            }
        }
        return zero;
    }

    @Test
    public void testSuperSearch5() throws Exception {

        UUID organisation_id = UUID.randomUUID();
        DateTime reference = new DateTime( 2016, 1, 1, 12, 40, 0);

        // index some keyword data
        indexDocument(organisation_id, reference, "http://1", "this is the car text to index", Document.META_BODY);
        indexDocument(organisation_id, reference, "http://2", "this is the banana text to index", Document.META_BODY);

        Map<String, DocumentIndexSet> results1 = doSearch(organisation_id, "exact body(banana) or exact body(car)");

        Assert.notNull(results1);
        Assert.isTrue(results1.size() == 2);
        contains(results1, "http://1");
        contains(results1, "http://2");
    }

    @Test
    public void testSuperSearch6() throws Exception {

        UUID organisation_id = UUID.randomUUID();
        DateTime reference = new DateTime( 2016, 1, 1, 12, 40, 0);

        // index some keyword data
        indexDocument(organisation_id, reference, "http://1", "this is the car text to index on 20:15", Document.META_BODY);
        indexDocument(organisation_id, reference, "http://2", "this is the banana text to index on 10:15", Document.META_BODY);

        Map<String, DocumentIndexSet> results1 = doSearch(organisation_id, "(exact body(banana) or exact body(car)) and date between 2015 and 2016-02");

        Assert.notNull(results1);
        Assert.isTrue(results1.size() == 2); // 2 indexes of type Index and 2 of type TimeIndex
        contains(results1, "http://1");
        contains(results1, "http://2");
    }

    @Test
    public void testSuperSearch7() throws Exception {

        UUID organisation_id = UUID.randomUUID();
        DateTime reference = new DateTime( 2016, 1, 1, 12, 40, 0);

        // temp enhance the lexicon with semantic entities to match
        List<KBEntry> entityList = new ArrayList<>();
        entityList.add( create(organisation_id, UUID.randomUUID(), "Peter", "person") );
        entityList.add( create(organisation_id, UUID.randomUUID(), "Mark", "person") );
        entityList.add( create(organisation_id, UUID.randomUUID(), "Auckland", "location") );
        entityList.add( create(organisation_id, UUID.randomUUID(), "Sydney", "location") );
        lexicon.addSemanticEntities(entityList, true);

        // index some keyword data
        indexDocument(organisation_id, reference, "http://1", "Peter wrote this text in Sydney.", Document.META_BODY);
        indexDocument(organisation_id, reference, "http://2", "Mark read this text in Auckland", Document.META_BODY);

        Map<String, DocumentIndexSet> results1 = doSearch(organisation_id, "(exact person(Peter) or exact location(Auckland))");

        Assert.notNull(results1);
        Assert.isTrue(results1.size() == 2);
        contains(results1, "http://1");
        contains(results1, "http://2");
    }

    @Test
    public void testSuperSearch8() throws Exception {

        UUID organisation_id = UUID.randomUUID();
        DateTime reference = new DateTime( 2016, 1, 1, 12, 40, 0);

        // index some keyword data
        indexDocument(organisation_id, reference, "http://1", "this is the car text to index on 20:15", Document.META_BODY);
        indexDocument(organisation_id, reference, "http://2", "this is the banana text to index on 10:15", Document.META_BODY);

        Map<String, DocumentIndexSet> results1 = doSearch(organisation_id, "(exact body(banana) or exact body(car)) and date before 2017");

        Assert.notNull(results1);
        Assert.isTrue(results1.size() == 2); // 2 indexes of type Index and 2 of type TimeIndex
        contains(results1, "http://1");
        contains(results1, "http://2");
    }

    @Test
    public void testSuperSearch9() throws Exception {

        UUID organisation_id = UUID.randomUUID();
        DateTime reference = new DateTime( 2016, 1, 1, 12, 40, 0);

        // index some keyword data
        indexDocument(organisation_id, reference, "http://1", "this is the car text to index on 20:15", Document.META_BODY);
        indexDocument(organisation_id, reference, "http://2", "this is the banana text to index on 10:15", Document.META_BODY);

        Map<String, DocumentIndexSet> results1 = doSearch(organisation_id, "(exact body(banana) or exact body(car)) and date after 2007");

        Assert.notNull(results1);
        Assert.isTrue(results1.size() == 2); // 2 indexes of type Index and 2 of type TimeIndex
        contains(results1, "http://1");
        contains(results1, "http://2");
    }

    //////////////////////////////////////////////////////////////////////////////////
    // helpers


    private void indexDocument(UUID organisation_id, DateTime referenceDate, String url,
                               String text, String metadata ) throws IOException, InterruptedException {
        List<Sentence> sentenceList = parser.parseText(text);
        List<Token> tokenList = new ArrayList<>();
        for ( Sentence sentence : sentenceList ) {
            tokenList.addAll(sentence.getTokenList());
        }
        new NLTimeResolver().resolveTimeTokens(filterDateTime(tokenList), referenceDate,
                grammarLibrary.getGrammarConversionMap() );
        indexer.indexDocument( organisation_id, url, metadata, acl_hash, sentenceList );

        // save the document for retrieval
        SentenceFromBinary sentenceFromBinary = new SentenceFromBinary();
        byte[] data = sentenceFromBinary.convert(sentenceList);
        Map<String,byte[]> documentMap = new HashMap<>();
        documentMap.put(metadata, data);
        dao.getDocumentDao().saveDocumentParseTreeMap(organisation_id, url, documentMap);
    }

    // remove any tokens that aren't date/time rule tokens
    private List<Token> filterDateTime( List<Token> tokenList ) {
        List<Token> newList = new ArrayList<>();
        for ( Token t : tokenList ) {
            if ( t.getGrammarRuleName() != null && (t.getGrammarRuleName().startsWith("date.") ||
                    t.getGrammarRuleName().startsWith("time.")) ) {
                newList.add(t);
            }
        }
        return newList;
    }

    // make sure url is in the list of indexes
    private void contains( Map<String, DocumentIndexSet> list, String url ) {
        boolean found = false;
        if ( list != null ) {
            for ( DocumentIndexSet set : list.values() ) {
                if ( set.getUrl().equals(url) ) {
                    found = true;
                }
            }
        }
        Assert.isTrue(found);
    }


    // helper function - call super search
    private Map<String, DocumentIndexSet> doSearch(UUID organisation_id, String str ) throws SSearchParserException {

        ISSearchItem searchItem = new SSearchParser().parse(str);
        HashSet<Integer> aclSet  = new HashSet<>();
        aclSet.add(acl_hash);

        Map<String, Integer> searchMap = new HashMap<>();
        Map<String, Integer> keywordMap = new HashMap<>();
        ssExecutor.getSearchRelationshipMap(searchItem, searchMap, keywordMap);

        return ssExecutor.doSearch(organisation_id, searchItem, aclSet, searchMap, keywordMap);
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


