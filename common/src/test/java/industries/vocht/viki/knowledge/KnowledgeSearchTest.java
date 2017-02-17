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

package industries.vocht.viki.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import industries.vocht.viki.ApplicationException;
import industries.vocht.viki.IDao;
import industries.vocht.viki.indexer.TupleIndexer;
import industries.vocht.viki.lexicon.Lexicon;
import industries.vocht.viki.model.Entity;
import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.model.indexes.IIndex;
import industries.vocht.viki.model.knowledge_base.KBEntry;
import industries.vocht.viki.model.semantics.Tuple;
import industries.vocht.viki.model.semantics.TupleQuery;
import industries.vocht.viki.model.semantics.TupleResultList;
import industries.vocht.viki.model.super_search.SSearchParserException;
import industries.vocht.viki.parser.NLParser;
import industries.vocht.viki.parser.TupleQueryParser;
import industries.vocht.viki.semantic_search.TupleSSearchExecutor;
import industries.vocht.viki.semantic_search.TupleSearch;
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
public class KnowledgeSearchTest {

    private final static int acl_hash = 1;

    @Autowired
    private IDao dao;

    @Autowired
    private Lexicon lexicon;

    @Autowired
    private NLParser parser;

    @Autowired
    private TupleIndexer tupleIndexer;

    @Autowired
    private TupleQueryParser tupleQueryParser;

    @Autowired
    private TupleSearch tupleSearch;

    @Autowired
    private TupleSSearchExecutor ssExecutor;

    @Autowired
    private TupleSearch tSearch;

    @Test
    public void testKS1() throws InterruptedException, IOException, SSearchParserException {

        UUID organisation_id = UUID.randomUUID();
        String url = "http://test1";

        // setup semantics
        lexicon.addSemanticEntity(create(organisation_id, UUID.randomUUID(), "Grossman", "person"));
        lexicon.addSemanticEntity(create(organisation_id, UUID.randomUUID(), "Kramer", "person"));

        String story = "“What we came here about, Professor,” Grossman began, but Kramer cut him off with an impatient wave.\n" +
                "“Let me talk. Can’t you and your men get out of here long enough to let me talk to him?”\n" +
                "Grossman swallowed. “All right, Kramer.” He nodded to the two men. " +
                "The three of them left the room, going out into the hall and closing the door after them.\n" +
                "The old man in the bed watched Kramer silently. “I don’t think much of him,” he said at last. " +
                "“I’ve seen his type before. What’s he want?”";

        // parse
        List<Sentence> sentenceList = parser.parseText(story);
        Assert.notNull(sentenceList);

        for ( Sentence sentence : sentenceList ) {
            Tuple tuple = sentence.getTuple();
            if ( tuple != null ) {
                tuple.setOrganisation_id(organisation_id);
                tuple.setUrl(url);
                tupleIndexer.indexTuple(organisation_id, tuple, acl_hash);
            }
        }

        doQuery(organisation_id, "Who swallowed?", 1);
        doQuery(organisation_id, "who doesn't think much of him?", 1);
        doQuery(organisation_id, "Who cut him off?", 1);

        // something and somewhat filtering
        doQuery(organisation_id, "Who swallowed something?", 1);
        doQuery(organisation_id, "Who swallowed what?", 1);
        doQuery(organisation_id, "Who swallowed it?", 1);
        doQuery(organisation_id, "Who swallowed a thing?", 1);
    }

    @Test
    public void testKS2() throws ApplicationException, SSearchParserException, IOException, InterruptedException {

        UUID organisation_id = UUID.randomUUID();
        String url = "http://test2";

        // setup semantics
        lexicon.addSemanticEntity(create(organisation_id, UUID.randomUUID(), "Grossman", "person"));
        lexicon.addSemanticEntity(create(organisation_id, UUID.randomUUID(), "Kramer", "person"));

        String story = "“What we came here about, Professor,” Grossman began, but Kramer cut him off with an impatient wave.\n" +
                "“Let me talk. Can’t you and your men get out of here long enough to let me talk to him?”\n" +
                "Grossman swallowed. “All right, Kramer.” He nodded to the two men. " +
                "The three of them left the room, going out into the hall and closing the door after them.\n" +
                "The old man in the bed watched Kramer silently. “I don’t think much of him,” he said at last. " +
                "“I’ve seen his type before. What’s he want?”";

        // parse
        List<Sentence> sentenceList = parser.parseText(story);
        Assert.notNull(sentenceList);

        for ( Sentence sentence : sentenceList ) {
            Tuple tuple = sentence.getTuple();
            if ( tuple != null ) {
                tuple.setOrganisation_id(organisation_id);
                tuple.setUrl(url);
                tupleIndexer.indexTuple(organisation_id, tuple, acl_hash);
            }
        }

        // setup security
        HashSet<Integer> accessSet = new HashSet<>();
        accessSet.add(acl_hash);

        TupleResultList trList = tSearch.tupleSearch(organisation_id, accessSet, "who doesn't think much of him?", 0, 10);
        Assert.notNull(trList);
        Assert.notNull(trList.getCaseTupleList());
        Assert.isTrue(trList.getCaseTupleList().size() == 1);
        String hlString = trList.getCaseTupleList().get(0).getTupleText();
        Assert.isTrue(hlString != null &&
                hlString.equals(" {hl1:}I {:hl1} do n't {hl1:} think {:hl1} much of {hl1:} him {:hl1} {hl1:} he {:hl1} said at last"));

        TupleResultList trList2 = tSearch.tupleSearch(organisation_id, accessSet, "who watched Kramer silently?", 0, 10);
        Assert.notNull(trList2);
        Assert.notNull(trList2.getCaseTupleList());
        Assert.isTrue(trList2.getCaseTupleList().size() == 1);
        String hlString2 = trList2.getCaseTupleList().get(0).getTupleText();
        Assert.isTrue(hlString2 != null &&
                hlString2.equals("The old {hl1:} man {:hl1} in the bed {hl2:} watched {:hl2} {hl1:} Kramer {:hl1} silently"));
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * helper - perform a query and check the number of results
     * @param organisation_id the id of the organisation
     * @param queryStr the string to query with
     * @param numResultsToCheck the number of results to check
     */
    private void doQuery(UUID organisation_id, String queryStr, int numResultsToCheck) throws IOException, SSearchParserException {
        TupleQuery query = tupleQueryParser.parseQuery(queryStr);
        Assert.notNull(query);
        Assert.isTrue(query.getTargetMetadata() != null && query.getTargetMetadata().equals(Tuple.META_TUPLE));
        Assert.notNull(query.getSearchItem());

        // setup security
        HashSet<Integer> accessSet = new HashSet<>();
        accessSet.add(acl_hash);

        Map<String, Integer> searchMap = new HashMap<>();
        List<IIndex> indexSet = ssExecutor.doSearch(organisation_id, query.getSearchItem(), accessSet, searchMap );
        Assert.notNull(indexSet);
        Assert.isTrue(indexSet.size() == numResultsToCheck); // check number of results is correct
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
