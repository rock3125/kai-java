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

package industries.vocht.viki.model.super_search;

import industries.vocht.viki.model.indexes.IIndex;

import java.util.List;

/**
 * Created by peter on 25/04/16.
 *
 * ordinary semantic index search
 *
 */
public class SSearchWord implements ISSearchItem {

    private List<IIndex> indexList;
    private String word;
    private String tag;  // penn-tag
    private String semantic; // semantic tag
    private String metadata; // meta-data tag
    private boolean exact;  // exact match?
    private int synset; // semantic synset if applicable (or -1 if n/a)
    private int shard; // the shard for this word
    private int meta_c_filter; // the tuple metadata content filter (see Tuple.META_C_WHO, etc.)

    public SSearchWord() {
        this.synset = -1;
        this.shard = 0;
    }

    public SSearchWord( String word, String tag, String metadata, int shard, int meta_c_filter, boolean exact ) {
        this.word = word;
        this.tag = tag;
        this.metadata = metadata;
        this.exact = exact;
        this.synset = -1;
        this.shard = shard;
        this.meta_c_filter = meta_c_filter;
    }

    public void getSearchTerms(List<SSearchWord> inList) {
        if ( inList != null ) {
            inList.add(this);
        }
    }

    public List<IIndex> getIndexList() {
        return indexList;
    }

    public void setIndexList(List<IIndex> indexList) {
        this.indexList = indexList;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public boolean isExact() {
        return exact;
    }

    public void setExact(boolean exact) {
        this.exact = exact;
    }

    public String getSemantic() {
        return semantic;
    }

    public void setSemantic(String semantic) {
        this.semantic = semantic;
    }

    public int getSynset() {
        return synset;
    }

    public void setSynset(int synset) {
        this.synset = synset;
    }

    public int getShard() {
        return shard;
    }

    public void setShard(int shard) {
        this.shard = shard;
    }

    public int getMeta_c_filter() {
        return meta_c_filter;
    }

    public void setMeta_c_filter(int meta_c_filter) {
        this.meta_c_filter = meta_c_filter;
    }
}


