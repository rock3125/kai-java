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
import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.indexes.Index;

import java.util.*;

/**
 * Created by peter on 12/06/16.
 *
 * items common between the UI based indexers and finders
 *
 */
public class IndexerFindCommon {


    /**
     * convert a list of uuid-strings to actual uuids
     * @param list the list of items
     * @return the list of converted uuid items
     */
    public List<UUID> stringListToUUIDList(List<String> list ) {
        if ( list != null && list.size() > 0 ) {
            List<UUID> uuidList = new ArrayList<>();
            for ( String str : list ) {
                uuidList.add( UUID.fromString(str) );
            }
            return uuidList;
        }
        return null;
    }

    /**
     * read a set of indexes using words as a filter for a specific set of meta-data
     * @param organisation_id the id of the organisation to read from
     * @param words a set of the words to get
     * @param shard the shard of the index
     * @return a list of URLs that matched
     */
    public List<String> readIndexesWithFilter(IDao dao, UUID organisation_id, String words, int shard, String metadata ) {
        IndexDao indexRepository = dao.getIndexDao();
        String[] wordList = words.split(" ");
        HashMap<String, List<Index>> indexHashMap = null;
        for ( String word : wordList ) {
            List<Index> indexList = indexRepository.readIndex(organisation_id, word, shard, metadata);
            if ( indexList == null || indexList.size() == 0 ) {
                return null;
            }
            if ( indexHashMap == null ) {
                indexHashMap = new HashMap<>();
                for ( Index index : indexList ) {
                    List<Index> indexList1 = new ArrayList<>();
                    indexList1.add(index);
                    indexHashMap.put( index.getUrl(), indexList1);
                }
            } else {
                HashMap<String, List<Index>> newIndexHashMap = new HashMap<>();
                for ( Index index : indexList ) {
                    List<Index> indexList1 = indexHashMap.get(index.getUrl());
                    if ( indexList1 != null ) {
                        indexList1.add(index);
                        newIndexHashMap.put( index.getUrl(), indexList1 );
                    }
                }
                if ( newIndexHashMap.size() == 0 ) {
                    return null;
                } else {
                    indexHashMap = newIndexHashMap; // move across
                }
            }
        } // for each word in the index

        if ( indexHashMap != null && indexHashMap.size() > 0 ){
            List<String> indexList = new ArrayList<>();
            for ( String url : indexHashMap.keySet() ) {
                indexList.add( url );
            }
            Collections.sort(indexList);
            return indexList;
        }
        return null;
    }


    /**
     * read a set of indexes using words as a filter for a specific set of meta-data
     * @param organisation_id the id of the organisation to read from
     * @param tokenList a list of tokens to look for
     * @param shard the shard of the index
     * @return a list of URLs that matched
     */
    public List<String> readIndexesWithTokens(IDao dao, UUID organisation_id, List<Token> tokenList, int shard, String metadata ) {
        IndexDao indexRepository = dao.getIndexDao();

        HashMap<String, List<Index>> indexHashMap = null;
        for ( Token token : tokenList ) {
            List<Index> indexList = indexRepository.readIndex(organisation_id, token.getText(), shard, metadata);
            if ( indexList == null || indexList.size() == 0 ) {
                return null;
            }
            if ( indexHashMap == null ) {
                indexHashMap = new HashMap<>();
                for ( Index index : indexList ) {
                    List<Index> indexList1 = new ArrayList<>();
                    indexList1.add(index);
                    indexHashMap.put( index.getUrl(), indexList1);
                }
            } else {
                HashMap<String, List<Index>> newIndexHashMap = new HashMap<>();
                for ( Index index : indexList ) {
                    List<Index> indexList1 = indexHashMap.get(index.getUrl());
                    if ( indexList1 != null ) {
                        indexList1.add(index);
                        newIndexHashMap.put( index.getUrl(), indexList1 );
                    }
                }
                if ( newIndexHashMap.size() == 0 ) {
                    return null;
                } else {
                    indexHashMap = newIndexHashMap; // move across
                }
            }
        } // for each word in the index

        if ( indexHashMap != null && indexHashMap.size() > 0 ){
            List<String> indexList = new ArrayList<>();
            for ( String url : indexHashMap.keySet() ) {
                indexList.add( url );
            }
            Collections.sort(indexList);
            return indexList;
        }
        return null;
    }


}
