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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by peter on 8/04/16.
 *
 * a single result of a search
 *
 */
public class SearchResult implements Comparable<SearchResult> {

    private String url;
    private String title;
    private String author;
    private String created_date;
    private List<String> text_list;
    private float score;

    private Map<String, Integer> person_set;
    private Map<String, Integer> location_set;
    private Map<String, Integer> time_set;

    public SearchResult() {
        this.text_list = new ArrayList<>();
        this.person_set = new HashMap<>();
        this.location_set = new HashMap<>();
        this.time_set = new HashMap<>();
    }

    public SearchResult( String url, List<String> text_list, float score) {
        this.url = url;

        this.text_list = text_list;
        this.score = score;
        this.person_set = new HashMap<>();
        this.location_set = new HashMap<>();
        this.time_set = new HashMap<>();
    }


    public List<String> getText_list() {
        return text_list;
    }

    public void setText_list(List<String> text_list) {
        this.text_list = text_list;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCreated_date() {
        return created_date;
    }

    public void setCreated_date(String created_date) {
        this.created_date = created_date;
    }

    public Map<String, Integer> getPerson_set() {
        return person_set;
    }

    public void setPerson_set(Map<String, Integer> person_set) {
        this.person_set = person_set;
    }

    public Map<String, Integer> getLocation_set() {
        return location_set;
    }

    public void setLocation_set(Map<String, Integer> location_set) {
        this.location_set = location_set;
    }

    public Map<String, Integer> getTime_set() {
        return time_set;
    }

    public void setTime_set(Map<String, Integer> time_set) {
        this.time_set = time_set;
    }

    @Override
    public int compareTo(SearchResult searchResult) {
        // score is the alpha comparison
        if ( score < searchResult.score ) return 1;
        if ( score > searchResult.score ) return -1;
        // secondary compare based on length of the list
        if ( text_list.size() < searchResult.text_list.size() ) return 1;
        if ( text_list.size() > searchResult.text_list.size() ) return -1;
        return 0;
    }


}


