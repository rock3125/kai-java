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
import industries.vocht.viki.client.SearchClientInterface;
import industries.vocht.viki.client.SubSearchThread;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.infrastructure.ClusterInfrastructure;
import industries.vocht.viki.lexicon.AmbiguousLexicon;
import industries.vocht.viki.lexicon.Lexicon;
import industries.vocht.viki.lexicon.Undesirables;
import industries.vocht.viki.model.cluster.KAIActionType;
import industries.vocht.viki.model.search.*;
import industries.vocht.viki.model.indexes.*;
import industries.vocht.viki.model.super_search.*;
import industries.vocht.viki.relationship.SynonymRelationshipProvider;
import industries.vocht.viki.semantic_search.util.SSearchCollapseResults;
import industries.vocht.viki.semantic_search.util.SSearchFragmentLoader;
import industries.vocht.viki.semantic_search.util.SSearchProcessQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * Created by peter on 25/04/16.
 *
 * super search query system - combine time, entities, keywords, summarisation
 * and anomalies together to scower the indexes
 *
 */
@Component
public class SuperSearch {

    private final Logger logger = LoggerFactory.getLogger(SuperSearch.class);

    @Autowired
    private IDao dao;

    @Autowired
    private IHazelcast hazelcast;

    @Autowired
    private Undesirables undesirables;

    @Autowired
    private AmbiguousLexicon ambiguousLexicon;

    @Autowired
    private Lexicon lexicon;

    @Autowired
    private SSearchExecutor ssExecutor;

    @Autowired
    private ClusterInfrastructure clusterInfrastructure;

    @Autowired
    private SynonymRelationshipProvider synonymRelationshipProvider; // the synonym based (semantic) relationship provider

    // return result window size (left and right of the word)
    @Value("${super.search.result.window.size:25}")
    private int windowSize;

    // score points for a hit of a time-index
    @Value("${search.score.time.index.hit:0.1f}")
    private float timeIndexScoreHit;

    // the maximum number of document fragments to return for a single document
    // -1 = all
    @Value("${search.result.fragment.count:10}")
    private int fragmentCount;


    public SuperSearch() {
    }


    /**
     * perform a search based on a super query
     * @param searchObject the search object for the super query
     * @return the set of indexes matching
     * @throws SSearchParserException any exception
     */
    public SearchResultList doSearch( String sessionID, String email, UUID organisation_id,
                                      SearchObject searchObject, int page, int numItemsPerPage, int maxDistanceAllowed )
            throws IOException, SSearchParserException, ApplicationException, InterruptedException {

        logger.debug("doSearch: setup query");

        if ( searchObject == null || searchObject.getEmail() == null && searchObject.getSearch_text() != null ) {
            throw new SSearchParserException("invalid search-object, must have user's email for security and search text");
        }

        // assume that the user is searching for body content if not specified otherwise
        if ( searchObject.getMetadata() == null ) {
            searchObject.setMetadata(Document.META_BODY);
        }

        // get the access set for security purposes - check the session is valid
        HashSet<Integer> accessSet = hazelcast.getUserAclMap(organisation_id).get(searchObject.getEmail());
        if ( accessSet == null ) {
            throw new ApplicationException("Your session has expired, please sign out and log back in.");
        }
        if ( accessSet.size() == 0 ) { // no acls, never any results
            logger.info("access-set empty for " + email + " - returning empty result set");
            return new SearchResultList();
        }

        // get the selected semantics from the search object for any selections made by the user
        List<UISynsetSet> selectedSemanticsList = searchObject.getSynset_set_list();
        Map<String, Integer> semanticSelectionMap = new HashMap<>();
        if ( selectedSemanticsList != null ) {
            // construct a lookup from the previous selections
            for (UISynsetSet set : selectedSemanticsList) {
                if (set.getWord() != null && set.getSelectedSynsetId() >= 0) {
                    semanticSelectionMap.put(set.getWord(), set.getSelectedSynsetId());
                }
            }
        }
        searchObject.setSynset_set_list(new ArrayList<>()); // clear this set for the coming search

        // determine the query to be executed
        SSearchProcessQuery queryProcessor = new SSearchProcessQuery(synonymRelationshipProvider, undesirables, ambiguousLexicon);
        SSearchProcessQuery.QueryToTermResult result = queryProcessor.queryEncapsulateSearchTerms(searchObject, semanticSelectionMap);

        // re-select any ambiguous objects in the searchObject
        if ( semanticSelectionMap.size() > 0 ) {
            // select these items in the search object's set
            for ( UISynsetSet set : searchObject.getSynset_set_list() ) {
                Integer selection = semanticSelectionMap.get(set.getWord());
                if ( selection != null ) {
                    set.setSelectedSynsetId(selection);
                }
            }
        }

        // setup return set
        SearchResultList obj = new SearchResultList();
        obj.setPage(page);
        obj.setItems_per_page(numItemsPerPage);
        obj.setTotal_document_count(0);
        obj.setOrganisation_id(organisation_id);
        // copy any ambiguous words to the result set
        obj.setSynset_set_list(searchObject.getSynset_set_list());

        // get the combine index set (by url) not yet scored or windowed
        Map<String, DocumentIndexSet> indexSet = readIndexesWithFilterSet( sessionID, email, organisation_id, result.searchItem );

        // filter by window sizes and collapse all items into windows
        logger.debug("doSearch: collapse search results for " + indexSet.size() + " indexes");
        SSearchCollapseResults collapseResults = new SSearchCollapseResults(synonymRelationshipProvider, timeIndexScoreHit);
        Map<String, DocumentIndexSet> combinedSet = collapseResults.collapseAndScoreResultsByWindow( indexSet, result.numKeywords, maxDistanceAllowed );

        // and paginate and highlight
        logger.debug("doSearch: paginate");
        paginate( organisation_id, obj, result.searchRelationshipSet, page, numItemsPerPage, combinedSet, maxDistanceAllowed );

        logger.debug("doSearch: search done, " + obj.getTotal_document_count() + " documents found");
        return obj;
    }


    /**
     * perform a search based on a super query and return only the ranked URLs for the query
     * @param searchObject the search object for the super query
     * @return set of URLs of the matching documents
     * @throws SSearchParserException any exception
     */
    public List<String> getURLList( String sessionID, String email, UUID organisation_id,
                                      SearchObject searchObject, int maxNumItems )
            throws IOException, SSearchParserException, ApplicationException, InterruptedException {

        logger.debug("getURLList: setup query");

        if ( searchObject == null || searchObject.getEmail() == null && searchObject.getSearch_text() != null ) {
            throw new SSearchParserException("getURLList: invalid search-object, must have user's email for security and search text");
        }

        // assume that the user is searching for body content if not specified otherwise
        if ( searchObject.getMetadata() == null ) {
            searchObject.setMetadata(Document.META_BODY);
        }

        // get the access set for security purposes - check the session is valid
        HashSet<Integer> accessSet = hazelcast.getUserAclMap(organisation_id).get(searchObject.getEmail());
        if ( accessSet == null ) {
            throw new ApplicationException("Your session has expired, please sign out and log back in.");
        }
        if ( accessSet.size() == 0 ) { // no acls, never any results
            logger.info("access-set empty for " + email + " - returning empty result set");
            return new ArrayList<>();
        }

        // get the selected semantics from the search object for any selections made by the user
        List<UISynsetSet> selectedSemanticsList = searchObject.getSynset_set_list();
        Map<String, Integer> semanticSelectionMap = new HashMap<>();
        if ( selectedSemanticsList != null ) {
            // construct a lookup from the previous selections
            for (UISynsetSet set : selectedSemanticsList) {
                if (set.getWord() != null && set.getSelectedSynsetId() >= 0) {
                    semanticSelectionMap.put(set.getWord(), set.getSelectedSynsetId());
                }
            }
        }
        searchObject.setSynset_set_list(new ArrayList<>()); // clear this set for the coming search

        // determine the query to be executed
        SSearchProcessQuery queryProcessor = new SSearchProcessQuery(synonymRelationshipProvider, undesirables, ambiguousLexicon);
        SSearchProcessQuery.QueryToTermResult result = queryProcessor.queryEncapsulateSearchTerms(searchObject, semanticSelectionMap);

        // re-select any ambiguous objects in the searchObject
        if ( semanticSelectionMap.size() > 0 ) {
            // select these items in the search object's set
            for ( UISynsetSet set : searchObject.getSynset_set_list() ) {
                Integer selection = semanticSelectionMap.get(set.getWord());
                if ( selection != null ) {
                    set.setSelectedSynsetId(selection);
                }
            }
        }

        // get the combine index set (by url) not yet scored or windowed
        Map<String, DocumentIndexSet> indexSet = readIndexesWithFilterSet( sessionID, email, organisation_id, result.searchItem );
        if ( indexSet != null ) {
            List<String> urlResultList = new ArrayList<>();
            urlResultList.addAll(indexSet.keySet());
            if (urlResultList.size() > maxNumItems) {  // todo: this is arbitrary, returning only the first x items
                Collections.sort(urlResultList);
                return urlResultList.subList(0, maxNumItems);
            }
            return urlResultList;
        }
        return new ArrayList<>();
    }


    /**
     * paginate a set of documents as well as syntax highlight
     * @param obj the result obj to return the items in
     * @param page the page offset
     * @param numItemsPerPage the number of items per page
     * @param documentUrlSet the set of survivors to paginate
     */
    private void paginate(UUID organisation_id, SearchResultList obj,
                          Map<String, Integer> searchRelationshipSet, int page, int numItemsPerPage,
                          Map<String, DocumentIndexSet> documentUrlSet, int maxDistanceAllowed ) throws IOException {

        if ( documentUrlSet != null ) {

            // convert the set to a list sorted by score
            List<DocumentIndexSet> documentList = new ArrayList<>();
            documentList.addAll(documentUrlSet.values());
            Collections.sort(documentList); // sort by score

            // paginate the set according to the user's wishes
            SSearchFragmentLoader fragmentLoader = new SSearchFragmentLoader(dao, lexicon);

            int offset = page * numItemsPerPage;
            int endOffset = offset + numItemsPerPage;
            for (int i = offset; i < endOffset; i++) {

                if (i < documentList.size()) {
                    DocumentIndexSet index = documentList.get(i);
                    SearchResult searchResult = fragmentLoader.processDocumentFragment(organisation_id, index, searchRelationshipSet,
                                                                                       windowSize, maxDistanceAllowed );
                    if ( searchResult != null ) {
                        obj.getSearch_result_list().add(searchResult);
                    }
                }

            } // for the paginated set

            // set total number of document results found
            obj.setTotal_document_count(documentList.size());

        } // document url list from indexes != null
    }


    /**
     * read a set of indexes through parallelism sharding
     * @param sessionID the sessionID
     * @param organisation_id the organisation's id
     * @param searchItem the search item queue
     * @return return the set of indexes grouped by URL
     */
    public Map<String, DocumentIndexSet> readIndexesWithFilterSet( String sessionID, String email, UUID organisation_id, ISSearchItem searchItem )
            throws IOException, SSearchParserException, ApplicationException, InterruptedException {

        Map<String, DocumentIndexSet> indexSet = new HashMap<>();
        if ( searchItem != null ) {
            // list of words
            List<SSearchWord> wordList = new ArrayList<>();
            searchItem.getSearchTerms(wordList);

            // get the number of shards - this is simple - it is the number of shards
            // times each other, each shard multiplies with the previous ones to get a total
            // of possible combinations
            logger.debug("read indexes: hazelcast: get shards");
            int numIterations = 1;
            int[] count = new int[wordList.size()];
            for (int i = 0; i < wordList.size(); i++) {
                count[i] = hazelcast.getShardCount(organisation_id, wordList.get(i).getMetadata(), wordList.get(i).getWord());
                numIterations = numIterations * count[i];
            }

            // easy case - only one shard "set", execute it locally
            logger.debug("read indexes: super search, read indexes from " + numIterations + " shard(s)");
            if (numIterations == 1) {
                // no need to go across cluster - one set of shards only - all shard 0
                Map<String, DocumentIndexSet> documentIndexSet = ssExecutor.doSearch(organisation_id, email, searchItem);
                indexSet = DocumentIndexSet.combine(indexSet, documentIndexSet);
            } else {
                // for each of the shards - collect all the results in parallel
                Map<String, DocumentIndexSet> list = recurseQueries(sessionID, searchItem, 0, wordList, count);
                indexSet = DocumentIndexSet.combine(indexSet, list);
            }
        }
        return indexSet;
    }

    /**
     * recursively probe all possible shard combinations in a keyword search round robin
     * and in parallel against the different search client interfaces
     * @param sessionID the session of the user's security passed to other nodes
     * @param searchItem the search to perform
     * @param counter_index the recursive index into wordList and counter
     * @param wordList the list of words that change shards
     * @param counter the list of counters / shards for each of the words
     * @return a list of all combined indexes
     */
    private Map<String, DocumentIndexSet> recurseQueries( String sessionID, ISSearchItem searchItem,
                                         int counter_index, List<SSearchWord> wordList, int[] counter ) throws IOException, InterruptedException {

        List<SubSearchThread> threadList = new ArrayList<>();
        Map<String, DocumentIndexSet> resultSet = new HashMap<>();
        for ( int i = 0; i < counter[counter_index]; i++ ) {

            wordList.get(counter_index).setShard(i); // setup the shard for the different combinations of shard searching

            // do a search
            SearchClientInterface searchClientInterface = (SearchClientInterface)clusterInfrastructure.getNextClientRoundRobin(KAIActionType.Search);
            threadList.add(searchClientInterface.subSearchParallel(sessionID, searchItem, resultSet));

            // recurse all other possibilities
            if ( i + 1 < counter.length ) {
                Map<String, DocumentIndexSet> indexSet = recurseQueries( sessionID, searchItem, counter_index + 1, wordList, counter );
                if ( indexSet != null ) {
                    for ( String key : indexSet.keySet() ) {
                        DocumentIndexSet existing = resultSet.get(key);
                        if ( existing == null ) {
                            resultSet.put(key, indexSet.get(key));
                        } else {
                            existing.combine(indexSet.get(key));
                        }
                    }
                }
            }
        }

        // wait for threads to finish
        if ( counter_index == 0 ) {
            for ( SubSearchThread thread : threadList ) {
                thread.getThread().join();
            }
        }

        return resultSet;
    }


    /**
     * group indexes by url - phase I
     * @param indexList the list of indexes to combine
     * @return the list of combined indexes - but no scoring applied yet
     */
    private Map<String, List<IIndex>> combineIndexesByUrl( List<IIndex> indexList ) {
        Map<String, List<IIndex>> combinedSetMap = new HashMap<>();
        if ( indexList != null ) {
            for (IIndex index : indexList) {
                List<IIndex> set = combinedSetMap.get(index.getUrl());
                if (set == null) {
                    set = new ArrayList<>();
                    combinedSetMap.put(index.getUrl(), set);
                }
                set.add( index );
            } // for each index
        } // if list valid
        return combinedSetMap;
    }



}


