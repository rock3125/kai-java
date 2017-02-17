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

package industries.vocht.viki;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import industries.vocht.viki.hazelcast.DocumentAction;
import industries.vocht.viki.hazelcast.Hazelcast;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by peter on 9/05/16.
 *
 * generalised hazelcast interface for in memory use
 * and cluster use (allow for unit tests)
 *
 */
public interface IHazelcast {

    // the names / types of queues
    enum QueueType {
        Convert,
        Parse,
        Vectorize,
        Summarize,
        Index,
        Emotion,
        Thumbnail
    }

    // word (string) to count (long)
    IMap<String, Long> getWordCountMap(UUID organisation_id, String metadata);

    // acl_hash -> list of string valued acls
    IMap<Integer, List<String>> getHashAclMap(UUID organisation_id);

    // user -> set of Acl hashes this user has access to
    IMap<String, HashSet<Integer>> getUserAclMap(UUID organisation_id);

    // add value to the count for word
    long addToWordCount(UUID organisation_id, String metadata, String word, long value);

    // get and increment the hazelcast shard count for a word with a much needed cache
    int getShard(UUID organisation_id, String metadata, Map<String, Long> tempCache,
                 Map<String, Long> startValue, String str);

    // flush the cache after done with getShard()
    void flush(UUID organisation_id, String metadata, Map<String, Long> tempCache, Map<String, Long> startValue);

    // get the number of shards for a given word without modifications
    int getShardCount(UUID organisation_id, String metadata, String str);

    // get and increment the semantic shard count for a word
    int getSemanticShard(UUID organisation_id, String metadata, String str);

    // return the instance of hazelcast
    HazelcastInstance getInstance();

    // get the number of members in a cluster
    int getMemberCount();

    // queue management operations
    void queueDocumentAction(Hazelcast.QueueType queueType, DocumentAction documentAction) throws InterruptedException;

    // queue management operations
    DocumentAction getNextDocumentAction(Hazelcast.QueueType queueType, long timeoutInMs) throws InterruptedException;

}

