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
import industries.vocht.viki.lexicon.*;
import industries.vocht.viki.model.*;
import industries.vocht.viki.model.indexes.Index;
import industries.vocht.viki.model.indexes.TimeIndex;
import industries.vocht.viki.relationship.RelatedWord;
import industries.vocht.viki.relationship.SynonymRelationshipProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/*
 * Created by peter on 20/12/14.
 *
 * the thing that creates indexes, take a document, and each tokenized item
 * in that document, and using each concept/word get a set of relationships
 * and persist that to the indexes system - semantic indexing as well as WSD using NN
 *
 *
 */
@Component
public class Indexer {

    private final static Logger logger = LoggerFactory.getLogger(Indexer.class);

    @Value("${svm.sliding.windows.size:25}")
    private int windowSize;

    @Autowired
    private SynonymRelationshipProvider synonymRelationshipProvider; // the synonym based (semantic) relationship provider

    @Autowired
    private Undesirables undesirables;

    @Autowired
    private IHazelcast hazelcast;

    @Autowired
    private IDao dao;

    @Autowired
    private AmbiguousLexicon ambiguousLexicon;



    public Indexer() {
    }

    /**
     * index a document - a list of tokens
     *
     * @param organisation_id the organisation owner
     * @param url the url of the document
     * @param metadataTag the meta-data tag to store the tokens under
     * @param acl_hash the hash of the document's security acls
     * @param sentenceList the set of sentences to index
     * @return the count of the number of indexes created for this set
     */
    public long indexDocument( UUID organisation_id, String url, String metadataTag,
                               int acl_hash, List<Sentence> sentenceList ) throws IOException {

        // only index valid/known metadata tags
        if ( !Document.validMetadataSet.contains(metadataTag) ) {
            return 0L;
        }

        long indexCount = 0;

        // get the storage repository
        IndexDao indexRepository = dao.getIndexDao();

        if ( sentenceList != null && sentenceList.size() > 0 && organisation_id != null ) {

            // collect time/date values for separate indexing
            List<TimeIndex> timeIndexList = new ArrayList<>();

            // one large list of tokens - no sentences here
            List<Token> tokenList = new ArrayList<>();
            for ( Sentence sentence : sentenceList ) {
                tokenList.addAll( sentence.getTokenList() );
            }

            Map<String, Long> tempCache = new HashMap<>();
            Map<String, Long> startValue = new HashMap<>();

            for ( int i = 0; i < tokenList.size(); i++ ) {

                Token word = tokenList.get(i);

                String currentWord = synonymRelationshipProvider.getStem(word.getText().toLowerCase());
                if ( undesirables == null || !undesirables.isUndesirable(currentWord) ) {

                    // is this a date or time based token?  need special indexing
                    if ( word.getValue() > 0L && word.getGrammarRuleName() != null &&
                            (word.getGrammarRuleName().startsWith("time.") || word.getGrammarRuleName().startsWith("date.") ) ) {
                        timeIndexList.add( new TimeIndex(url, i, word.getValue(), acl_hash) );

                    } else {

                        // all other tokens go into the indexes here

                        // is this an ambiguous word?
                        if (word.getSynid() < 0 ) {

                            // get synonyms for this word from the ordinary relationship provider
                            List<RelatedWord> synonymList = synonymRelationshipProvider.getRelationships(word.getText());
                            if (synonymList != null) {
                                for (RelatedWord synonym : synonymList) {
                                    String unStemmed = synonymRelationshipProvider.getStem(synonym.getWord());
                                    if (unStemmed.compareToIgnoreCase(currentWord) != 0) { // no duplicates through relationship
                                        indexRepository.addIndex(organisation_id, new Index(url, unStemmed, hazelcast.getShard(organisation_id, metadataTag, tempCache, startValue, unStemmed), currentWord, -1, metadataTag,
                                                acl_hash, 0, word.getPennType().toString(), i));

                                        indexCount++;
                                    }
                                }
                            }

                            indexRepository.addIndex(organisation_id, new Index(url, currentWord, hazelcast.getShard(organisation_id, metadataTag, tempCache, startValue, currentWord), null, -1, metadataTag,
                                    acl_hash, 0, word.getPennType().toString(), i));
                            indexCount++;

                        } else {

                            // this indexes a disambiguated syn (through the parser's WSD NNET)
                            List<LexiconSynset> synsetList = ambiguousLexicon.getSynset(currentWord);
                            if ( synsetList != null && synsetList.size() > 0 ) {

                                LexiconSynset synset;
                                if (word.getSynid() >= 0 && word.getSynid() < synsetList.size()) {
                                    synset = synsetList.get(word.getSynid());
                                } else {
                                    logger.error("indexer: synid index out of range, word " + currentWord + ", index:" + word.getSynid() + ", defaulting to 0");
                                    synset = synsetList.get(0);
                                }


                                // index the synset relationships
                                for (String synonym : synset.getRelationshipSet()) {
                                    String unStemmed = synonymRelationshipProvider.getStem(synonym);
                                    if (unStemmed.compareToIgnoreCase(currentWord) != 0) { // no duplicates through relationship
                                        indexRepository.addIndex(organisation_id, new Index(url, unStemmed, hazelcast.getShard(organisation_id, metadataTag, tempCache, startValue, unStemmed),
                                                currentWord, synset.getSynsetId(), metadataTag, acl_hash, 0, word.getPennType().toString(), i));

                                        indexCount++;
                                    }
                                }

                                // index the semantic word
                                indexRepository.addIndex(organisation_id, new Index(url, currentWord, hazelcast.getShard(organisation_id, metadataTag, tempCache, startValue, currentWord),
                                        null, synset.getSynsetId(), metadataTag,
                                        acl_hash, 0, word.getPennType().toString(), i));
                                indexCount++;

                            } else {
                                logger.error("indexer: ambiguously marked word " + currentWord + ", does not have a semantic-noun list");
                            }

                        }

                    } // else if not time token

                } // if not undesirable

            } // for each token

            // save date/time indexes
            if ( timeIndexList.size() > 0 ) {
                indexRepository.addTimeIndexes(organisation_id, timeIndexList);
                indexCount = indexCount + timeIndexList.size();
            }

            // done!
            indexRepository.flushIndexes();

            // flush shard data back to hazelcast
            hazelcast.flush(organisation_id, metadataTag, tempCache, startValue);

        }
        return indexCount;
    }

    /**
     * return a list of tokens for the semantic NN WSD system lying around i with a
     * window-size defined.  Skip any undesirables
     * @param i the offset into tokenList
     * @param tokenList the list of tokens
     * @return a window with counts of the words occurring inside the window of tokenList
     */
    private List<String> getSentenceWindow( int i, List<Token> tokenList ) {

        List<String> wordList = new ArrayList<>();

        // construct a window left and right of the word
        int size = tokenList.size();
        int left = i - windowSize;
        if ( left < 0 ) left = 0;
        int right = i + windowSize;
        if ( right + 1 >= size ) {
            right = size - 1;
        }

        // a hit for each syn is counted, we don't want any crossovers between synsets
        for ( int j = left; j <= right; j++ ) {
            Token token = tokenList.get(j);
            // ignore #s
            if ( token.getType() == TokenizerConstants.Type.Number ) {
                continue;
            }
            String part_j = token.getText().toLowerCase();
            if ( !undesirables.isUndesirable(part_j) ) {
                // vector (any word) hit count
                wordList.add(part_j);
            } // if not undesirable
        }
        return wordList;
    }


}

