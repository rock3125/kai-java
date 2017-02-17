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
import industries.vocht.viki.model.group.Group;
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
 * a simple indexer for groups users in the admin UI
 *
 */
@Component
public class IndexerGroupFind extends IndexerFindCommon {

    @Autowired
    private IDao dao;

    @Autowired
    private WordSplitterRelationshipProvider wordSplitterRelationshipProvider; // how we find relationships for words

    // what shard of the index system these items live
    @Value("${indexer.group.index.shard:2001000}")
    private int shard;

    public IndexerGroupFind() {
    }

    /**
     * index a group object
     * @param group the group object to index
     * @throws IOException
     */
    public void index( Group group ) throws IOException {

        // get the storage repository
        IndexDao indexRepository = dao.getIndexDao();
        List<RelatedWord> relatedWordList = getGroupIndexItems(group);
        UUID organisation_id = group.getOrganisation_id();

        int offset = 0;
        for ( RelatedWord word : relatedWordList ) {
            indexRepository.addIndex( organisation_id, new Index(group.getName(), word.getWord(), shard,
                    null, -1, Document.META_FIND_GROUP_UI, 0, 0, PennType.NNP.toString(), offset) );
            offset++;
        }
        // done!
        indexRepository.flushIndexes();
    }

    /**
     * read a set of group names (or null) given a filter
     * @param organisation_id the organisation
     * @param words the filter words
     * @return a list of group names or null
     */
    public List<String> find(UUID organisation_id, String words) {
        return readIndexesWithFilter( dao, organisation_id, words, shard, Document.META_FIND_GROUP_UI );
    }

    /**
     * un-index a group
     * @param organisation_id the organisation
     * @param name the name of the group (its url so to speak)
     * @throws IOException
     */
    public void unindexGroup( UUID organisation_id, String name ) throws IOException {
        // get the storage repository
        IndexDao indexRepository = dao.getIndexDao();
        indexRepository.removeIndex(organisation_id, name, Document.META_FIND_GROUP_UI);
    }


    /**
     * get all simple index items for a group that need to be indexed
     * @param group the group to index
     * @return the list of related words for the indexes
     */
    private List<RelatedWord> getGroupIndexItems(Group group) {
        List<RelatedWord> wordList = new ArrayList<>();
        if ( group != null ) {
            wordList.addAll(wordSplitterRelationshipProvider.getRelationships(group.getName()));
            if ( group.getUser_list() != null ) {
                for ( String str  :group.getUser_list() ) {
                    wordList.addAll(wordSplitterRelationshipProvider.getRelationships(str));
                }
            }
        }
        return wordList;
    }


}
