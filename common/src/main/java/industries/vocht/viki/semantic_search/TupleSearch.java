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
import industries.vocht.viki.lexicon.AmbiguousLexicon;
import industries.vocht.viki.lexicon.Lexicon;
import industries.vocht.viki.lexicon.Undesirables;
import industries.vocht.viki.model.indexes.DocumentIndex;
import industries.vocht.viki.model.indexes.IIndex;
import industries.vocht.viki.model.indexes.Index;
import industries.vocht.viki.model.indexes.DocumentIndexSet;
import industries.vocht.viki.model.search.SearchObject;
import industries.vocht.viki.model.semantics.*;
import industries.vocht.viki.model.super_search.SSearchParserException;
import industries.vocht.viki.model.super_search.SSearchWord;
import industries.vocht.viki.parser.TupleQueryParser;
import industries.vocht.viki.relationship.RelatedWord;
import industries.vocht.viki.relationship.SynonymRelationshipProvider;
import industries.vocht.viki.semantic_search.util.SSearchCollapseResults;
import industries.vocht.viki.semantic_search.util.SSearchHighlighter;
import industries.vocht.viki.semantic_search.util.SSearchProcessQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * Created by peter on 20/07/16.
 *
 * tuple search engine
 *
 */
@Component
public class TupleSearch {

    private final Logger logger = LoggerFactory.getLogger(SSearchExecutor.class);

    @Autowired
    private IDao dao;

    @Autowired
    private IHazelcast hazelcast;

    @Autowired
    private Lexicon lexicon;

    @Autowired
    private SynonymRelationshipProvider synonymRelationshipProvider;

    @Autowired
    private TupleQueryParser tupleQueryParser;

    @Autowired
    private Undesirables undesirables;

    @Autowired
    private AmbiguousLexicon ambiguousLexicon;

    @Autowired
    private TupleSSearchExecutor tssExecutor;

    // score points for a hit of a time-index
    @Value("${search.score.time.index.hit:0.1f}")
    private float timeIndexScoreHit;

    /**
     * provide a super search for the tuple-search interface
     * each tuple is a document in the indexes with its url being the GUID id of the tuple (see tuple table)
     * @param organisation_id the organisation's id
     * @param accessSet for acl access
     * @param nlQuery the natural language tuple query (just english)
     * @param page the page offset
     * @param pageSize the size of each page
     * @return the list of indexes
     * @throws ApplicationException wrong
     * @throws SSearchParserException wrong
     */
    public TupleResultList tupleSearch(UUID organisation_id, HashSet<Integer> accessSet, String nlQuery, int page, int pageSize )
            throws ApplicationException, SSearchParserException, IOException, InterruptedException {

        TupleQuery query = tupleQueryParser.parseQuery(nlQuery);
        if ( query == null || query.getSearchItem() == null ) {
            throw new SSearchParserException("query cannot be processed \"" + nlQuery + "\"");
        }

        // get the combine index set (by url) not yet scored or windowed
        Map<String, Integer> searchMap = new HashMap<>();
        List<IIndex> indexList = tssExecutor.doSearch( organisation_id, query.getSearchItem(), accessSet, searchMap );
        if ( indexList != null && indexList.size() > 0 ) {

            int meta_c_type = tssExecutor.getMetaCType(query.getSearchItem());

            // get the selected semantics from the search object for any selections made by the user
            Map<String, Integer> semanticSelectionMap = new HashMap<>();
            SearchObject searchObject = new SearchObject();
            searchObject.setSynset_set_list(new ArrayList<>()); // clear this set for the coming search

            // convert the set to a list sorted by score
            List<Index> documentList = new ArrayList<>();
            for (IIndex index : indexList) {
                if (index instanceof Index) {
                    documentList.add((Index)index);
                }
            }
            Collections.sort(documentList); // sort by score

            // remove any duplicate urls from the document list
            List<Index> finalDocumentList = new ArrayList<>();
            HashSet<String> urlDecector = new HashSet<>();
            for (Index item : documentList) {
                String url = item.getUrl();
                if (!urlDecector.contains(url)) {
                    urlDecector.add(url);
                    finalDocumentList.add(item);
                }
            }

            // sort them and paginate them
            List<UUID> idList = new ArrayList<>();
            int startIndex = page * pageSize;
            int endIndex = startIndex + pageSize;
            for ( int i = startIndex; i < endIndex; i++ ) {
                if ( i < finalDocumentList.size() ) {
                    Index item = finalDocumentList.get(i);
                    UUID uuid = UUID.fromString(item.getUrl());
                    idList.add(uuid);
                }
            }

            // get the actual words used to search for highlighting
            Map<String, Integer> relatedWordSet = new HashMap<>();
            List<SSearchWord> searchWords = new ArrayList<>();
            query.getSearchItem().getSearchTerms(searchWords);
            for ( SSearchWord word : searchWords ) {
                relatedWordSet.put(word.getWord(), 1); // immediate relationship
                List<RelatedWord> relatedWordList = synonymRelationshipProvider.getRelationships(word.getWord());
                if ( relatedWordList != null ) {
                    for (RelatedWord rw : relatedWordList) {
                        if (!relatedWordSet.containsKey(rw.getWord())) {
                            relatedWordSet.put(rw.getWord(), 2);
                        }
                    }
                }
            }

            // read all the applicable tuples
            List<Tuple> tupleList = dao.getTupleDao().readTuples( organisation_id, idList );
            List<TupleResult> tupleResultList = new ArrayList<>();
            if ( tupleList != null && tupleList.size() > 0 ) {
                // group offsets into sets of highlights within windowSize
                SSearchHighlighter highlighter = new SSearchHighlighter(lexicon);
                for ( Tuple tuple : tupleList ) {
                    tupleResultList.add(highlighter.getHighlightStringForTuple(tuple, meta_c_type, relatedWordSet));
                }
            }

            return new TupleResultList(tupleResultList);

        }
        return null;
    }


}

