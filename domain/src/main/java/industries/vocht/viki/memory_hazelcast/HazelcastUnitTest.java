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

package industries.vocht.viki.memory_hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.*;
import industries.vocht.viki.IHazelcast;
import industries.vocht.viki.hazelcast.DocumentAction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by peter on 9/05/16.
 *
 * hazelcast in memory / unit test version
 *
 */
@Component
public class HazelcastUnitTest implements IHazelcast {

    private HazelcastInstance instance;

    @Value("${hazelcast.semantic.shard.offset:1000000}")
    private int semanticShardOffset;

    // words per shard
    @Value("${indexes.shard.size:100000}")
    private long indexesPerShard;


    public HazelcastUnitTest() {
    }

    public void init() {
        Config config = new Config();
        config.setProperty("hazelcast.shutdownhook.enabled", "false");
        config.setProperty("hazelcast.phone.home.enabled", "false");
        config.setProperty( "hazelcast.logging.type", "slf4j" );
        NetworkConfig network = config.getNetworkConfig();
        network.getJoin().getTcpIpConfig().setEnabled(false);
        network.getJoin().getMulticastConfig().setEnabled(false);
        instance = Hazelcast.newHazelcastInstance(config);
    }

    @Override
    public IMap<String, Long> getWordCountMap(UUID organisation_id, String metadata) {
        return instance.getMap("word-count." + metadata + "." + organisation_id.toString());
    }

    @Override
    public IMap<Integer, List<String>> getHashAclMap(UUID organisation_id) {
        return instance.getMap("acls." + organisation_id.toString());
    }

    @Override
    public IMap<String, HashSet<Integer>> getUserAclMap(UUID organisation_id) {
        return instance.getMap("user.acls." + organisation_id.toString());
    }

    @Override
    public long addToWordCount( UUID organisation_id, String metadata, String word, long value ) {
        long result = value;
        IMap<String, Long> map = instance.getMap("word-count." + metadata + "." + organisation_id.toString());
        map.lock(word);
        try {
            Long v = map.get(word);
            if ( v == null ) {
                v = value;
            } else {
                v = v + value;
                result = v;
            }
            map.put(word, v);
        } finally {
            map.unlock(word);
        }
        return result;
    }

    /**
     * return the shard for a given word and - rough - not exact, and much faster
     * with a small cache attached - this cache needs to be re-consilidated at the end of an index session
     * increment the shard count for this word - don't lock - much faster.  Needs a call to flush() when done
     * @param organisation_id the organisation's id
     * @param tempCache a temporary storage map to speed things up
     * @param str the word
     * @return the shard id
     */
    public int getShard(UUID organisation_id, String metadata, Map<String, Long> tempCache,
                        Map<String, Long> startValue, String str ) {
        if ( organisation_id != null && str != null && metadata != null ) {
            String wordStr = "{s:" + str + "}";
            Long value = tempCache.get(wordStr);
            if ( value == null ) {
                IMap<String, Long> wordCountMap = getWordCountMap(organisation_id, metadata);
                Long value2 = wordCountMap.get(wordStr);
                if (value2 == null) {
                    startValue.put( wordStr, 0L);
                    value = 1L;
                } else {
                    startValue.put( wordStr, value2);
                    value = value2 + 1L;
                }
                tempCache.put( wordStr, value );
            } else {
                value = value + 1L;
                tempCache.put( wordStr, value );
            }
            return (int)(value / indexesPerShard);
        }
        return 0;
    }

    /**
     * flush a previously constructed index cache back over Hazelcast
     * @param organisation_id the organisation's id
     * @param tempCache a temporary storage map to speed things up
     */
    public void flush(UUID organisation_id, String metadata, Map<String, Long> tempCache, Map<String, Long> startValue ) {
        if ( organisation_id != null && tempCache != null && metadata != null ) {
            IMap<String, Long> wordCountMap = getWordCountMap(organisation_id, metadata);
            for ( String key : tempCache.keySet() ) {
                long value = tempCache.get(key) - startValue.get(key);
                wordCountMap.lock(key);
                try {
                    Long value2 = wordCountMap.get(key);
                    if ( value2 == null ) {
                        value2 = value;
                    } else {
                        value2 = value2 + value;
                    }
                    wordCountMap.set(key, value2);
                } finally {
                    wordCountMap.unlock(key);
                }
            } // for each key
        } // if valid parameters
    }

    /**
     * return the actual count of a shard without changing it
     * @param organisation_id the organisation's id
     * @param str the word to get the number of shards for
     * @return  the number of shards for a word
     */
    public int getShardCount( UUID organisation_id, String metadata, String str ) {
        if ( organisation_id != null && str != null && metadata != null ) {
            String wordStr = "{s:" + str + "}";
            IMap<String, Long> wordCountMap = getWordCountMap(organisation_id, metadata);
            Long value = wordCountMap.get(wordStr);
            if ( value == null ) {
                value = 0L;
            }
            return (int)(value / indexesPerShard) + 1;
        }
        return 1; // all words have at least one shard
    }

    /**
     * return the shard for a given word in the semantic system
     * increment the shard count for this word
     * @param organisation_id the organisation's id
     * @param str the word
     * @return the shard id, starting at semantic-shard-offset
     */
    public int getSemanticShard( UUID organisation_id, String metadata, String str ) {
        if ( organisation_id != null && str != null && metadata != null ) {
            String wordStr = "{se:" + str + "}";
            IMap<String, Long> wordCountMap = getWordCountMap(organisation_id, metadata);
            Long value = wordCountMap.get(wordStr);
            if ( value == null ) {
                value = 0L;
            }
            addToWordCount(organisation_id, metadata, wordStr, 1);
            return semanticShardOffset + (int)(value / indexesPerShard);
        }
        return semanticShardOffset;
    }

    /**
     * return the actual count of a semantic shard without changing it
     * @param organisation_id the organisation's id
     * @param str the word to get the number of shards for
     * @return  the number of shards for a word
     */
    public int getSemanticShardCount( UUID organisation_id, String metadata, String str ) {
        if ( organisation_id != null && str != null && metadata != null ) {
            String wordStr = "{se:" + str + "}";
            IMap<String, Long> wordCountMap = getWordCountMap(organisation_id, metadata);
            Long value = wordCountMap.get(wordStr);
            if ( value == null ) {
                value = 0L;
            }
            return (int)(value / indexesPerShard) + 1;
        }
        return 1; // all words have at least one shard
    }

    /**
     * @return the hazelcast instance for this set
     */
    @Override
    public HazelcastInstance getInstance() {
        return instance;
    }

    /**
     * @return how many members are connected to the cluster, the first
     * item is of course yourself
     */
    public int getMemberCount() {
        if ( instance != null ) {
            Cluster cluster = instance.getCluster();
            if ( cluster != null ) {
                Set<Member> memberSet = cluster.getMembers();
                if ( memberSet != null ) {
                    return memberSet.size();
                }
            }
            return 1;
        }
        return 0;
    }

    @Override
    public void queueDocumentAction(QueueType queueType, DocumentAction documentAction) throws InterruptedException {
        instance.getQueue("queue." + queueType.toString()).put(documentAction);
    }

    @Override
    public DocumentAction getNextDocumentAction(QueueType queueType, long timeoutInMs) throws InterruptedException {
        IQueue<DocumentAction> documentActionQueue = instance.getQueue("queue." + queueType.toString());
        return documentActionQueue.poll(timeoutInMs, TimeUnit.MILLISECONDS);
    }

}


