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
import industries.vocht.viki.dao.IndexDao;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.lexicon.Lexicon;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.indexes.IIndex;
import industries.vocht.viki.model.indexes.Index;
import industries.vocht.viki.model.indexes.TimeIndex;
import industries.vocht.viki.model.indexes.TimeSelectorSetWithBoundaries;
import industries.vocht.viki.model.super_search.*;
import industries.vocht.viki.relationship.RelatedWord;
import industries.vocht.viki.relationship.SynonymRelationshipProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

/**
 * Created by peter on 12/08/16.
 *
 * the tuple version of the ssearch executor - the url of a tuple is the same for all tuples in the same set
 *
 */
public class TupleSSearchExecutor {

    private final Logger logger = LoggerFactory.getLogger(TupleSSearchExecutor.class);

    @Autowired
    private IDao dao;

    @Autowired
    private IHazelcast hazelcast;

    @Autowired
    private SynonymRelationshipProvider synonymRelationshipProvider;

    @Autowired
    private Lexicon lexicon;

    // score points for if the word itself was found
    @Value("${search.score.exact.hit:100.0f}")
    private float fullHit;

    // score points for a hit through a related word (e.g. synonym)
    @Value("${search.score.related.hit:2.5f}")
    private float relatedHit;

    // score points for a hit through a relationship of another word to this word
    @Value("${search.score.related.hit:0.5f}")
    private float inverseRelatedHit;

    // how many years to go into the past/future for before/after searches
    @Value("${super.search.years.window.for.times:10}")
    private int backToTheFuture;

    public TupleSSearchExecutor() {
    }

    /**
     * provide a super search for sub-search interfaces
     * @param organisation_id the organisation's id
     * @param email the email of the user (for acl access)
     * @param searchItem the search item with the shards set
     * @return the list of indexes for the shard combinations
     */
    public List<IIndex> doSearch(UUID organisation_id, String email, ISSearchItem searchItem ) throws ApplicationException, SSearchParserException {
        Map<String, Integer> map = getSearchRelationshipMap(searchItem);
        HashSet<Integer> accessSet = hazelcast.getUserAclMap(organisation_id).get(email);
        if ( accessSet == null ) {
            throw new ApplicationException("invalid session");
        }
        if ( accessSet.size() == 0 ) { // no acls, never any results
            logger.info("access-set empty for " + email + " - returning null");
            return null;
        }
        return doSearch(organisation_id, searchItem, accessSet, map);
    }

    /**
     * construct a map of search term strings
     * @param searchItem the search tree
     * @return the map
     */
    public Map<String, Integer> getSearchRelationshipMap( ISSearchItem searchItem ) {
        List<SSearchWord> wordList = new ArrayList<>();
        searchItem.getSearchTerms(wordList);
        // a general set of all the words present AND their relationships if appropriate
        Map<String, Integer> searchRelationshipSet = new HashMap<>();
        for ( SSearchWord token : wordList ) {
            // get the relationships for this word
            String currentWord = token.getWord().toLowerCase();
            String unStemmed = synonymRelationshipProvider.getStem(currentWord);
            searchRelationshipSet.put(unStemmed, 1); // original search term
            if ( !token.isExact() ) {
                List<RelatedWord> synonymRelationshipList = synonymRelationshipProvider.getRelationships(unStemmed);
                if (synonymRelationshipList != null) {
                    for (RelatedWord relatedWord : synonymRelationshipList) {
                        String relatedStem = synonymRelationshipProvider.getStem(relatedWord.getWord());
                        if (relatedStem.compareToIgnoreCase(unStemmed) != 0 && !searchRelationshipSet.containsKey(relatedStem) ) {
                            searchRelationshipSet.put(relatedStem, 2); // related word
                        }
                    }
                }
            }
        }
        return searchRelationshipSet;
    }

    /**
     * perfom the serach using the objects itself
     * @param searchItem a series of search object
     * @return the matching indexes
     */
    public List<IIndex> doSearch(UUID organisation_id, ISSearchItem searchItem, HashSet<Integer> accessSet,
                                 Map<String, Integer> wordScoreMap ) throws SSearchParserException {

        if ( searchItem != null ) {

            if ( searchItem instanceof SSearchAnd) {
                List<IIndex> set1 = doSearch( organisation_id, ((SSearchAnd)searchItem).getLeft(), accessSet, wordScoreMap );
                List<IIndex> set2 = doSearch( organisation_id, ((SSearchAnd)searchItem).getRight(), accessSet, wordScoreMap );
                return intersection(set1, set2);
            }

            else if ( searchItem instanceof SSearchAndNot) {
                List<IIndex> set1 = doSearch( organisation_id, ((SSearchAndNot)searchItem).getLeft(), accessSet, wordScoreMap );
                List<IIndex> set2 = doSearch( organisation_id, ((SSearchAndNot)searchItem).getRight(), accessSet, wordScoreMap );
                return intersectionNot(set1, set2);
            }

            else if ( searchItem instanceof SSearchOr) {
                List<IIndex> set1 = doSearch( organisation_id, ((SSearchOr)searchItem).getLeft(), accessSet, wordScoreMap );
                List<IIndex> set2 = doSearch( organisation_id, ((SSearchOr)searchItem).getRight(), accessSet, wordScoreMap );
                return union(set1, set2);
            }

            else if ( searchItem instanceof SSearchDateRange) {
                SSearchDateRange dtr = (SSearchDateRange)searchItem;
                return readIndexesForTime(organisation_id, dtr.getOperation(),
                        dtr.getYear1(), dtr.getMonth1(), dtr.getDay1(), dtr.getHour1(), dtr.getMin1(),
                        dtr.getYear2(), dtr.getMonth2(), dtr.getDay2(), dtr.getHour2(), dtr.getMin2(), backToTheFuture);
            }

            else if ( searchItem instanceof SSearchWord ) {
                SSearchWord word = (SSearchWord)searchItem;
                return readIndexesForTerm(organisation_id, word, accessSet, wordScoreMap);
            }

            else {
                throw new SSearchParserException("unknown/unhandled super search type");
            }
        }
        return null;
    }

    /**
     * get the first keyword - and get its meta_c_type
     * @param searchItem a series of search object
     * @return the meta_c_type or 0
     */
    public int getMetaCType(ISSearchItem searchItem) throws SSearchParserException {

        if ( searchItem != null ) {

            if ( searchItem instanceof SSearchAnd) {
                int v1 = getMetaCType( ((SSearchAnd)searchItem).getLeft());
                if ( v1 != 0 ) return v1;
                return getMetaCType( ((SSearchAnd)searchItem).getRight());
            }

            else if ( searchItem instanceof SSearchAndNot) {
                int v1 = getMetaCType( ((SSearchAndNot)searchItem).getLeft());
                if ( v1 != 0 ) return v1;
                return getMetaCType( ((SSearchAndNot)searchItem).getRight());
            }

            else if ( searchItem instanceof SSearchOr) {
                int v1 = getMetaCType( ((SSearchOr)searchItem).getLeft());
                if ( v1 != 0 ) return v1;
                return getMetaCType( ((SSearchOr)searchItem).getRight());
            }

            else if ( searchItem instanceof SSearchDateRange) {
                return 0;
            }

            else if ( searchItem instanceof SSearchWord ) {
                SSearchWord word = (SSearchWord)searchItem;
                return word.getMeta_c_filter();
            }

            else {
                throw new SSearchParserException("unknown/unhandled super search type");
            }
        }
        return 0;
    }

    /**
     * read indexes for a single term and filter as required, filter by security first,
     * and set the index internal scoring
     * @param organisation_id the organisation owner
     * @param searchWord the word to look for from the super search parser query
     * @param accessSet the security acl set
     * @param wordScoreMap known words and their relationship types for the search
     * @return a set off filtered (by meta-data) indexes
     */
    public List<IIndex> readIndexesForTerm(UUID organisation_id, SSearchWord searchWord, HashSet<Integer> accessSet,
                                           Map<String, Integer> wordScoreMap ) {
        IndexDao indexRepository = dao.getIndexDao();
        // assume body search if no metadata found
        if ( searchWord.getMetadata() == null ) {
            searchWord.setMetadata(Document.META_BODY);
        }

        List<Index> indexList = indexRepository.readIndex(organisation_id, searchWord.getWord(), searchWord.getShard(), searchWord.getMetadata());
        Map<String, Index> filteredIndexSet = new HashMap<>();
        String semantic = searchWord.getSemantic();

        // do we have a list if indexes?
        if ( indexList != null ) {
            for (Index index : indexList) { // filter them by the right meta-data tag

                // apply the security filter
                if ( accessSet != null && accessSet.contains(index.getAcl_hash()) ) {

                    if ( index.getWord().compareToIgnoreCase(searchWord.getWord()) == 0 &&
                         (searchWord.getMeta_c_filter() == 0 || (index.getMeta_c_type() & searchWord.getMeta_c_filter()) != 0) &&
                         (!searchWord.isExact() || index.getWord_origin() == null || index.getWord_origin().length() == 0)) {

                        // filter by tag?
                        if ( searchWord.getSynset() == -1 || index.getSynset() == -1 || searchWord.getSynset() == index.getSynset() ) {

                            if (searchWord.getTag() != null && semantic != null) {

                                if (tagMatch(searchWord.getTag(), index.getTag()) && semanticMatch(searchWord.getWord(), semantic)) {
                                    scoreIndex(index, wordScoreMap);
                                    filteredIndexSet.put(index.getUrl(), index);
                                }

                            } else if (searchWord.getTag() != null) {

                                if (tagMatch(searchWord.getTag(), index.getTag())) {
                                    scoreIndex(index, wordScoreMap);
                                    filteredIndexSet.put(index.getUrl(), index);
                                }

                            } else {
                                scoreIndex(index, wordScoreMap);
                                filteredIndexSet.put(index.getUrl(), index);
                            }

                        } // synset filter

                    }

                }

            }
        }

        List<IIndex> newIndexList = new ArrayList<>();
        newIndexList.addAll(filteredIndexSet.values());
        return newIndexList;
    }

    /**
     * score an index in the set, add to existing scores depending on the type
     * of "hit" on the specified index
     * @param index the index to score
     * @param wordScoreMap map of the words used in the query and their applicable related words
     */
    private void scoreIndex( Index index, Map<String, Integer> wordScoreMap ) {
        // score this index
        if ( wordScoreMap != null ) {
            float score;
            if (index.getWord_origin() == null || index.getWord_origin().length() == 0) {
                score = fullHit;
            } else if ( wordScoreMap.containsKey(index.getWord()) ) {
                score = relatedHit;
            } else {
                score = inverseRelatedHit;
            }
            index.score = index.score + score;
        }
    }


    /**
     * return true if the two tags are compatible
     * @param tag the tag from the search query
     * @param indexTag the tag from the index
     * @return true if the tag is filter ok
     */
    private boolean tagMatch( String tag, String indexTag ) {
        if ( tag != null && indexTag != null ) {
            switch ( tag ) {
                case "noun": {
                    return indexTag.toUpperCase().startsWith("NN");
                }
                case "proper noun": {
                    return indexTag.toUpperCase().startsWith("NNP");
                }
                case "adjective": {
                    return indexTag.toUpperCase().startsWith("JJ");
                }
                case "verb": {
                    return indexTag.toUpperCase().startsWith("VB");
                }
                default: {
                    return indexTag.compareToIgnoreCase(tag) == 0;
                }
            }
        }
        return false;
    }

    /**
     * use the lexicon to semantically match the word and its semantic
     * if this is a known entity - it can be matched against the lexicon's known semantics
     * @param word the word to match
     * @param semantic the semantic of the word
     * @return true if the word has this semantic anywhere in the lexicon
     */
    private boolean semanticMatch( String word, String semantic ) {
        if ( word != null && semantic != null ) {
            List<Token> lexiconList = lexicon.getByName(word);
            if ( lexiconList != null ) {
                for ( Token token : lexiconList ) {
                    if ( token.getSemantic() != null ) {
                        if ( semantic.equals("location" ) && lexicon.isLocationSemantic(token.getSemantic()) ) {
                            return true;
                        } else if ( semantic.equals("person") && lexicon.isPersonSemantic(token.getSemantic()) ) {
                            return true;
                        } else if ( semantic.compareToIgnoreCase(token.getSemantic()) == 0 ) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * return a set of indexes for the specified date/range time types
     * @param organisation_id the organisation that owns the indexes
     * @param operation the type of time search, before/after/between/exact
     * @param year1 the start year
     * @param month1 the start month (can be -1)
     * @param day1 the start day (can be -1)
     * @param hour1 the start hour (can be -1)
     * @param min1 the start minute (can be -1, assumed -1 if hour == -1 )
     * @param year2 the end year for between and exact operations
     * @param month2 the end month for between and exact operations (can be -1)
     * @param day2  the end day for between and exact operations (can be -1)
     * @param hour2  the end hour for between and exact operations (can be -1)
     * @param min2 the end minute (can be -1, assumed -1 if hour == -1 )
     * @param yearsOffset for before/after operations, how many years to go into the past / future with data gathering
     * @return a set of indexes matching the times, or null
     */
    public List<IIndex> readIndexesForTime( UUID organisation_id, SSearchDateRangeType operation,
                                            int year1, int month1, int day1, int hour1, int min1,
                                            int year2, int month2, int day2, int hour2, int min2,
                                            int yearsOffset ) throws SSearchParserException {

        TimeSelectorSetWithBoundaries set;
        if ( operation == null || operation == SSearchDateRangeType.Exact || operation == SSearchDateRangeType.Between ) {
            set = dao.getIndexDao().getTimeSelectorsForRange(year1, month1, day1, hour1, min1,
                    year2, month2, day2, hour2, min2);
        } else if ( operation == SSearchDateRangeType.Before ) {
            set = dao.getIndexDao().getTimeSelectorsBefore(year1, month1, day1, hour1, min1, yearsOffset);
        } else if ( operation == SSearchDateRangeType.After ) {
            set = dao.getIndexDao().getTimeSelectorsAfter(year1, month1, day1, hour1, min1, yearsOffset);
        } else {
            throw new SSearchParserException("unknown date-range operation type " + operation );
        }
        List<TimeIndex> timeIndexList = dao.getIndexDao().getIndexListForRange(organisation_id, set);
        if ( timeIndexList != null ) {
            List<IIndex> indexList = new ArrayList<>();
            indexList.addAll(timeIndexList);
            return indexList;
        }
        return null;
    }

    /**
     * put two sets of indexes together into a single one
     * @param set1 first set
     * @param set2 second set
     * @return the union of the two sets
     */
    private List<IIndex> union( List<IIndex> set1, List<IIndex> set2 ) {
        List<IIndex> resultSet = new ArrayList<>();
        Map<String, IIndex> indexResultMap = new HashMap<>();
        if ( set1 != null ) {
            for ( IIndex index : set1 ) {
                indexResultMap.put( index.getUrl(), index );
            }
        }
        if ( set2 != null ) {
            for ( IIndex index : set2 ) {
                indexResultMap.put( index.getUrl(), index );
            }
        }
        resultSet.addAll(indexResultMap.values());
        return resultSet;
    }

    /**
     * intersect two sets of indexes together into a single one
     * @param set1 first set
     * @param set2 second set
     * @return the intersection of the two sets (at url level)
     */
    private List<IIndex> intersection( List<IIndex> set1, List<IIndex> set2 ) {
        List<IIndex> resultSet = new ArrayList<>();

        // either empty?
        if ( set1 == null || set2 == null || set1.size() == 0 || set2.size() == 0 ) {
            return resultSet;
        }

        // first set
        Map<String, IIndex> indexHashMap = new HashMap<>();
        for ( IIndex index : set1 ) {
            indexHashMap.put( index.getUrl(), index );
        }

        // intersection of the second set
        Map<String, IIndex> indexResultMap = new HashMap<>();
        for ( IIndex index2 : set2 ) {

            IIndex index1 = indexHashMap.get(index2.getUrl());
            if ( index1 != null ) {
                indexResultMap.put(index1.getUrl(), index1);
            }
        }

        resultSet.addAll(indexResultMap.values());
        return resultSet;
    }


    /**
     * intersect two sets of indexes together into a single one with a logical NOT
     * @param set1 first set
     * @param set2 second set
     * @return set1 and NOT set2
     */
    private List<IIndex> intersectionNot( List<IIndex> set1, List<IIndex> set2 ) {

        List<IIndex> resultSet = new ArrayList<>();

        // set 2 empty?
        if ( set2 == null || set2.size() == 0 ) {
            resultSet.addAll(set1);
            return resultSet;
        }

        // first set - gather all existing data
        HashSet<String> indexSet = new HashSet<>();
        for ( IIndex index : set2 ) {
            indexSet.add(index.getUrl());
        }

        // intersection of the second set
        Map<String, IIndex> indexResultMap = new HashMap<>();
        for ( IIndex index : set1 ) {
            // if the first item isn't in the second set - we're fine
            if ( !indexSet.contains(index.getUrl()) ) {
                indexResultMap.put( index.getUrl(), index );
            }
        }

        resultSet.addAll(indexResultMap.values());
        return resultSet;
    }


}

