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

package industries.vocht.viki.agent_common.client_interface;


import java.util.*;

/**
 * Created by peter on 4/03/16.
 *
 */
public class AgentDocument implements Comparable<AgentDocument> {

    // reserved metadata items
    public static final String META_BODY = "{body}";
    public static final String META_ORIGIN = "{origin}";
    public static final String META_TITLE = "{title}";
    public static final String META_AUTHOR = "{author}";
    public static final String META_URL = "{url}";
    public static final String META_ACLS = "{acls}";
    public static final String META_UPLOAD_DATE_TIME = "{uploaded-date-time}";
    public static final String META_CREATED_DATE_TIME = "{created-date-time}";
    public static final String META_LAST_MODIFIED_DATE_TIME = "{last-modified-date-time}";
    public static final String META_SUMMARIZATION = "{summary}";
    public static final String META_CLASSIFICATION = "{classification}";

    private UUID organisation_id;
    private String url;
    private HashSet<AgentAcl> acl_set;
    private Map<String, String> name_value_set;
    private String origin;
    // the hash value of this document's acls
    private int aclHash;

    private String author;
    private String title;
    private long created;

    // content's unique key (binary)
    private String content_hash;

    // bit patterns for the processing pipe-line
    // a 1 indicates "do it" and a 0 is a skip
    private long processingPipeline = -1L;

    // time stamps for processes - when they took place
    private long ts_converted;
    private long ts_parsed;
    private long ts_vectorised;
    private long ts_summarised;
    private long ts_indexed;
    private long ts_clustered;
    private long ts_entity_analysed;
    private long ts_emotion_analysed;
    private long ts_knowledge_analysed;

    // uploaded time-stamp
    private long date_time_uploaded;

    public AgentDocument() {
        acl_set = new HashSet<>();
        name_value_set = new HashMap<>();
    }

    /**
     * copy name-value data from document into this item
     * @param document the item to copy from
     */
    public void mergeMetadata( AgentDocument document ) {
        if ( document.getName_value_set().size() > 0 ) {
            for ( String name : document.getName_value_set().keySet() ) {
                String otherValue = document.getName_value_set().get(name);
                if ( otherValue != null ) {
                    name_value_set.put(name, otherValue);
                }
            }
        }
    }

    public String toString() {
        return "Document: " + getOrganisation_id().toString() + ":" + url;
    }

    /**
     * turn an internal meta-data name to a "pretty" printable / presentable meta-data name
     * @param name the name to change
     * @return the new name if its one of ours, otherwise the original name
     */
    public static String prettyName( String name ) {
        if ( name.startsWith("{") && name.endsWith("}") && name.length() > 2 ) {
            return name.substring(1, name.length() - 1);
        }
        return name;
    }

    /**
     * pretty print the acls of this document
     * @return a pretty string of the acls
     */
    public String aclsToPrettyString() {
        List<AgentAcl> acl_list = new ArrayList<>();
        acl_list.addAll(acl_set);
        Collections.sort(acl_list);
        StringBuilder sb = new StringBuilder();
        for ( AgentAcl acl : acl_list ) {
            sb.append(acl.toPrettyString()).append(".");
        }
        return sb.toString();
    }

    public UUID getOrganisation_id() {
        return organisation_id;
    }

    public void setOrganisation_id(UUID organisation_id) {
        this.organisation_id = organisation_id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public HashSet<AgentAcl> getAcl_set() {
        return acl_set;
    }

    public void setAcl_set(HashSet<AgentAcl> acl_set) {
        this.acl_set = acl_set;
    }

    public long getDate_time_uploaded() {
        return date_time_uploaded;
    }

    public void setDate_time_uploaded(long date_time_uploaded) {
        this.date_time_uploaded = date_time_uploaded;
    }

    public Map<String, String> getName_value_set() {
        return name_value_set;
    }

    public void setName_value_list(Map<String, String> name_value_set) {
        this.name_value_set = name_value_set;
    }

    @Override
    public int compareTo(AgentDocument document) {
        return url.compareTo(document.url);
    }

    public long getTs_parsed() {
        return ts_parsed;
    }

    public void setTs_parsed(long ts_parsed) {
        this.ts_parsed = ts_parsed;
    }

    public long getTs_vectorised() {
        return ts_vectorised;
    }

    public void setTs_vectorised(long ts_vectorised) {
        this.ts_vectorised = ts_vectorised;
    }

    public long getTs_summarised() {
        return ts_summarised;
    }

    public void setTs_summarised(long ts_summarised) {
        this.ts_summarised = ts_summarised;
    }

    public long getTs_indexed() {
        return ts_indexed;
    }

    public void setTs_indexed(long ts_indexed) {
        this.ts_indexed = ts_indexed;
    }

    public long getTs_clustered() {
        return ts_clustered;
    }

    public void setTs_clustered(long ts_clustered) {
        this.ts_clustered = ts_clustered;
    }

    public long getTs_entity_analysed() {
        return ts_entity_analysed;
    }

    public void setTs_entity_analysed(long ts_entity_analysed) {
        this.ts_entity_analysed = ts_entity_analysed;
    }

    public long getTs_emotion_analysed() {
        return ts_emotion_analysed;
    }

    public void setTs_emotion_analysed(long ts_emotion_analysed) {
        this.ts_emotion_analysed = ts_emotion_analysed;
    }

    public long getTs_converted() {
        return ts_converted;
    }

    public void setTs_converted(long ts_converted) {
        this.ts_converted = ts_converted;
    }

    public long getTs_knowledge_analysed() {
        return ts_knowledge_analysed;
    }

    public void setTs_knowledge_analysed(long ts_knowledge_analysed) {
        this.ts_knowledge_analysed = ts_knowledge_analysed;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }


    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public String getContent_hash() {
        return content_hash;
    }

    public void setContent_hash(String content_hash) {
        this.content_hash = content_hash;
    }

    public int getAclHash() {
        return aclHash;
    }

    public void setAclHash(int aclHash) {
        this.aclHash = aclHash;
    }

    public long getProcessingPipeline() {
        return processingPipeline;
    }

    public void setProcessingPipeline(long processingPipeline) {
        this.processingPipeline = processingPipeline;
    }
}


