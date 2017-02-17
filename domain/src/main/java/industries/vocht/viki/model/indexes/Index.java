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

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by peter on 6/03/16.
 * a semantic index
 *
 */
public class Index implements IIndex, Comparable<Index> {

    private String url;                 // owner url
    private int offset;                 // word offset relative to begin of text
    private int shard;                  // system shard
    private String word;                // the word indexed
    private int synset;                 // synset index for ambigous nouns
    private String tag;                 // penn type of word
    private String word_origin;         // owner of index if applicable
    private String meta_data;           // metadata field
    private int acl_hash;               // security hash
    private int meta_c_type;            // metadata content types for tuples (see Tuple.META_C_WHO etc.)

    // not to be serialised - temporary score keeper
    @JsonIgnore
    public float score;

    // not to be serialised - temporary keyword index keeper
    @JsonIgnore
    public int keyword_index;


    public Index() {
    }

    public Index( String url, String word, int shard, String word_origin, int synset, String meta_data, int acl_hash,
                  int meta_c_type, String tag, int offset ) {
        this.url = url;
        this.word = word;
        this.offset = offset;
        this.meta_data = meta_data;
        this.word_origin = word_origin;
        this.tag = tag;
        this.acl_hash = acl_hash;
        this.meta_c_type = meta_c_type;
        this.synset = synset;
        this.shard = shard;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public String getWord_origin() {
        return word_origin;
    }

    public void setWord_origin(String word_origin) {
        this.word_origin = word_origin;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getMeta_data() {
        return meta_data;
    }

    public void setMeta_data(String meta_data) {
        this.meta_data = meta_data;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getAcl_hash() {
        return acl_hash;
    }

    public void setAcl_hash(int acl_hash) {
        this.acl_hash = acl_hash;
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

    public int getMeta_c_type() {
        return meta_c_type;
    }

    public void setMeta_c_type(int meta_c_type) {
        this.meta_c_type = meta_c_type;
    }

    @Override
    public int compareTo(Index o) {
        if ( score < o.score ) return 1;
        if ( score > o.score ) return -1;
        return 0;
    }

}


