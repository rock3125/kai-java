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
import industries.vocht.viki.dao.PennType;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.model.indexes.Index;
import industries.vocht.viki.model.user.User;
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
 * a simple indexer for finding users in the admin UI
 *
 */
@Component
public class IndexerUserFind extends IndexerFindCommon {

    @Autowired
    private IDao dao;

    @Autowired
    private WordSplitterRelationshipProvider wordSplitterRelationshipProvider; // how we find relationships for words

    // what shard of the index system these items live
    @Value("${indexer.user.index.shard:2002000}")
    private int shard;

    public IndexerUserFind() {
    }

    /**
     * index a user object
     * @param user the user
     * @throws IOException
     */
    public void index( User user ) throws IOException {

        // get the storage repository
        IndexDao indexRepository = dao.getIndexDao();
        List<RelatedWord> relatedWordList = getUserIndexItems(user);
        UUID organisation_id = user.getOrganisation_id();

        int offset = 0;
        for ( RelatedWord word : relatedWordList ) {
            indexRepository.addIndex(organisation_id, new Index(user.getEmail(), word.getWord(), shard, null,
                                    -1, Document.META_FIND_USER_UI, 0, 0, PennType.NNP.toString(), offset));
            offset++;
        }
        // done!
        indexRepository.flushIndexes();
    }

    /**
     * read a set of user emails (or null) given a filter
     * @param organisation_id the organisation
     * @param words the filter words
     * @return a list of user emails or null
     */
    public List<String> findUser(UUID organisation_id, String words) {
        return readIndexesWithFilter( dao, organisation_id, words, shard, Document.META_FIND_USER_UI);
    }

    /**
     * remove a user's index
     * @param organisation_id the organisation's id
     * @param user_id the user's id
     * @throws IOException
     */
    public void unindexUser( UUID organisation_id, UUID user_id ) throws IOException {
        // get the storage repository
        IndexDao indexRepository = dao.getIndexDao();
        indexRepository.removeIndex(organisation_id, user_id.toString(), Document.META_FIND_USER_UI);
    }


    /**
     * expand entity to related words
     * @param user the user to expand
     * @return the expanded entity list
     */
    private List<RelatedWord> getUserIndexItems(User user) {
        List<RelatedWord> wordList = new ArrayList<>();
        if ( user != null ) {
            wordList.addAll(wordSplitterRelationshipProvider.getRelationships(user.getFirst_name()));
            wordList.addAll(wordSplitterRelationshipProvider.getRelationships(user.getSurname()));
            wordList.addAll(wordSplitterRelationshipProvider.getRelationships(user.getEmail()));
        }
        return wordList;
    }




}
