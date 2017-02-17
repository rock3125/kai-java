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

package industries.vocht.viki.hazelcast;

import com.hazelcast.config.*;
import com.hazelcast.core.*;

import industries.vocht.viki.ApplicationException;
import industries.vocht.viki.IDao;
import industries.vocht.viki.IHazelcast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by peter on 23/04/16.
 *
 * the Hazelcast client connection system
 *
 */
@Component
public class Hazelcast implements IHazelcast {

    private final Logger logger = LoggerFactory.getLogger(Hazelcast.class);

    @Autowired(required = false)
    private HazelcastWordCountMapStore hazelcastWordStore;

    @Autowired(required = false)
    private HazelcastHashAclMapStore hazelcastAclStore;

    @Autowired(required = false)
    private HazelcastQueueStore hazelcastQueueStore;

    @Autowired
    private IDao dao;

    ////////////////////////////////////////////////////////////////
    // 7 queues for now: see enum QueueType

    @Value("${hazelcast.queue.prefix:viki.queue}")
    private String queueName;

    ////////////////////////////////////////////////////////////////

    @Value("${hazelcast.group.name:viki}")
    private String groupName;

    // words per shard
    @Value("${indexes.shard.size:100000}")
    private long indexesPerShard;

    @Value("${hazelcast.group.password:not-set}")
    private String password;

    @Value("${hazelcast.word.count.map.prefix:viki.wordcount}")
    private String wordCountMapName;

    @Value("${hazelcast.hash.acl.map.prefix:viki.aclset}")
    private String hashAclMapName;

    // a user's session security information (Access info)
    @Value("${hazelcast.user.acl.map.prefix:viki.user.aclset}")
    private String userAclMapName;

    // a user's session security information (Access info)
    @Value("${hazelcast.remote.search.map:viki.remote.search}")
    private String remoteSearchMapName;

    // how long the session will stay in the cluster
    @Value("${hazelcast.security.session.duration.in.minutes:30}")
    private int securitySessionDurationInMinutes;

    @Value("${hazelcast.port:5701}")
    private int port;

    @Value("${hazelcast.store.write.delay.in.seconds:10}")
    private int writeDelayInSeconds;

    @Value("${hazelcast.store.write.batch.size:1000}")
    private int writeBatchSize;

    @Value("${hazelcast.management.center.url:http://localhost:8180/mancenter}")
    private String managementCenterUrl;

    @Value("${hazelcast.management.center.enabled:false}")
    private boolean managementCenterEnabled;

    @Value("${hazelcast.semantic.shard.offset:1000000}")
    private int semanticShardOffset;

    // seems to work best if ONLY the ip address is used, not the port
    @Value("${hazelcast.tcp.group.csv.ip:localhost:5701}")
    private String tcpIpGroup;

    @Value("${hazelcast.tcp.group.timeout.in.seconds:10}")
    private int groupTimeoutInSeconds;

    // the hazelcast system access
    private HazelcastInstance instance;

    /**
     * void constructor
     */
    public Hazelcast() {
    }

    /**
     * initialise the maps and hazelcast's networking
     * @throws ApplicationException wrong
     */
    public void init() throws ApplicationException {

        logger.info("Hazelcast setup: group=" + groupName + ", start-port=" + port);

        if ( password.equals("not-set") ) {
            throw new ApplicationException("\"hazelcast.group.password\" not set");
        }

        instance = com.hazelcast.core.Hazelcast.newHazelcastInstance( createNewConfig() );

        logger.info("Hazelcast setup: done");
    }


    /**
     * create the configuration for all maps in Hazelcast
     * @return configuration for hazelcast
     */
    private Config createNewConfig() {
        Config config = new Config();

        // create word count map
        MapConfig wordCountMapConfig = config.getMapConfig(wordCountMapName + ".*");
        if ( hazelcastWordStore != null ) {
            MapStoreConfig wordMapStoreConfig = new MapStoreConfig();
            wordMapStoreConfig.setImplementation(hazelcastWordStore);
            wordMapStoreConfig.setWriteBatchSize(writeBatchSize);
            wordMapStoreConfig.setWriteDelaySeconds(writeDelayInSeconds);
            wordMapStoreConfig.setEnabled(true);
            wordCountMapConfig.setMapStoreConfig(wordMapStoreConfig);
        }
        wordCountMapConfig.setBackupCount( 2 );
        config.addMapConfig( wordCountMapConfig );

        // create hash acl map
        MapConfig hashAclMapConfig = config.getMapConfig(hashAclMapName + ".*");
        if ( hazelcastAclStore != null ) {
            MapStoreConfig aclMapStoreConfig = new MapStoreConfig();
            aclMapStoreConfig.setImplementation(hazelcastAclStore);
            aclMapStoreConfig.setWriteDelaySeconds(0);
            aclMapStoreConfig.setEnabled(true);
            hashAclMapConfig.setMapStoreConfig(aclMapStoreConfig);
        }
        hashAclMapConfig.setBackupCount( 2 );
        config.addMapConfig( hashAclMapConfig );

        // create queue system
        QueueConfig queueConfig = config.getQueueConfig(queueName + ".*");
        if (hazelcastQueueStore != null) {
            QueueStoreConfig queueStoreConfig = new QueueStoreConfig();
            queueStoreConfig.setStoreImplementation(hazelcastQueueStore);
            queueStoreConfig.setEnabled(true);
            queueConfig.setQueueStoreConfig(queueStoreConfig);
        }
        queueConfig.setBackupCount(2);
        config.addQueueConfig(queueConfig);

        // create user acl access map
        MapConfig userAclMapConfig = config.getMapConfig(userAclMapName + ".*");
        userAclMapConfig.setBackupCount( 2 );
        userAclMapConfig.setMaxIdleSeconds( securitySessionDurationInMinutes * 60 );
        config.addMapConfig( userAclMapConfig );

        // create remote search map
        MapConfig remoteSearchMapConfig = config.getMapConfig(remoteSearchMapName + ".*");
        remoteSearchMapConfig.setBackupCount( 1 );
        remoteSearchMapConfig.setMaxIdleSeconds( securitySessionDurationInMinutes * 60 );
        config.addMapConfig( remoteSearchMapConfig );


        NetworkConfig network = config.getNetworkConfig();

        // setup multicast to join a cluster
        JoinConfig join = network.getJoin();

        // nope - don't use these methods for comms
        join.getMulticastConfig().setEnabled(false);
        join.getAwsConfig().setEnabled(false);
        // use tcp
        join.getTcpIpConfig().setEnabled(true);

        // setup tcp ip comms instead
        logger.info("Hazelcast cluster address: " + tcpIpGroup);
        List<String> contactList = Arrays.asList(tcpIpGroup.split(","));
        join.getTcpIpConfig().setMembers(contactList);
        join.getTcpIpConfig().setConnectionTimeoutSeconds(groupTimeoutInSeconds);
        //join.getTcpIpConfig().setRequiredMember(contactList.get(0));

        config.getGroupConfig().setName(groupName);
        config.getGroupConfig().setPassword(password);

        config.getNetworkConfig().setPort( port );
        config.getNetworkConfig().setPortAutoIncrement( true );

        config.setProperty("hazelcast.phone.home.enabled", "false");
        config.setProperty( "hazelcast.logging.type", "slf4j" );

        config.getManagementCenterConfig().setEnabled(managementCenterEnabled);
        config.getManagementCenterConfig().setUrl(managementCenterUrl);

        return config;
    }

    /**
     * return the shard for a given word and - rough - not exact, and much faster
     * with a small cache attached - this cache needs to be re-consilidated at the end of an index session
     * increment the shard count for this word - don't lock - much faster.  Needs a call to flush() when done
     * @param organisation_id the organisation's id
     * @param tempCache a temporary storage map to speed things up
     * @param startValue the start values of the index offsets to account for differences only
     * @param str the word
     * @return the shard id
     */
    public int getShard( UUID organisation_id, String metadata, Map<String, Long> tempCache, Map<String, Long> startValue, String str ) {
        if ( organisation_id != null && str != null ) {
            String wordStr = "{s:" + str + "}";
            Long value = tempCache.get(wordStr);
            if ( value == null ) {
                IMap<String, Long> wordCountMap = getWordCountMap(organisation_id, metadata);
                Long value2 = wordCountMap.get(wordStr);
                if (value2 == null) {
                    startValue.put( wordStr, 0L );
                    value = 1L;
                } else {
                    startValue.put( wordStr, value2 );
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
    public void flush( UUID organisation_id, String metadata, Map<String, Long> tempCache, Map<String, Long> startValue ) {
        if ( organisation_id != null && tempCache != null ) {
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
        if ( organisation_id != null && str != null ) {
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
        if ( organisation_id != null && str != null ) {
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
        if ( organisation_id != null && str != null ) {
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
     * get the count map for a specific organisation
     * @param organisation_id the organisation
     * @return the count map
     */
    public IMap<String, Long> getWordCountMap(UUID organisation_id, String metadata) {
        return instance.getMap(wordCountMapName + "." + metadata + "." + organisation_id.toString());
    }

    /**
     * use an exclusive lock to add a value to a word for the organisation
     * @param organisation_id the organisation in question
     * @param word the word to add to
     * @param value the amount to add to word
     * @return the updated training count
     */
    public long addToWordCount( UUID organisation_id, String metadata, String word, long value ) {
        long returnValue = -1;
        if ( organisation_id != null && word != null && value != 0L ) {
            IMap<String, Long> wordCountMap = getWordCountMap(organisation_id, metadata);
            wordCountMap.lock(word);
            try {
                Long value2 = wordCountMap.get(word);
                if (value2 == null) {
                    value2 = value;
                } else {
                    value2 = value2 + value;
                }
                returnValue = value2;
                wordCountMap.set(word, value2);
            } finally {
                wordCountMap.unlock(word);
            }
        }
        return returnValue;
    }

    /**
     * get the hash acl map for a specific organisation
     * @param organisation_id the organisation
     * @return the hash acl map
     */
    public IMap<Integer, List<String>> getHashAclMap(UUID organisation_id) {
        return instance.getMap(hashAclMapName + "." + organisation_id.toString());
    }

    /**
     * get the user's hash acl map
     * @param organisation_id the organisation
     * @return the user acl map
     */
    public IMap<String, HashSet<Integer>> getUserAclMap(UUID organisation_id) {
        return instance.getMap(userAclMapName + "." + organisation_id.toString());
    }

    /**
     * @return access the one and only hazelcast instance
     */
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

    //////////////////////////////////////////////////////////////////////////////////
    // queueing system

    /**
     * add a new document action to the system's queues, checks inputs and doesn't add if parameters invalid / null
     * @param queueType the queue to add the item to
     * @param documentAction the action item to add to this queue
     */
    public void queueDocumentAction(QueueType queueType, DocumentAction documentAction) throws InterruptedException {
        if (queueType != null && documentAction != null &&
                documentAction.getUrl() != null && documentAction.getOrganisation_id() != null ) {
            IQueue<DocumentAction> documentActionQueue = instance.getQueue(queueName + "." + queueType.toString());
            documentActionQueue.put(documentAction);
        }
    }

    /**
     * take the next item from the queue, wait for timeout, return null if nothing in the queue
     * @param queueType the queue to ask for an item
     * @param timeoutInMs the amount of ms to wait for the item
     * @return null if nothing available otherwise, the next document action
     */
    public DocumentAction getNextDocumentAction(QueueType queueType, long timeoutInMs) throws InterruptedException {
        if (queueType != null ) {
            IQueue<DocumentAction> documentActionQueue = instance.getQueue(queueName + "." + queueType.toString());
            return documentActionQueue.poll(timeoutInMs, TimeUnit.MILLISECONDS);
        }
        return null;
    }



}


