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

package industries.vocht.viki.system_stats;

/**
 * Created by peter on 8/05/16.
 *
 * class for returning general system statistics
 *
 */
public class GeneralStatistics {

    private long total_count;
    private long total_valid_count;
    private long total_index_count;
    private long total_content_bytes;

    private long document_count;
    private long sentence_count;

    private long noun;
    private long proper_noun;
    private long verb;
    private long adjective;
    private long adverb;

    private long percent;
    private long date;
    private long url;
    private long time;
    private long decimal;
    private long number;
    private long email;
    private long money;
    private long phone;

    public GeneralStatistics() {
    }


    public long getTotal_count() {
        return total_count;
    }

    public void setTotal_count(long total_count) {
        this.total_count = total_count;
    }

    public long getTotal_valid_count() {
        return total_valid_count;
    }

    public void setTotal_valid_count(long total_valid_count) {
        this.total_valid_count = total_valid_count;
    }

    public long getNoun() {
        return noun;
    }

    public void setNoun(long noun) {
        this.noun = noun;
    }

    public long getProper_noun() {
        return proper_noun;
    }

    public void setProper_noun(long proper_noun) {
        this.proper_noun = proper_noun;
    }

    public long getVerb() {
        return verb;
    }

    public void setVerb(long verb) {
        this.verb = verb;
    }

    public long getAdjective() {
        return adjective;
    }

    public void setAdjective(long adjective) {
        this.adjective = adjective;
    }

    public long getAdverb() {
        return adverb;
    }

    public void setAdverb(long adverb) {
        this.adverb = adverb;
    }

    public long getPercent() {
        return percent;
    }

    public void setPercent(long percent) {
        this.percent = percent;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public long getUrl() {
        return url;
    }

    public void setUrl(long url) {
        this.url = url;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getDecimal() {
        return decimal;
    }

    public void setDecimal(long decimal) {
        this.decimal = decimal;
    }

    public long getNumber() {
        return number;
    }

    public void setNumber(long number) {
        this.number = number;
    }

    public long getEmail() {
        return email;
    }

    public void setEmail(long email) {
        this.email = email;
    }

    public long getMoney() {
        return money;
    }

    public void setMoney(long money) {
        this.money = money;
    }

    public long getPhone() {
        return phone;
    }

    public void setPhone(long phone) {
        this.phone = phone;
    }

    public long getDocument_count() {
        return document_count;
    }

    public void setDocument_count(long document_count) {
        this.document_count = document_count;
    }

    public long getTotal_index_count() {
        return total_index_count;
    }

    public void setTotal_index_count(long total_index_count) {
        this.total_index_count = total_index_count;
    }

    public long getSentence_count() {
        return sentence_count;
    }

    public void setSentence_count(long sentence_count) {
        this.sentence_count = sentence_count;
    }

    public long getTotal_content_bytes() {
        return total_content_bytes;
    }

    public void setTotal_content_bytes(long total_content_bytes) {
        this.total_content_bytes = total_content_bytes;
    }
}


