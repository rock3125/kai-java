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

import com.hazelcast.core.IMap;
import industries.vocht.viki.IDao;
import industries.vocht.viki.IHazelcast;
import industries.vocht.viki.indexer.TupleIndexer;
import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.model.indexes.DocumentIndex;
import industries.vocht.viki.model.indexes.DocumentIndexSet;
import industries.vocht.viki.model.indexes.IIndex;
import industries.vocht.viki.model.indexes.Index;
import industries.vocht.viki.model.semantics.Tuple;
import industries.vocht.viki.model.super_search.ISSearchItem;
import industries.vocht.viki.model.super_search.SSearchWord;
import industries.vocht.viki.model.semantics.TupleQuery;
import industries.vocht.viki.parser.NLParser;
import industries.vocht.viki.parser.TupleQueryParser;
import industries.vocht.viki.semantic_search.SSearchExecutor;
import industries.vocht.viki.semantic_search.TupleSearch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.*;

/**
 * Created by peter on 8/06/16.
 *
 * test the semantic compression system, the case tuple system,
 * our main NLU system
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test-context.xml"})
public class SemanticCompressionTest {

    @Autowired
    private IHazelcast hazelcast;

    @Autowired
    private IDao dao;

    @Autowired
    private NLParser parser;

    @Autowired
    private TupleIndexer tupleIndexer;

    @Autowired
    private SSearchExecutor ssExecutor;

    @Autowired
    private TupleQueryParser tupleQueryParser;

    @Autowired
    private TupleSearch tupleSearch;


    @Value("${hazelcast.semantic.shard.offset:1000000}")
    private int semanticShardOffset;


    // test an index and search for an item
    @Test
    public void testCaseIndexFind1() throws Exception {
        UUID org_id = UUID.randomUUID();
        String url = "http://testCaseIndexFind1";
        int acl_hash = 1;
        List<Sentence> sentenceList = parser.parseText("the quick dog jumped over the slow brown fox.");
        Assert.isTrue(sentenceList != null && sentenceList.size() == 1);
        List<Tuple> caseList = new ArrayList<>();
        for ( Sentence sentence : sentenceList ) {
            if ( sentence.getTuple() != null ) {
                sentence.getTuple().setOrganisation_id(org_id);
                sentence.getTuple().setUrl(url);
                caseList.add(sentence.getTuple());
            }
        }
        Assert.notNull(caseList);
        Assert.isTrue(caseList.size() == 1);
        Tuple case1 = caseList.get(0);
        tupleIndexer.indexTuple(org_id, case1, acl_hash );

        ISSearchItem searchItem = new SSearchWord("dog", null, Tuple.META_TUPLE, semanticShardOffset, 0, true );

        // setup security
        HashSet<Integer> accessSet = new HashSet<>();
        accessSet.add(acl_hash);

        Map<String, Integer> searchMap = new HashMap<>();
        Map<String, Integer> keywordMap = new HashMap<>();
        ssExecutor.getSearchRelationshipMap(searchItem, searchMap, keywordMap);

        Map<String, DocumentIndexSet> indexSet = ssExecutor.doSearch(org_id, searchItem, accessSet, searchMap, keywordMap );
        Assert.notNull(indexSet);
        Assert.isTrue(indexSet.size() == 1);
        DocumentIndexSet index = null;
        for ( DocumentIndexSet set : indexSet.values() ) {
            index = set;
        }
        Assert.notNull(index);
        Assert.isTrue( index.getUrl().equals( case1.getId().toString() ) );
    }

    @Test
    public void testCaseTupleQueryParser1() throws Exception {

        UUID org_id = UUID.randomUUID();
        String url = "http://testCaseTupleQueryParser1";

        int acl_hash = 1;
        List<Sentence> sentenceList = parser.parseText("the quick dog jumped over the slow brown fox.");
        Assert.isTrue(sentenceList != null && sentenceList.size() == 1);
        List<Tuple> caseList = new ArrayList<>();
        for ( Sentence sentence : sentenceList ) {
            if ( sentence.getTuple() != null ) {
                sentence.getTuple().setOrganisation_id(org_id);
                sentence.getTuple().setUrl(url);
                caseList.add(sentence.getTuple());
            }
        }
        Assert.notNull(caseList);
        Assert.isTrue(caseList.size() == 1);
        Tuple case1 = caseList.get(0);
        tupleIndexer.indexTuple(org_id, case1, acl_hash );

        TupleQuery query = tupleQueryParser.parseQuery("what did the dog do?");
        Assert.notNull(query);
        Assert.isTrue(query.getTargetMetadata() != null && query.getTargetMetadata().equals(Tuple.META_TUPLE));
        Assert.notNull(query.getSearchItem());

        // setup security
        HashSet<Integer> accessSet = new HashSet<>();
        accessSet.add(acl_hash);

        Map<String, Integer> searchMap = new HashMap<>();
        Map<String, Integer> keywordMap = new HashMap<>();
        ssExecutor.getSearchRelationshipMap(query.getSearchItem(), searchMap, keywordMap);

        Map<String, DocumentIndexSet> indexSet = ssExecutor.doSearch(org_id, query.getSearchItem(), accessSet, searchMap, keywordMap );
        Assert.notNull(indexSet);
        Assert.isTrue(indexSet.size() == 1); // 1 - only one sentence
        DocumentIndexSet index = null;
        for ( DocumentIndexSet set : indexSet.values() ) {
            index = set;
        }
        Assert.notNull(index);
        Assert.isTrue( index.getUrl().equals( case1.getId().toString() ) );
    }


}

