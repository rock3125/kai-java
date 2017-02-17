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

package industries.vocht.viki.cassandra_db;

import industries.vocht.viki.IDao;
import industries.vocht.viki.IDatabase;
import industries.vocht.viki.dao.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by peter on 6/03/16.
 *
 * cassandra based data-store
 *
 */
@Component
public class CassandraDao implements IDao {

    private IDatabase cassandraDatabase;

    private DocumentDao documentDao;
    private GroupDao groupDao;
    private OrganisationDao organisationDao;
    private UserDao userDao;
    private IndexDao indexDao;
    private StatisticsDao statisticsDao;
    private ClusterDao clusterDao;
    private RuleDao ruleDao;
    private ReportDao reportDao;
    private NNetDao nnetDao;
    private TupleDao tupleDao;
    private QueueDao queueDao;
    private KBDao KBDao;

    // is the a node (like admin) that can create the cluster?
    @Value("${cassandra.server:localhost}")
    private String server;
    @Value("${cassandra.keyspace.can.create:false}")
    private boolean canCreateCluster;
    @Value("${cassandra.keyspace:viki}")
    private String keyspace;
    @Value("${cassandra.username:viki}")
    private String username;
    @Value("${cassandra.replication.factor:1}")
    private int replicationFactor;
    @Value("${cassandra.password:not-set}")
    private String password;
    @Value("${cassandra.timeout.retry.count:10}")
    private int timeoutRetryCount;
    @Value("${system.min.password.length:8}")
    private int minPasswordLength; // password minimum length restriction

    public CassandraDao() {
    }

    public void init() throws InterruptedException {
        cassandraDatabase = new CassandraDatabase(new CassandraConfiguration(server, keyspace, username, password,
                replicationFactor, timeoutRetryCount, canCreateCluster));
        documentDao = new DocumentDao(cassandraDatabase);
        groupDao = new GroupDao(cassandraDatabase);
        organisationDao = new OrganisationDao(cassandraDatabase, minPasswordLength);
        userDao = new UserDao(cassandraDatabase, minPasswordLength);
        indexDao = new IndexDao(cassandraDatabase);
        statisticsDao = new StatisticsDao(cassandraDatabase);
        clusterDao = new ClusterDao(cassandraDatabase);
        ruleDao = new RuleDao(cassandraDatabase);
        reportDao = new ReportDao(cassandraDatabase);
        nnetDao = new NNetDao(cassandraDatabase);
        tupleDao = new TupleDao(cassandraDatabase);
        queueDao = new QueueDao(cassandraDatabase);
        KBDao = new KBDao(cassandraDatabase);
    }

    public IDatabase getDatabase() { return cassandraDatabase; }

    public DocumentDao getDocumentDao() {
        return documentDao;
    }

    public GroupDao getGroupDao() {
        return groupDao;
    }

    public OrganisationDao getOrganisationDao() {
        return organisationDao;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    public IndexDao getIndexDao() {
        return indexDao;
    }

    public StatisticsDao getStatisticsDao() {
        return statisticsDao;
    }

    public ClusterDao getClusterDao() { return clusterDao; }

    public RuleDao getRuleDao() { return ruleDao; }

    public ReportDao getReportDao() { return reportDao; }

    public NNetDao getNNetDao() { return nnetDao; }

    public TupleDao getTupleDao() { return tupleDao; }

    public QueueDao getQueueDao() { return queueDao; }

    public KBDao getKBDao() { return KBDao; }

    public boolean isCanCreateCluster() {
        return canCreateCluster;
    }

    public void setCanCreateCluster(boolean canCreateCluster) {
        this.canCreateCluster = canCreateCluster;
    }
}

