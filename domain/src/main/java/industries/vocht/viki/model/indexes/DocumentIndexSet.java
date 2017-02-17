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

package industries.vocht.viki.model.indexes;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by peter on 10/04/16.
 *
 * a combined index set that is enabled
 *
 */
public class DocumentIndexSet implements Comparable<DocumentIndexSet> {

    private String url;
    private List<DocumentIndex> documentIndexList;
    private float total_score;

    public DocumentIndexSet() {
    }

    public DocumentIndexSet(String url, int keyword_index, float score, int offset ) {
        this.url = url;
        this.documentIndexList = new ArrayList<>();
        this.total_score = this.total_score + score;
        this.documentIndexList.add( new DocumentIndex(keyword_index, score, offset) );
    }

    public String getUrl() {
        return url;
    }

    /**
     * add a new index to this index
     * @param offset the offset
     */
    public void addIndexOffset( int keyword_index, float score, int offset ) {
        this.total_score = this.total_score + score;
        this.documentIndexList.add( new DocumentIndex(keyword_index, score, offset) );
    }

    /**
     * combine another set this with one
     * @param set the set to combine
     */
    public DocumentIndexSet combine( DocumentIndexSet set ) {
        if ( set != null ) {
            this.documentIndexList.addAll(set.documentIndexList);
            this.total_score = this.total_score + set.total_score;
        }
        return this;
    }

    /**
     * combine two maps at a document level
     * @param map1 the first result map
     * @param map2 the second map
     * @return the combined map based on map1 (modifies map1)
     */
    public static Map<String, DocumentIndexSet> combine(Map<String, DocumentIndexSet> map1, Map<String, DocumentIndexSet> map2 ) {
        if ( map1 != null ) {
            if (map2 != null && map2.size() > 0) {
                for (String key2 : map2.keySet()) {
                    DocumentIndexSet set1 = map1.get(key2);
                    if (set1 == null) { // add the item to map1
                        map1.put(key2, map2.get(key2));
                    } else { // already exists - combine
                        set1.combine(map2.get(key2));
                    }
                } // for each item in map 2
            }
        }
        return map1;
    }

    public float getTotal_score() {
        return total_score;
    }

    public List<DocumentIndex> getDocumentIndexList() {
        return documentIndexList;
    }

    @Override
    public int compareTo(DocumentIndexSet documentIndexSet) {
        if ( total_score < documentIndexSet.total_score ) return 1;
        if ( total_score > documentIndexSet.total_score ) return -1;
        return url.compareTo(documentIndexSet.url);
    }


}


