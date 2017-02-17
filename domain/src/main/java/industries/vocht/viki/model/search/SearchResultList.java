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

import java.util.*;

/**
 * Created by peter on 8/04/16.
 *
 * the list of results
 *
 */
public class SearchResultList {

    private UUID organisation_id;
    private List<SearchResult> search_result_list;
    private List<UISynsetSet> synset_set_list;
    private int page;
    private int items_per_page;
    private int total_document_count;
    private String ai_answer;               // query was answered by AIML if not null

    public SearchResultList() {
        this.search_result_list = new ArrayList<>();
        this.synset_set_list = new ArrayList<>();
    }

    public SearchResultList(List<SearchResult> search_result_list) {
        this.search_result_list = search_result_list;
        this.synset_set_list = new ArrayList<>();
    }

    public List<SearchResult> getSearch_result_list() {
        return search_result_list;
    }

    public void setSearch_result_list(List<SearchResult> search_result_list) {
        this.search_result_list = search_result_list;
    }

    public UUID getOrganisation_id() {
        return organisation_id;
    }

    public void setOrganisation_id(UUID organisation_id) {
        this.organisation_id = organisation_id;
    }

    public int getItems_per_page() {
        return items_per_page;
    }

    public void setItems_per_page(int items_per_page) {
        this.items_per_page = items_per_page;
    }

    public int getTotal_document_count() {
        return total_document_count;
    }

    public void setTotal_document_count(int total_document_count) {
        this.total_document_count = total_document_count;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public List<UISynsetSet> getSynset_set_list() {
        return synset_set_list;
    }

    public void setSynset_set_list(List<UISynsetSet> synset_set_list) {
        this.synset_set_list = synset_set_list;
    }

    public String getAi_answer() {
        return ai_answer;
    }

    public void setAi_answer(String ai_answer) {
        this.ai_answer = ai_answer;
    }

    // sort by url
    public void sort() {
        Collections.sort(search_result_list);
    }

}
