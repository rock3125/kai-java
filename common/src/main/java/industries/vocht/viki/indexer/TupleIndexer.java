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
import industries.vocht.viki.dao.PennType;
import industries.vocht.viki.lexicon.Lexicon;
import industries.vocht.viki.lexicon.TupleUndesirables;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.TokenizerConstants;
import industries.vocht.viki.model.indexes.Index;
import industries.vocht.viki.model.indexes.TimeIndex;
import industries.vocht.viki.relationship.RelatedWord;
import industries.vocht.viki.relationship.StemRelationshipProvider;
import industries.vocht.viki.model.semantics.Tuple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 9/06/16.
 *
 * an indexer specifically created for NLU
 * take the case tuples and index them in their own shards
 *
 */
@Component
public class TupleIndexer {

    @Autowired
    private IDao dao;

    @Autowired
    private StemRelationshipProvider stemRelationshipProvider;

    @Autowired
    private TupleUndesirables tupleUndesirables;

    @Autowired
    private Lexicon lexicon;

    @Autowired
    private IHazelcast hazelcast;


    public TupleIndexer() {
    }


    /**
     * semantically index a tuple - assign it an ID if it doesn't already have one
     * @param organisation_id the organisation of the tuple
     * @param tuple the tuple for this sentence
     * @param acl_hash the security hash of the tuple
     * @throws IOException error
     */
    public void indexTuple(UUID organisation_id, Tuple tuple, int acl_hash ) throws IOException {

        if ( organisation_id != null && tuple != null && tuple.getRoot() != null ) {
            // tuple must be valid!!!
            if ( tuple.getOrganisation_id() == null || tuple.getUrl() == null ) {
                throw new IOException("invalid tuple, organisation_id or url null");
            }

            // allocate a new id if not yet set
            UUID tuple_id = tuple.getId();
            if (tuple.getId() == null) {
                tuple_id = UUID.randomUUID();
            }

            int meta_c_type = Tuple.META_C_ALL;

            List<Token> item_list = tuple.getRoot().retrieveAllTokens();
            List<Token> verb_list = tuple.getRoot().retrieveAllVerbs();
            tuple.setId(tuple_id);
            tuple.setOrganisation_id(organisation_id);

            // setup the c-type for the indexes
            meta_c_type = getMetaCType(meta_c_type, item_list);
            for ( Token verb : verb_list ) {
                Token verbt = dealWithContraction(verb);
                if (verbt != null && !verbt.getText().equals(verb.getText())) {
                    item_list.add(verbt);
                }
            }
            indexTuple(organisation_id, tuple_id, item_list, Tuple.META_TUPLE, meta_c_type, acl_hash);

            // save tuple to tuple store
            dao.getTupleDao().writeTuple(organisation_id, tuple);
        }
    }

    /**
     * update the meta_c_type depending on its current status and the items in the token list
     * @param meta_c_type the existing meta_c_type
     * @param tokenList the list of tokens to process
     * @return the updated or unmodified meta_c_type
     */
    private int getMetaCType(int meta_c_type, List<Token> tokenList) {
        // determine the meta_c_type
        for ( Token token : tokenList ) {
            if ( (token.getPennType() != null && token.getPennType() == PennType.PRP) ||
                    (token.getSemantic() != null && lexicon.isPersonSemantic(token.getSemantic()) ) ) {
                if ((meta_c_type & Tuple.META_C_WHO) == 0) {
                    meta_c_type += Tuple.META_C_WHO;
                }
            }
            if ( token.getSemantic() != null && lexicon.isLocationSemantic(token.getSemantic()) ) {
                if ((meta_c_type & Tuple.META_C_WHERE) == 0) {
                    meta_c_type += Tuple.META_C_WHERE;
                }
            }
            if ( token.getGrammarRuleName() != null && token.getValue() > 0L &&
                    (token.getGrammarRuleName().startsWith("date.") || token.getGrammarRuleName().startsWith("time.")) ) {
                if ((meta_c_type & Tuple.META_C_WHEN) == 0) {
                    meta_c_type += Tuple.META_C_WHEN;
                }
            }
            if ( (token.getPennType() != null && (token.getPennType().toString().startsWith("VB")) ||
                    token.getPennType().toString().startsWith("JJ")) ) {
                if ((meta_c_type & Tuple.META_C_HOW) == 0) {
                    meta_c_type += Tuple.META_C_HOW;
                }
            }
            if ( (token.getPennType() != null && token.getPennType().toString().startsWith("NN")) ) {
                if ((meta_c_type & Tuple.META_C_WHAT) == 0) {
                    meta_c_type += Tuple.META_C_WHAT;
                }
            }
        }
        return meta_c_type;
    }

    /**
     * revert contractions back to verbs
     * @param token the token to check
     * @return the original token, or its contraction verb if it exists or null otherwise
     */
    private Token dealWithContraction(Token token) {
        if ( token != null && token.getSemantic() != null && token.getSemantic().equals("contraction") ) {
            return TokenizerConstants.contractionToVerbToken(token.getText());
        }
        return token;
    }

    /**
     * index a semantic set of tokens in a particular metadata role of the case tuple
     * @param organisation_id the organisation owner
     * @param tuple_id the unique id of the tuple
     * @param tokenList the list of tokens of the tuple
     * @param metadata the metadata category of the tokens
     * @param acl_hash the security hash of the index/tuple
     */
    private void indexTuple( UUID organisation_id, UUID tuple_id, List<Token> tokenList, String metadata, int meta_c_type, int acl_hash ) {
        if ( tokenList != null && tokenList.size() > 0 ) {


            // get the storage repository
            IndexDao indexRepository = dao.getIndexDao();

            int offset = 0;
            for (Token token : tokenList) {

                String text = token.getText().toLowerCase();
                if ( token.getGrammarRuleName() == null && !tupleUndesirables.isUndesirable(text) ) {

                    // add word and its stems
                    List<RelatedWord> wordList = stemRelationshipProvider.getRelationships(text);

                    for (RelatedWord word : wordList) {

                        // setup word origin
                        String word_origin = null;
                        if (word.getWord().compareToIgnoreCase(text) != 0) {
                            word_origin = text;
                        }

                        // index the word
                        Index index = new Index(tuple_id.toString(), word.getWord(),
                                hazelcast.getSemanticShard(organisation_id, metadata, word.getWord()), word_origin, -1,
                                metadata, acl_hash, meta_c_type, token.getPennType().toString(), offset);
                        indexRepository.addIndex(organisation_id, index );

                    } // for each word

                } // if not undesirable

                offset++;
            }

            // done!
            indexRepository.flushIndexes();
        }
    }


    /**
     * index a semantic set of tokens for time
     * @param organisation_id the organisation owner
     * @param id the unique id of the tuple
     * @param tokenList the list of tokens of the tuple hopefully containing time
     * @param acl_hash the security hash of the index/tuple
     */
    private void indexTime( UUID organisation_id, UUID id, List<Token> tokenList, int acl_hash ) {
        if ( tokenList != null && tokenList.size() > 0 ) {

            // collect time/date values for separate indexing
            List<TimeIndex> timeIndexList = new ArrayList<>();

            // get the storage repository
            IndexDao indexRepository = dao.getIndexDao();

            int offset = 0;
            for (Token word : tokenList) {

                // is this a date or time based token?  need special indexing
                if ( word.getValue() > 0L && word.getGrammarRuleName() != null &&
                        (word.getGrammarRuleName().startsWith("time.") || word.getGrammarRuleName().startsWith("date.") ) ) {
                    timeIndexList.add( new TimeIndex(id.toString(), offset, word.getValue(), acl_hash) );
                }

                offset++;
            }

            // save date/time indexes
            if ( timeIndexList.size() > 0 ) {
                indexRepository.addTimeIndexes(organisation_id, timeIndexList);
            }

            // done!
            indexRepository.flushIndexes();
        }
    }


}

