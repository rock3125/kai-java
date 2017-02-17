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

package industries.vocht.viki.indexer;

import industries.vocht.viki.IDao;
import industries.vocht.viki.dao.IndexDao;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.model.Acl;
import industries.vocht.viki.model.indexes.Index;
import industries.vocht.viki.relationship.RelatedWord;
import industries.vocht.viki.relationship.WordSplitterRelationshipProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 12/06/16.
 *
 * a simple indexer for finding documents in the admin UI
 *
 */
@Component
public class IndexerDocumentFind extends IndexerFindCommon {

    @Autowired
    private IDao dao;

    @Autowired
    private WordSplitterRelationshipProvider wordSplitterRelationshipProvider;

    // what shard of the index system these items live
    @Value("${indexer.document.index.shard:2003000}")
    private int shard;

    public IndexerDocumentFind() {
    }


    /**
     * index the document for find-ability
     * @param organisation_id the organisation owner
     * @param document the document to index
     * @throws IOException
     */
    public void index( UUID organisation_id, Document document ) throws IOException {

        // get the storage repository
        IndexDao indexRepository = dao.getIndexDao();
        if ( document != null && document.getUrl() != null && organisation_id != null ) {
            List<RelatedWord> relatedWordList = getDocumentIndexItems(document);
            int offset = 0;
            for ( RelatedWord word : relatedWordList ) {
                indexRepository.addIndex(organisation_id, new Index(document.getUrl(), word.getWord(), shard, null, -1,
                                                                    Document.META_FIND_DOCUMENT_UI, 0, document.getAclHash(), null, offset));
                offset++;
            }
            // done!
            indexRepository.flushIndexes();
        }
    }

    /**
     * return a set of urls for an item in the document shard
     * @param organisation_id the organisation to look for
     * @param searchStr the words to look for
     * @return a list of urls for the document
     */
    public List<String> findDocument( UUID organisation_id, String searchStr ) {
        return readIndexesWithFilter( dao, organisation_id, searchStr, shard, Document.META_FIND_DOCUMENT_UI );
    }

    /**
     * expand document to related words
     * @param document the document to get entities from
     * @return the expanded entity list
     */
    private List<RelatedWord> getDocumentIndexItems(Document document) {
        List<RelatedWord> wordList = new ArrayList<>();
        if ( document != null ) {

            wordList.addAll(wordSplitterRelationshipProvider.getRelationships(document.getUrl()));
            wordList.addAll(wordSplitterRelationshipProvider.getRelationships(document.getOrigin()));
            for ( String key : document.getName_value_set().keySet() ) {
                String value = document.getName_value_set().get(key);
                if ( value != null ) {
                    wordList.addAll(wordSplitterRelationshipProvider.getRelationships(key));
                    wordList.addAll(wordSplitterRelationshipProvider.getRelationships(value));
                }
            }
            for ( Acl acl : document.getAcl_set() ) {
                wordList.addAll(wordSplitterRelationshipProvider.getRelationships(acl.getUser_group()));
            }
        }
        return wordList;
    }

    /**
     * un-index a document
     * @param organisation_id the organisation
     * @param url the document
     * @throws IOException
     */
    public void unindexDocument( UUID organisation_id, String url ) throws IOException {
        // get the storage repository
        IndexDao indexRepository = dao.getIndexDao();
        indexRepository.removeIndex(organisation_id, url, Document.META_FIND_DOCUMENT_UI);
    }



}
