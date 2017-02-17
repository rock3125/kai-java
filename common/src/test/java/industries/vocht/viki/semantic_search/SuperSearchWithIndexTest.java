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

import industries.vocht.viki.ApplicationException;
import industries.vocht.viki.IDao;
import industries.vocht.viki.IHazelcast;
import industries.vocht.viki.datastructures.IntList;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.indexer.Indexer;
import industries.vocht.viki.model.*;
import industries.vocht.viki.model.indexes.Index;
import industries.vocht.viki.model.indexes.DocumentIndexSet;
import industries.vocht.viki.model.search.SearchObject;
import industries.vocht.viki.model.search.SearchResult;
import industries.vocht.viki.model.search.SearchResultList;
import industries.vocht.viki.model.super_search.SSearchParserException;
import industries.vocht.viki.parser.NLParser;
import industries.vocht.viki.services.TestBase;
import industries.vocht.viki.utility.SentenceFromBinary;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Created by peter on 15/04/16.
 *
 * test the indexer and super search engine
 *
 */
public class SuperSearchWithIndexTest extends TestBase {

    @Autowired
    private Indexer indexer;

    @Autowired
    private NLParser parser;

    @Autowired
    private SuperSearch superSearch;

    @Autowired
    private IDao dao;

    @Autowired
    private IHazelcast hazelcast;

    // score points for if the word itself was found
    @Value("${search.score.exact.hit:100.0f}")
    private float fullHit;

    // score points for a hit through a related word (e.g. synonym)
    @Value("${search.score.related.hit:2.5f}")
    private float relatedHit;

    // score points for a hit through a relationship of another word to this word
    @Value("${search.score.related.hit:0.5f}")
    private float inverseRelatedHit;


    // test indexer andex search results after index
    @Test
    public void testIndexer1() throws IOException, ApplicationException, SSearchParserException, InterruptedException {
        // basic organisation id and url
        UUID organisation_id = org1.getId();
        String url1 = "http://www.test.com/peter";

        // load the text to be indexed
        String testDocument = loadTestText();
        Assert.notNull(testDocument);

        indexText( organisation_id, url1, ACL, testDocument );

        // setup fake security
        String security_email = "peter@peter.co.nz";
        HashSet<Integer> set = new HashSet<>();
        set.add(1);
        hazelcast.getUserAclMap(organisation_id).put(security_email, set);

        // perform a search and see if the scoring is working
        SearchResultList searchResult = search(sessionID.toString(), organisation_id, "gas", security_email);
        List<SearchResult> resultList = searchResult.getSearch_result_list();
        Assert.notNull(resultList);
        Assert.isTrue(resultList.size() == 1);
        Assert.isTrue( resultList.get(0).getText_list().size() == 2 );

        String text1 = resultList.get(0).getText_list().get(0);
        Assert.notNull(text1);
        String text2 = resultList.get(0).getText_list().get(1);
        Assert.notNull(text2);

        Assert.isTrue( text1.contains("AGA{:hl3}") );
        Assert.isTrue( text2.contains("AGA{:hl3}") );
    }



    // test indexer and search results after index
    @Test
    public void testIndexer2() throws IOException, ApplicationException, SSearchParserException, InterruptedException {
        // basic organisation id and url
        UUID organisation_id = org1.getId();
        int acl_hash = ACL;
        String url1 = "http://www.test.com/peter1";
        String url2 = "http://www.test.com/peter2";
        String url3 = "http://www.test.com/peter3";
        String url4 = "http://www.test.com/peter4";

        // setup fake security
        String security_email = "peter@peter.co.nz";
        HashSet<Integer> set = new HashSet<>();
        set.add(acl_hash);
        hazelcast.getUserAclMap(organisation_id).put(security_email, set);

        // related to gas through word-associations
        indexText(organisation_id,url1, acl_hash, "Test the word Aga is indexed properly and scores.");

        // related to gas through word-associations
        indexText(organisation_id,url2, acl_hash, "Test the word flatulence is indexed properly and scores.");

        // related to gas through word-associations
        indexText(organisation_id,url3, acl_hash, "Test the word emission is indexed properly and scores.");

        // the word itself
        indexText(organisation_id,url4, acl_hash, "Test the word gas is indexed properly and scores.");

        // perform a search and see if the scoring is working
        SearchObject searchObject = new SearchObject();
        searchObject.setMetadata(Document.META_BODY);
        searchObject.setSearch_text("gas"); // related to Aga
        searchObject.setEmail(security_email);
        SearchResultList searchResultList = superSearch.doSearch( sessionID.toString(), EMAIL_USER_1, organisation_id, searchObject, 0, 1000, 0);
        Assert.notNull(searchResultList);
        List<SearchResult> resultList = searchResultList.getSearch_result_list();
        Assert.notNull(resultList);
        Assert.isTrue(resultList.size() == 4);

        // first hit is a "full hit" - exact
        Assert.isTrue( resultList.get(0).getUrl().equals(url4) );
        Assert.isTrue( resultList.get(0).getScore() == fullHit );

        for ( int i = 1; i < 4; i++ ) {
            Assert.isTrue( resultList.get(i).getScore() == relatedHit );
        }
    }



    // test indexer and search results after index
    @Test
    public void testIndexer3() throws IOException, ApplicationException, SSearchParserException, InterruptedException {
        // basic organisation id and url
        UUID organisation_id = org1.getId();
        int acl_hash = ACL;
        String url1 = "http://www.test.com/peter1";
        String url2 = "http://www.test.com/peter2";
        String url3 = "http://www.test.com/peter3";
        String url4 = "http://www.test.com/peter4";

        // setup fake security
        String security_email = "peter@peter.co.nz";
        HashSet<Integer> set = new HashSet<>();
        set.add(acl_hash);
        hazelcast.getUserAclMap(organisation_id).put(security_email, set);

        // related to gas through word-associations
        indexText(organisation_id,url1, acl_hash, "Test the word Aga is indexed properly and scores.");

        // related to gas through word-associations
        indexText(organisation_id,url2, acl_hash, "Test the word flatulence is indexed properly and scores.  Flatulence is a form of gas.");

        // related to gas through word-associations
        indexText(organisation_id,url3, acl_hash, "Test the word emission is indexed properly and scores.  Also gas, gas, gas is good!");

        // the word itself
        indexText(organisation_id,url4, acl_hash, "Test the word gas is indexed properly and scores.  And there is a repetition of gas.");

        // perform a search and see if the scoring is working
        SearchObject searchObject = new SearchObject();
        searchObject.setMetadata(Document.META_BODY);
        searchObject.setSearch_text("gas"); // related to Aga
        searchObject.setEmail(security_email);
        SearchResultList searchResultList = superSearch.doSearch( sessionID.toString(), EMAIL_USER_1, organisation_id, searchObject, 0, 1000, 0);
        Assert.notNull(searchResultList);
        List<SearchResult> resultList = searchResultList.getSearch_result_list();
        Assert.notNull(resultList);
        Assert.isTrue(resultList.size() == 4);

        Assert.isTrue( resultList.get(0).getUrl().equals(url3) );
        Assert.isTrue( resultList.get(0).getScore() == (fullHit * 3 + relatedHit) );

        Assert.isTrue( resultList.get(1).getUrl().equals(url4) );
        Assert.isTrue( resultList.get(1).getScore() == (fullHit * 2) );

        Assert.isTrue( resultList.get(2).getUrl().equals(url2) );
        Assert.isTrue( resultList.get(2).getScore() == (fullHit + relatedHit * 2) );

        Assert.isTrue( resultList.get(3).getUrl().equals(url1) );
        Assert.isTrue( resultList.get(3).getScore() == (relatedHit) );
    }




    // test indexer and search multiple word results after index
    @Test
    public void testIndexer4() throws IOException, ApplicationException, SSearchParserException, InterruptedException {
        // basic organisation id and url
        UUID organisation_id = org1.getId();
        int acl_hash = ACL;
        String url1 = "http://www.test.com/peter1";
        String url2 = "http://www.test.com/peter2";
        String url3 = "http://www.test.com/peter3";
        String url4 = "http://www.test.com/peter4";

        // setup fake security
        String security_email = "peter@peter.co.nz";
        HashSet<Integer> set = new HashSet<>();
        set.add(acl_hash);
        hazelcast.getUserAclMap(organisation_id).put(security_email, set);

        // related to gas through word-associations
        indexText(organisation_id, url1, acl_hash, "Stark terror ruled the Inner-Flight ship on that last Mars-Terra run. For the black-clad Leiters were on the prowl ... and the grim red planet was not far behind.");

        // related to gas through word-associations
        indexText(organisation_id, url2, acl_hash, "The metallic rasp of the speaker echoed through the corridors of the great ship. The passengers glanced at each other uneasily, murmuring and peering out the port windows at the small speck below outside the ship.");

        // related to gas through word-associations
        indexText(organisation_id, url3, acl_hash, "\"What's up?\" an anxious passenger asked one of the pilots, hurrying through the ship to check the escape lock.");

        // the word itself
        indexText(organisation_id, url4, acl_hash, "\"There's something going on,\" a woman passenger said nervously. \"Lord, I thought we were finally through with those Martians. Now what?\"");

        // perform a search and see if the scoring is working
        SearchObject searchObject = new SearchObject();
        searchObject.setMetadata(Document.META_BODY);
        searchObject.setSearch_text("ship passengers");
        searchObject.setEmail(security_email);
        SearchResultList searchResultList = superSearch.doSearch(sessionID.toString(), EMAIL_USER_1, organisation_id, searchObject, 0, 1000, 0);
        Assert.notNull(searchResultList);

        List<SearchResult> resultList = searchResultList.getSearch_result_list();
        Assert.notNull(resultList);
        Assert.isTrue(resultList.size() == 3);

        Assert.isTrue(getUrl(resultList, url2) != null);
        Assert.isTrue(getUrl(resultList, url3) != null);

        Assert.isTrue(getUrl(resultList, url2).getScore() > getUrl(resultList, url3).getScore());
    }


    ////////////////////////////////////////////////////////////////////////////////////////

    // does the search result list contain url?  return the result or null
    private SearchResult getUrl( List<SearchResult> resultList, String url ) {
        if ( resultList != null && url != null ) {
            for ( SearchResult result : resultList ) {
                if ( result.getUrl() != null && result.getUrl().equals(url) ) {
                    return result;
                }
            }
        }
        return null;
    }

    // perform a text index
    private void indexText( UUID organisation_id, String url, int acl_hash, String text )
            throws IOException, InterruptedException {
        // parse it
        List<Sentence> sentenceList = parser.parseText(text);
        Assert.notNull(sentenceList);
        Assert.isTrue( sentenceList.size() > 0 );

        // save the document so it can be retrieved by search
        SentenceFromBinary sentenceFromBinary = new SentenceFromBinary();
        Map<String, byte[]> documentMap = new HashMap<>();
        documentMap.put( Document.META_BODY, sentenceFromBinary.convert(sentenceList) );
        dao.getDocumentDao().saveDocumentParseTreeMap(organisation_id, url, documentMap);

        // get the owner of this document

        // index the document in the system
        indexer.indexDocument( organisation_id, url, Document.META_BODY, acl_hash, sentenceList );
    }

    // perform a test search
    private SearchResultList search(String sessionID, UUID organisation_id, String text, String security_email)
            throws IOException, ApplicationException, SSearchParserException, InterruptedException {
        // perform a search and see if the scoring is working
        SearchObject searchObject = new SearchObject();
        searchObject.setSearch_text(text); // related to Aga
        searchObject.setMetadata(Document.META_BODY);
        searchObject.setEmail(security_email);
        return superSearch.doSearch( sessionID, EMAIL_USER_1, organisation_id, searchObject, 0, 1000, 0);
    }

    private String loadTestText() throws IOException {
        InputStream in = getClass().getResourceAsStream("/common/aga-test.txt");
        Assert.notNull(in);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
        }
        return out.toString();
    }

}

