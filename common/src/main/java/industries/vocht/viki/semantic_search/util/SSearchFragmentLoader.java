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

import industries.vocht.viki.IDao;
import industries.vocht.viki.dao.PennType;
import industries.vocht.viki.datastructures.IntList;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.lexicon.Lexicon;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.indexes.DocumentIndex;
import industries.vocht.viki.model.indexes.DocumentIndexSet;
import industries.vocht.viki.model.search.SearchResult;
import industries.vocht.viki.tokenizer.Tokenizer;
import industries.vocht.viki.utility.SentenceFromBinary;
import org.joda.time.DateTime;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by peter on 4/06/16.
 *
 * load a super search fragment and add all
 * metadata to it, as well as performing a detailed analysis of the entities in the document
 *
 */
public class SSearchFragmentLoader {

    private IDao dao;
    private Lexicon lexicon;

    public SSearchFragmentLoader(IDao dao, Lexicon lexicon) {
        this.dao = dao;
        this.lexicon = lexicon;
    }

    /**
     * turn a document fragment into a full search result for return to the UI
     * @param organisation_id the organisation
     * @param item the document index set
     * @param searchRelationshipSet the related words of our search term
     * @return a new document fragment that is a direct search result
     * @throws IOException error
     */
    public SearchResult processDocumentFragment(UUID organisation_id, DocumentIndexSet item,
                                                Map<String, Integer> searchRelationshipSet,
                                                int windowSize, int maxDistanceAllowed ) throws IOException {

        if ( organisation_id != null && item != null && item.getUrl() != null ) {

            SSearchHighlighter highlighter = new SSearchHighlighter(lexicon);

            // first read the parsed document
            String url = item.getUrl();
            Map<String, byte[]> documentData = dao.getDocumentDao().getDocumentParseTreeMap(organisation_id, url);
            byte[] data = documentData.get(Document.META_BODY);
            if (data != null) {

                // convert these trees into a list of tokens corresponding to the offsets
                SentenceFromBinary converter = new SentenceFromBinary();
                List<Token> tokenList = converter.convertToTokenList(data);

                // does it have a title?
                String title = null;
                byte[] titleData = documentData.get(Document.META_TITLE);
                if (titleData != null) {
                    List<Token> titleList = converter.convertToTokenList(titleData);
                    title = new Tokenizer().toString(titleList);
                }

                // does it have an author?
                String author = null;
                byte[] authorData = documentData.get(Document.META_AUTHOR);
                if (authorData != null) {
                    List<Token> authorList = converter.convertToTokenList(authorData);
                    author = new Tokenizer().toString(authorList);
                }

                // does it have a created date/time?
                String created = null;
                byte[] createdData = documentData.get(Document.META_CREATED_DATE_TIME);
                if (createdData != null) {
                    List<Token> createdList = converter.convertToTokenList(createdData);
                    created = new Tokenizer().toString(createdList);
                }

                // group offsets into sets of highlights within windowSize
                List<IntList> highlightSet = getHighlightSets( item, windowSize );
                Collections.sort(highlightSet); // sort by largest first
                List<String> highlightStrings = new ArrayList<>();

                for ( IntList set : highlightSet ) {
                    highlightStrings.add(highlighter.getHighlightStringForItem(set, tokenList, searchRelationshipSet, windowSize));
                }

                SearchResult searchResult = new SearchResult(url, highlightStrings, item.getTotal_score());
                searchResult.setAuthor( author );
                if ( title != null && !title.equals(url) ) {
                    searchResult.setTitle(title);
                }
                searchResult.setCreated_date( created );

                analyse(tokenList, getCombinedOffsets(item), searchResult, maxDistanceAllowed);

                return searchResult;
            }
        }
        return null;
    }

    /**
     * group items into bunches around a window size
     * @param item the item with its offsets
     * @param windowSize the size of the windows
     * @return a set of grouped lists of offsets within window size
     */
    private List<IntList> getHighlightSets(DocumentIndexSet item, int windowSize ) {
        List<DocumentIndex> allOffsets = item.getDocumentIndexList();
        Collections.sort(allOffsets);

        List<IntList> result = new ArrayList<>();
        IntList currentList = new IntList();
        int startOffset = allOffsets.get(0).offset;
        for ( DocumentIndex index : allOffsets ) {
            int diff = Math.abs(index.offset - startOffset);
            if ( diff < windowSize * 2 ) {
                currentList.add(index.offset);
            } else {
                result.add( currentList );
                currentList = new IntList();
                currentList.add(index.offset);
                startOffset = index.offset;
            }
        }
        if ( currentList.size() > 0 ) {
            result.add(currentList);
        }
        return result;
    }

    /**
     * get a list of all combined offsets for the indexes, sorted ascending
     * @param item the main index
     * @return a list of index offsets
     */
    private IntList getCombinedOffsets( DocumentIndexSet item ) {
        IntList list = new IntList(item.getDocumentIndexList().size());
        for ( DocumentIndex index : item.getDocumentIndexList() ) {
            list.add(index.offset);
        }
        list.sort();
        return list;
    }

    /**
     * perform an entity analysis on the body of this document
     * @param tokenList the body tokens
     * @param offsetList the document offsets
     * @param result the result carrier
     * @param maxDistanceAllowed item must be within this distance of an index to be included (if > 0)
     * @throws IOException
     */
    private void analyse( List<Token> tokenList, IntList offsetList, SearchResult result, int maxDistanceAllowed ) throws IOException {

        Map<String, Integer> person_set = new HashMap<>();
        Map<String, Integer> location_set = new HashMap<>();
        Map<String, Integer> time_set = new HashMap<>();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // analyse body
        if ( tokenList != null ) {
            int size = tokenList.size();
            for ( int i = 0; i < size; i++ ) {
                Token token = tokenList.get(i);
                if ( inRange(i, offsetList, maxDistanceAllowed ) ) {

                    // proper noun? and person / location
                    if (token.getPennType() == PennType.NNP) {
                        if (token.getSemantic() != null) {
                            if (lexicon.isPersonSemantic(token.getSemantic())) {
                                addToSet(token.getText(), person_set);
                            } else if (lexicon.isLocationSemantic(token.getSemantic())) {
                                addToSet(token.getText(), location_set);
                            }
                        }
                    }

                    // recognized time entities
                    if (token.getGrammarRuleName() != null) {
                        if (token.getValue() > 0L &&
                                (token.getGrammarRuleName().startsWith("time") || token.getGrammarRuleName().startsWith("date"))) {
                            addToSet(formatter.format(new DateTime(token.getValue()).toDate()), time_set);
                        }
                    }

                } // in range

            } // for each token
        }

        result.setLocation_set(location_set);
        result.setPerson_set(person_set);
        result.setTime_set(time_set);
    }

    /**
     * return true if an index is within range of an offset
     * @param index the index offset to check
     * @param offsetList the list of index offsets
     * @param maxDistanceAllowed maximum distance allowed to be valid
     * @return true if in range
     */
    private boolean inRange( int index, IntList offsetList, int maxDistanceAllowed ) {
        if ( maxDistanceAllowed <= 0 ) {
            return true;
        } else {
            int prevDelta = -1;
            for ( int value : offsetList ) {
                int delta = Math.abs(value - index);
                if ( delta < maxDistanceAllowed ) {
                    return true;
                }
                if ( prevDelta == -1 ) {
                    prevDelta = delta;
                } else if ( prevDelta > delta ) {
                    break; // finished
                }
            }
        }
        return false;
    }

    /**
     * add a count item to the map
     * @param text the text to add / increment
     * @param map the container map
     */
    private void addToSet( String text, Map<String, Integer> map ) {
        if ( map != null && text != null ) {
            Integer value = map.get(text);
            if ( value == null ) {
                map.put(text, 1);
            } else {
                map.put(text, value + 1);
            }
        }
    }


}
