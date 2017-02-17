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

package industries.vocht.viki.model.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by peter on 8/04/16.
 *
 * an object for instigating a search, the parameters for a search
 *
 */
public class SearchObject {

    private String search_text;
    private String author;
    private String url;
    private String document_type;
    private List<String> url_list;

    // the metadata field to use
    private String metadata;

    // multiple-free metadata items
    private Map<String, String> metadata_set;

    // list of ambiguous words used
    private List<UISynsetSet> synset_set_list;

    // the user's email - who is doing the searching (security access)
    private String email;

    public SearchObject() {
        url_list = new ArrayList<>();
        synset_set_list = new ArrayList<>();
    }

    public String getSearch_text() {
        return search_text;
    }

    public void setSearch_text(String search_text) {
        this.search_text = search_text;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDocument_type() {
        return document_type;
    }

    public void setDocument_type(String document_type) {
        this.document_type = document_type;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<UISynsetSet> getSynset_set_list() {
        return synset_set_list;
    }

    public void setSynset_set_list(List<UISynsetSet> synset_set_list) {
        this.synset_set_list = synset_set_list;
    }

    public Map<String, String> getMetadata_set() {
        return metadata_set;
    }

    public void setMetadata_set(Map<String, String> metadata_set) {
        this.metadata_set = metadata_set;
    }

    public List<String> getUrl_list() {
        return url_list;
    }

    public void setUrl_list(List<String> url_list) {
        this.url_list = url_list;
    }
}


