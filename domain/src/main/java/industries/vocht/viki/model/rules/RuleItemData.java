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

package industries.vocht.viki.model.rules;

/**
 * Created by peter on 12/05/16.
 *
 * a data collection of items (like a union) for UI storage and processing
 *
 */
public class RuleItemData {

    private String origin_filter;
    private String document_type_filter;

    private String time_csv;
    private String word_csv;
    private String metadata;
    private Boolean exact;

    private String interval_unit;
    private Integer interval;

    private String name;
    private String value;

    private String protocol;
    private String url;
    private String path;
    private String username;
    private String password;
    private String domain;

    private String to;
    private String subject;

    public RuleItemData() {
    }

    public String getTime_csv() {
        return time_csv;
    }

    public void setTime_csv(String time_csv) {
        this.time_csv = time_csv;
    }

    public String getWord_csv() {
        return word_csv;
    }

    public void setWord_csv(String word_csv) {
        this.word_csv = word_csv;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Boolean isExact() {
        return exact;
    }

    public void setExact(Boolean exact) {
        this.exact = exact;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getInterval_unit() {
        return interval_unit;
    }

    public void setInterval_unit(String interval_unit) {
        this.interval_unit = interval_unit;
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    public String getOrigin_filter() {
        return origin_filter;
    }

    public void setOrigin_filter(String origin_filter) {
        this.origin_filter = origin_filter;
    }

    public String getDocument_type_filter() {
        return document_type_filter;
    }

    public void setDocument_type_filter(String document_type_filter) {
        this.document_type_filter = document_type_filter;
    }
}



