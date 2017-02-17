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

import com.carrotsearch.hppc.IntHashSet;
import industries.vocht.viki.model.indexes.*;
import industries.vocht.viki.relationship.SynonymRelationshipProvider;

import java.util.*;

/**
 * Created by peter on 4/06/16.
 *
 * collapse results according to user search requirements
 * discard anything that doesn't fit the results
 *
 */
public class SSearchCollapseResults {

    private float timeIndexScoreHit;
    private SynonymRelationshipProvider synonymRelationshipProvider;

    public SSearchCollapseResults( SynonymRelationshipProvider synonymRelationshipProvider, float timeIndexScoreHit ) {
        this.synonymRelationshipProvider = synonymRelationshipProvider;
        this.timeIndexScoreHit = timeIndexScoreHit;
    }


    /**
     * use the max-distance allowed window size over the map of document oriented indexes
     * to cut down the indexes.  Use the word-list to cut down incomplete segments
     * @param indexMap the map of indexes by document url
     * @param numKeywords the number of keywords to look for
     * @param maxDistanceAllowed the maximum distance allowed between words for grouping, 0 = document level
     * @return the survivor set of indexes with adjusted scores (per document)
     */
    public Map<String, DocumentIndexSet> collapseAndScoreResultsByWindow(Map<String, DocumentIndexSet> indexMap,
                                                                               int numKeywords,
                                                                               int maxDistanceAllowed ) {
        // set scores onto map
        Map<String, DocumentIndexSet> resultSetMap = new HashMap<>();
        for ( String url : indexMap.keySet() ) {
            // make the index distances possible - within whatever limits we have set above
            DocumentIndexSet documentIndexSet = combineLocalIndexes(indexMap.get(url), numKeywords, maxDistanceAllowed);
            if ( documentIndexSet != null && documentIndexSet.getDocumentIndexList() != null &&
                    documentIndexSet.getDocumentIndexList().size() > 0 ) {
                resultSetMap.put(url, documentIndexSet);
            }
        }
        if ( resultSetMap.size() > 0 ) {
            return resultSetMap;
        }
        return null;
    }

    /**
     * combine indexes close enough to each other into one data-set with offsets
     * @param indexList a list of indexes for the same document
     * @oaram numKeywords the number of keywords that we're searching for
     * @param maxDistanceAllowed the distance allowed between keywords to be valid (<= 0 is document level)
     * @return the combined set of indexes
     */
    private DocumentIndexSet combineLocalIndexes(DocumentIndexSet indexList, int numKeywords, int maxDistanceAllowed ) {
        // combine local indexes based on max-dinstance allowed and return a combined index set
        // data-structure
        if ( maxDistanceAllowed <= 0 ) { // no limits

            return indexList;

        } else {

            // there is a distance limits, and we have more than one index
            if ( indexList.getDocumentIndexList().size() > 1 ) {

                DocumentIndexSet newSet = new DocumentIndexSet();

//                // combine indexes based on max-distance allowed
//                List<Index> combinedSet = new ArrayList<>();
//                IIndex index1 = indexList.get(0); // get zero item
//                if ( index1 instanceof  Index) {
//                    combinedSet.add( (Index)index1 );
//                }
//                DocumentIndexSet currentItem = new DocumentIndexSet(index1.getUrl(), getIndexScore(index1), index1.getOffset());
//
//                // take note of its offset
//                int currentOffset = index1.getOffset();
//                for ( int i = 1; i < indexList.size(); i++ ) {
//                    IIndex index2 = indexList.get(i); // inside the max distance region?
//                    if ( Math.abs(currentOffset - index2.getOffset()) <= maxDistanceAllowed ) {
//                        currentItem.addOffsetWithScore( index2.getOffset(), getIndexScore(index2) );
//                        if ( index2 instanceof  Index) {
//                            combinedSet.add( (Index)index2 );
//                        }
//                    } else { // we've exceeded the max length from start to end - create a new offset for the next
//                        if ( validIndexSet( combinedSet, numKeywords) ) {
//                            currentItem.adjustScore( numKeywords ); // adjust score for distance
//                            resultList.add(currentItem); // save the item
//                        }
//                        currentItem = new DocumentIndexSet(index2.getUrl(), getIndexScore(index2), index2.getOffset());
//                        if ( index2 instanceof  Index) {
//                            combinedSet.add( (Index)index2 );
//                        }
//                        currentOffset = index2.getOffset();
//                    }
//                }
//                // add last item after processing
//                if ( validIndexSet( combinedSet, numKeywords) ) {
//                    currentItem.adjustScore(numKeywords); // adjust score for distance
//                    resultList.add(currentItem);
//                }
                return newSet;

            } else {

                // single index, single offset - easy - it already matches orderedSearchWordList since it made it through
                return indexList;

            }
        }
    }

    /**
     * determine if a set of indexes fits all the required search words, return true if so
     * @param indexList the list of indexes to check
     * @param numKeywords the number of keywords
     * @return true if its a fit
     */
    private boolean validIndexSet( List<Index> indexList, int numKeywords ) {
        if ( numKeywords > indexList.size() ) { // more words than indexes?
            return false;
        } else {
            IntHashSet setMatch = new IntHashSet(indexList.size());
            for ( Index index : indexList ) {
                setMatch.add(index.keyword_index);
            }
            // matching?
            return setMatch.size() == numKeywords;
        }
    }

    // get the appropriate score for an index
    private float getIndexScore( IIndex index ) {
        if ( index instanceof TimeIndex) {
            return timeIndexScoreHit;
        } else {
            Index index1 = (Index)index;
            return index1.score;
        }
    }

    // temporary index structure for scoring document wide indexes
    private class TempIndex implements Comparable<TempIndex> {
        public TempIndex(int keywordIndex, int offset, float score) {
            this.keywordIndex = keywordIndex;
            this.offset = offset;
            this.score = score;
        }
        public float score;
        public int keywordIndex;
        public int offset;
        // sort by smallest offset first
        @Override
        public int compareTo(TempIndex o) {
            if ( offset < o.offset ) return -1;
            if ( offset > o.offset ) return 1;
            return 0;
        }
    }

    /**
     * convert a set of indexes to temp indexes that can be sorted and recognized for their keywords
     * @param indexList the list of indexes to convert (only type Index is converted)
     * @param stringToKeywordIndex the list of keywords (and their relationships)
     * @return a list of converted indexes
     */
    private List<TempIndex> convertIndexes(List<IIndex> indexList, Map<String, Integer> stringToKeywordIndex) {
        List<TempIndex> tempList = new ArrayList<>();
        for ( IIndex index : indexList ) {
            if ( index instanceof Index ) {
                Index i = (Index)index;
                Integer kwIndex = stringToKeywordIndex.get(i.getWord());
                if ( kwIndex != null ) {
                    tempList.add(new TempIndex(kwIndex, i.getOffset(), i.score));
                }
            }
        }
        Collections.sort(tempList); // must be sorted in order of offset
        return tempList;
    }

    /**
     * does this set of temporary indexes represent a complete set of keywords inside it?
     * @param tempList the list to check
     * @param numKeywords the number of keywords to look for
     * @return true if conditions are met
     */
    private boolean isComplete( List<TempIndex> tempList, int numKeywords ) {
        IntHashSet set = new IntHashSet();
        // add them to a set to find how many UNIQUE indexes we have
        for ( TempIndex tempIndex : tempList ) {
            set.add( tempIndex.keywordIndex );
        }
        return set.size() >= numKeywords;
    }

    /**
     * adjust a document wide scoring - find the "smallest" / narrowest possible combination of all keywords
     * and use that as a multiplier on the document's final score if there were more than 1 keyword used in the search
     * @param indexList the document level indexes for this document
     * @param numKeywords the number of keywords to look for
     * @param stringToKeywordIndex the keywords that were searched for
     */
    private float getDocumentScoreAdjustment( List<IIndex> indexList,  int numKeywords, Map<String, Integer> stringToKeywordIndex ) {
        if ( indexList != null && stringToKeywordIndex != null ) {
            List<TempIndex> tempList = convertIndexes(indexList, stringToKeywordIndex);

            int bestDistance = Integer.MAX_VALUE;
            for ( int i = 0; i < tempList.size() - (numKeywords-1); i++ ) {
                List<TempIndex> fragmentList = new ArrayList<>();
                for ( int j = i; j < tempList.size(); j++ ) {
                    fragmentList.add( tempList.get(j) );
                    if ( fragmentList.size() >= numKeywords && isComplete(fragmentList, numKeywords) ) {
                        int thisDistance = (fragmentList.get(fragmentList.size()-1).offset - fragmentList.get(0).offset) + 1;
                        if ( thisDistance < bestDistance ) {
                            bestDistance = thisDistance;
                        }
                        break;
                    }
                }
            }
            // did we find a best distance?
            if ( bestDistance < Integer.MAX_VALUE ) {
                return (float) numKeywords / (float) bestDistance;
            }
            return 0.001f; // all other cases - downgrade the score if we can't find it
        }
        return 1.0f; // only one keyword - always x 1.0
    }


}
