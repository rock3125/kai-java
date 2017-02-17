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

package industries.vocht.viki.semantic_search.util;

import industries.vocht.viki.lexicon.AmbiguousLexicon;
import industries.vocht.viki.lexicon.LexiconSynset;
import industries.vocht.viki.lexicon.Undesirables;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.search.SearchObject;
import industries.vocht.viki.model.search.UISynsetSet;
import industries.vocht.viki.model.super_search.ISSearchItem;
import industries.vocht.viki.model.super_search.SSearchParser;
import industries.vocht.viki.model.super_search.SSearchParserException;
import industries.vocht.viki.model.super_search.SSearchWord;
import industries.vocht.viki.relationship.RelatedWord;
import industries.vocht.viki.relationship.SynonymRelationshipProvider;
import industries.vocht.viki.semantic_search.SuperSearch;
import industries.vocht.viki.tokenizer.Tokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by peter on 4/06/16.
 *
 * process the query string and exploit its relationships
 * and create maps for the purpose
 *
 */
public class SSearchProcessQuery {

    private final Logger logger = LoggerFactory.getLogger(SuperSearch.class);

    private SynonymRelationshipProvider synonymRelationshipProvider;
    private AmbiguousLexicon ambiguousLexicon;
    private Undesirables undesirables;

    public SSearchProcessQuery(SynonymRelationshipProvider synonymRelationshipProvider, Undesirables undesirables,
                               AmbiguousLexicon ambiguousLexicon) {
        this.synonymRelationshipProvider = synonymRelationshipProvider;
        this.ambiguousLexicon = ambiguousLexicon;
        this.undesirables = undesirables;
    }

    // parameter return class for this system
    public static class QueryToTermResult {
        public ISSearchItem searchItem; // root of the super query
        public int numKeywords;
        public Map<String, Integer> searchRelationshipSet; // all words considered to be valid search items
        public Map<String, Integer> stringToKeywordIndex; // each search terms contributions to the search
        public QueryToTermResult( ISSearchItem searchItem, Map<String, Integer> searchRelationshipSet, int numKeywords, Map<String, Integer> stringToKeywordIndex ) {
            this.searchItem = searchItem;
            this.numKeywords = numKeywords;
            this.searchRelationshipSet = searchRelationshipSet;
            this.stringToKeywordIndex = stringToKeywordIndex;
        }
    }

    /**
     * convert a string to a super query - even ordinary strings can be tokenized
     * if a string starts with ( it is assumed to be a super query
     * setup the synset items too in searchObject
     * @param searchObject for setting up any ambiguous words in the system and the search query
     * @return a search item token or null
     */
    public QueryToTermResult queryEncapsulateSearchTerms(SearchObject searchObject, Map<String, Integer> synsetSelectionMap) throws SSearchParserException {

        if ( searchObject != null && searchObject.getSearch_text() != null ) {
            // a general set of all the words present AND their relationships if appropriate
            Map<String, Integer> searchRelationshipSet = new HashMap<>();
            // a list of words that must be present for the search to be considered valid
            Map<String, Integer> stringToKeywordIndex = new HashMap<>();

            String queryStr = searchObject.getSearch_text().trim();
            if ( queryStr.startsWith("(") ) {
                logger.debug("super search: " + queryStr);
                ISSearchItem searchItem = new SSearchParser().parse(queryStr);

                // collect the words used
                List<SSearchWord> wordList = new ArrayList<>();
                searchItem.getSearchTerms(wordList);

                int keywordIndex = 0;
                for ( SSearchWord token : wordList ) {
                    // set syn for this word?
                    if ( synsetSelectionMap != null ) {
                        Integer synset = synsetSelectionMap.get(token.getWord());
                        if ( synset != null ) {
                            token.setSynset(synset);
                        }
                    }
                    String unStemmed = synonymRelationshipProvider.getStem(token.getWord().toLowerCase());
                    if ( !undesirables.isUndesirable(unStemmed)) {

                        HashSet<String> orderedSearchWord = new HashSet<>();

                        if ( token.getSemantic() == null || token.getSemantic().length() == 0 ) {
                            checkAmbiguity(unStemmed, searchObject, ambiguousLexicon); // check ambiguous words
                        }

                        // get the relationships for this word
                        orderedSearchWord.add( unStemmed );
                        searchRelationshipSet.put(unStemmed, 1); // original search term
                        if ( !token.isExact() ) {
                            List<RelatedWord> synonymRelationshipList = synonymRelationshipProvider.getRelationships(unStemmed);
                            if (synonymRelationshipList != null) {
                                for (RelatedWord relatedWord : synonymRelationshipList) {
                                    String relatedStem = synonymRelationshipProvider.getStem(relatedWord.getWord());
                                    orderedSearchWord.add( relatedStem );
                                    if (relatedStem.compareToIgnoreCase(unStemmed) != 0 && !searchRelationshipSet.containsKey(relatedStem) ) {
                                        searchRelationshipSet.put(relatedStem, 2); // related word
                                    }
                                }
                            }
                        }

                        for ( String keyword : orderedSearchWord ) {
                            stringToKeywordIndex.put( keyword, keywordIndex );
                        }

                    } // if ! undesirable

                    keywordIndex = keywordIndex + 1; // next keyword

                } // for each token

                return new QueryToTermResult(searchItem, searchRelationshipSet, keywordIndex, stringToKeywordIndex);

            } else {
                // tokenize and provide a body type search for now
                Tokenizer tokenizer = new Tokenizer();
                List<Token> tokenList = tokenizer.filterOutSpaces(tokenizer.tokenize(queryStr));
                if ( tokenList != null ) {
                    List<Token> filteredList = new ArrayList<>();
                    int keywordIndex = 0;
                    for ( Token token : tokenList ) {

                        String unStemmed = synonymRelationshipProvider.getStem(token.getText().toLowerCase());
                        if (!undesirables.isUndesirable(unStemmed)) {
                            checkAmbiguity(unStemmed, searchObject, ambiguousLexicon); // check ambiguous words

                            HashSet<String> orderedSearchWord = new HashSet<>();

                            // get the relationships for this word
                            orderedSearchWord.add( unStemmed );
                            searchRelationshipSet.put(unStemmed, 1); // original search term
                            List<RelatedWord> synonymRelationshipList = synonymRelationshipProvider.getRelationships(unStemmed);
                            if ( synonymRelationshipList != null ) {
                                for (RelatedWord relatedWord : synonymRelationshipList) {
                                    String relatedStem = synonymRelationshipProvider.getStem(relatedWord.getWord());
                                    if ( relatedStem.compareToIgnoreCase(unStemmed) != 0 && !searchRelationshipSet.containsKey(relatedStem)) {
                                        searchRelationshipSet.put( relatedStem, 2 ); // related word
                                        orderedSearchWord.add( relatedStem );
                                    }
                                }
                            }

                            // only process unStemmed items
                            token.setText(unStemmed);
                            filteredList.add(token);

                            for ( String keyword : orderedSearchWord ) {
                                stringToKeywordIndex.put( keyword, keywordIndex );
                            }

                        } // if ! undesirable

                        keywordIndex = keywordIndex + 1;

                    } // for each token

                    StringBuilder sb = new StringBuilder();
                    for ( int i = 0; i < filteredList.size(); i++ ) {
                        sb.append("body(").append(filteredList.get(i).getText()).append(")");
                        if (i + 1 < filteredList.size()) {
                            sb.append(" and ");
                        }
                    }
                    logger.debug("constructed super search: " + sb.toString());
                    ISSearchItem searchItem = new SSearchParser().parse(sb.toString());
                    if ( synsetSelectionMap != null && synsetSelectionMap.size() > 0 ) {
                        List<SSearchWord> wordList = new ArrayList<>();
                        searchItem.getSearchTerms(wordList);
                        for (SSearchWord token : wordList) {
                            // set syn for this word?
                            Integer synset = synsetSelectionMap.get(token.getWord());
                            if (synset != null) {
                                token.setSynset(synset);
                            }
                        }
                    }
                    return new QueryToTermResult(searchItem, searchRelationshipSet, keywordIndex, stringToKeywordIndex);
                }
            }
        }
        return null;
    }


    /**
     * encapsulate a super search query into a set of relationships and their order
     * @param searchObject the object to return data into
     * @param query the super search query
     * @param synsetSelectionMap the synset selection map for specific synset selecting
     * @return a query-term object
     * @throws SSearchParserException anything goes wrong
     */
    public QueryToTermResult queryEncapsulateSearchTerms(SearchObject searchObject, ISSearchItem query,
                                                         Map<String, Integer> synsetSelectionMap) throws SSearchParserException {

        if ( searchObject != null && query != null ) {
            // a general set of all the words present AND their relationships if appropriate
            HashMap<String, Integer> searchRelationshipSet = new HashMap<>();
            // a list of words that must be present for the search to be considered valid
            Map<String, Integer> stringToKeywordIndex = new HashMap<>();

            // collect the words used
            List<SSearchWord> wordList = new ArrayList<>();
            query.getSearchTerms(wordList);

            int keywordIndex = 0;
            for ( SSearchWord token : wordList ) {
                // set syn for this word?
                if ( synsetSelectionMap != null ) {
                    Integer synset = synsetSelectionMap.get(token.getWord());
                    if ( synset != null ) {
                        token.setSynset(synset);
                    }
                }
                String unStemmed = synonymRelationshipProvider.getStem(token.getWord().toLowerCase());
                if ( !undesirables.isUndesirable(unStemmed)) {

                    HashSet<String> orderedSearchWord = new HashSet<>();

                    if ( token.getSemantic() == null || token.getSemantic().length() == 0 ) {
                        checkAmbiguity(unStemmed, searchObject, ambiguousLexicon); // check ambiguous words
                    }

                    // get the relationships for this word
                    orderedSearchWord.add( unStemmed );
                    searchRelationshipSet.put(unStemmed, 1); // original search term
                    if ( !token.isExact() ) {
                        List<RelatedWord> synonymRelationshipList = synonymRelationshipProvider.getRelationships(unStemmed);
                        if (synonymRelationshipList != null) {
                            for (RelatedWord relatedWord : synonymRelationshipList) {
                                String relatedStem = synonymRelationshipProvider.getStem(relatedWord.getWord());
                                orderedSearchWord.add( relatedStem );
                                if (relatedStem.compareToIgnoreCase(unStemmed) != 0 && !searchRelationshipSet.containsKey(relatedStem) ) {
                                    searchRelationshipSet.put(relatedStem, 2); // related word
                                }
                            }
                        }
                    }

                    for ( String keyword : orderedSearchWord ) {
                        stringToKeywordIndex.put( keyword, keywordIndex );
                    }

                } // if ! undesirable

                keywordIndex = keywordIndex + 1;

            } // for each word

            return new QueryToTermResult(query, searchRelationshipSet, keywordIndex, stringToKeywordIndex);

        }
        return null;
    }


    /**
     * create any synsets present for the word into the search object for transport back to the UI
     * @param word the word in question
     * @param searchObject the user's search object
     * @param ambiguousLexicon the store of ambiguous nouns / synsets
     */
    private void checkAmbiguity( String word, SearchObject searchObject, AmbiguousLexicon ambiguousLexicon ) {
        if ( word != null && searchObject != null && ambiguousLexicon != null ) {
            List<LexiconSynset> synsetList = ambiguousLexicon.getSynset(word);
            if ( synsetList != null && synsetList.size() > 1 ) {
                searchObject.getSynset_set_list().add( new UISynsetSet(word, LexiconSynset.convert(synsetList) ) );
            }
        }
    }



}
