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
import industries.vocht.viki.IHazelcast;
import industries.vocht.viki.dao.IndexDao;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.lexicon.Undesirables;
import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.indexes.Index;
import industries.vocht.viki.relationship.RelatedWord;
import industries.vocht.viki.relationship.SynonymRelationshipProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by peter on 12/06/16.
 *
 * summary index items, used by the TextRank service layer
 *
 */
@Component
public class SummaryIndexer {

    @Autowired
    private IHazelcast hazelcast;

    @Autowired
    private IDao dao;

    @Autowired
    private Undesirables undesirables;

    @Autowired
    private SynonymRelationshipProvider synonymRelationshipProvider; // the synonym based (semantic) relationship provider

    public SummaryIndexer() {
    }

    /**
     * index summarisation information for a different kind of search
     * @param organisation_id the organisation owner
     * @param url the url of the document
     * @param acl_hash the document's security
     * @param wordSetList a list of words - helper function for the real work below
     * @throws IOException
     */
    public void summaryIndexDocument(UUID organisation_id, String url, int acl_hash, List<Sentence> wordSetList ) throws IOException {
        if ( wordSetList != null && wordSetList.size() > 0 ) {
            Sentence sentence = new Sentence();
            for ( Sentence sentence1 : wordSetList ) {
                sentence.getTokenList().addAll( sentence1.getTokenList() );
            }
            summaryIndexDocument(organisation_id, url, acl_hash, sentence);
        }
    }


    /**
     * index summarisation information for a different kind of search
     * @param organisation_id the organisation owner
     * @param url the url of the document
     * @param acl_hash the document's security hash
     * @param wordSet the words to index
     * @throws IOException
     */
    public void summaryIndexDocument( UUID organisation_id, String url, int acl_hash, Sentence wordSet ) throws IOException {

        // get the storage repository
        IndexDao indexRepository = dao.getIndexDao();

        if ( wordSet != null && organisation_id != null ) {
            int offset = 0;

            Map<String, Long> tempCache = new HashMap<>();
            Map<String, Long> startValue = new HashMap<>();

            for ( Token word : wordSet.getTokenList() ) {
                String currentWord = word.getText().toLowerCase();
                if ( undesirables == null || !undesirables.isUndesirable(currentWord) ) {

                    // get synonyms for this word
                    List<RelatedWord> synonymList = synonymRelationshipProvider.getRelationships(word.getText());
                    if ( synonymList != null ) {
                        for (RelatedWord synonym : synonymList) {
                            String unStemmed = synonymRelationshipProvider.getStem(synonym.getWord());
                            if ( unStemmed.compareToIgnoreCase(currentWord) != 0 ) { // no duplicates through relationship
                                indexRepository.addIndex(organisation_id, new Index(url, unStemmed,
                                        hazelcast.getShard(organisation_id, Document.META_SUMMARIZATION, tempCache, startValue, unStemmed), currentWord, -1,
                                        Document.META_SUMMARIZATION, acl_hash, 0, word.getPennType().toString(), offset));
                            }
                        }
                    }
                    indexRepository.addIndex(organisation_id, new Index(url, currentWord,
                            hazelcast.getShard(organisation_id, Document.META_SUMMARIZATION, tempCache, startValue, currentWord), null, -1,
                            Document.META_SUMMARIZATION, acl_hash, 0, word.getPennType().toString(), offset) );
                }
                offset++;
            }

            // done!
            indexRepository.flushIndexes();
            hazelcast.flush(organisation_id, Document.META_SUMMARIZATION, tempCache, startValue);

        }
    }



}
