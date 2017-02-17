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

import industries.vocht.viki.IDao;
import industries.vocht.viki.dao.PennType;
import industries.vocht.viki.datastructures.IntList;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.lexicon.Lexicon;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.search.SearchObject;
import industries.vocht.viki.model.search.SearchResult;
import industries.vocht.viki.model.search.SearchResultList;
import industries.vocht.viki.semantic_search.util.SSearchHighlighter;
import industries.vocht.viki.tokenizer.Tokenizer;
import industries.vocht.viki.utility.SentenceFromBinary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by peter on 5/06/16.
 *
 * view entities by urls
 *
 */
@Component
public class ViewSearchEntities {

    @Autowired
    private Lexicon lexicon;

    @Autowired
    private IDao dao;

    // return result window size (left and right of the word)
    @Value("${super.search.result.window.size:25}")
    private int windowSize;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    public ViewSearchEntities() {
    }

    /**
     * processs the search result entries for the given search object - take the given url_list
     * and the search_text to identify all entities in the urls that match this item
     * @param organisation_id the organisation id
     * @param searchObject the search object to use
     * @return a new result object with related items
     */
    public SearchResultList getSearchEntities(UUID organisation_id, SearchObject searchObject) throws IOException {

        SSearchHighlighter highlighter = new SSearchHighlighter(lexicon);
        SearchResultList resultList = new SearchResultList();

        if ( organisation_id != null && searchObject != null && searchObject.getUrl_list() != null ) {

            // is the search text an ISO date?
            long dateValue = 0L;
            String textStr = searchObject.getSearch_text();
            if ( textStr != null ) {
                try {
                    Date date = dateFormat.parse(textStr);
                    if (date != null ) {
                        dateValue = date.getTime();
                    }
                } catch (Exception ex) {
                }
            }

            Map<String, Integer> searchRelationshipSet = new HashMap<>();
            searchRelationshipSet.put( searchObject.getSearch_text(), 1 );

            for ( String url : searchObject.getUrl_list() ) {

                List<Token> tokenList = getBodyText(organisation_id, url);
                IntList offsetList = new IntList();
                if ( tokenList != null ) {
                    for ( int i = 0; i < tokenList.size(); i++ ) {
                        Token token = tokenList.get(i);
                        if ( (token.getPennType() == PennType.NNP || token.getPennType() == PennType.NNPS) && token.getSemantic() != null ) {
                            if ( token.getText().compareToIgnoreCase(searchObject.getSearch_text()) == 0 ) {
                                offsetList.add(i);
                            }
                        } else if ( token.getValue() > 0 && token.getValue() == dateValue ) {
                            offsetList.add(i);
                        }
                    }
                }

                // process the offsets
                if ( offsetList.size() > 0 ) {
                    SearchResult searchResult = new SearchResult();
                    searchResult.setUrl(url);
                    List<IntList> highlightList = getHighlightSets( offsetList, windowSize );
                    Collections.sort(highlightList);
                    for ( IntList list : highlightList ) {
                        String highlightStr = highlighter.getHighlightStringForItem(list, tokenList, searchRelationshipSet, windowSize);
                        if ( highlightStr != null ) {
                            searchResult.getText_list().add(highlightStr);
                        }
                    }
                    resultList.getSearch_result_list().add( searchResult );
                }

            } // for each url
        }
        return resultList;
    }

    /**
     * group items into bunches around a window size
     * @param offsetList the total offset for a document
     * @param windowSize the size of the windows
     * @return a set of grouped lists of offsets within window size
     */
    private List<IntList> getHighlightSets(IntList offsetList, int windowSize ) {

        offsetList.sort();
        List<IntList> result = new ArrayList<>();
        IntList currentList = new IntList();
        int startOffset = offsetList.get(0);
        for ( int offset : offsetList ) {
            int diff = Math.abs(offset - startOffset);
            if ( diff < windowSize * 2 ) {
                currentList.add(offset);
            } else {
                result.add( currentList );
                currentList = new IntList();
                currentList.add(offset);
                startOffset = offset;
            }
        }
        if ( currentList.size() > 0 ) {
            result.add(currentList);
        }
        return result;
    }

    /**
     * get the body text / tokens
     * @param organisation_id the organisation id of the document
     * @param url the url of the document
     * @return null if dne, otherwise a list of tokens that is the document body
     * @throws IOException
     */
    private List<Token> getBodyText(UUID organisation_id, String url) throws IOException {
        // first read the parsed document
        Map<String, byte[]> documentData = dao.getDocumentDao().getDocumentParseTreeMap(organisation_id, url);
        byte[] data = documentData.get(Document.META_BODY);
        if (data != null) {

            // convert these trees into a list of tokens corresponding to the offsets
            SentenceFromBinary converter = new SentenceFromBinary();
            return converter.convertToTokenList(data);
        }
        return null;
    }



}

