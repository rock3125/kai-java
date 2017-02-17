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

package industries.vocht.viki.memory_db;

import industries.vocht.viki.IDao;
import industries.vocht.viki.IDatabase;
import industries.vocht.viki.dao.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by peter on 4/03/16.
 *
 * a memory based data-store (for unit testing)
 *
 */
@Component
public class MemoryDao implements IDao {

    private IDatabase memoryDatabase;

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


    @Value("${system.min.password.length:8}")
    private int minPasswordLength; // password minimum length restriction

    public MemoryDao() {
        memoryDatabase = new MemoryDatabase();
        documentDao = new DocumentDao(memoryDatabase);
        groupDao = new GroupDao(memoryDatabase);
        organisationDao = new OrganisationDao(memoryDatabase, minPasswordLength);
        userDao = new UserDao(memoryDatabase, minPasswordLength);
        indexDao = new IndexDao(memoryDatabase);
        statisticsDao = new StatisticsDao(memoryDatabase);
        clusterDao = new ClusterDao(memoryDatabase);
        ruleDao = new RuleDao(memoryDatabase);
        reportDao = new ReportDao(memoryDatabase);
        nnetDao = new NNetDao(memoryDatabase);
        tupleDao = new TupleDao(memoryDatabase);
        queueDao = new QueueDao(memoryDatabase);
        KBDao = new KBDao(memoryDatabase);
    }

    public IDatabase getDatabase() { return memoryDatabase; }

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

    public StatisticsDao getStatisticsDao() { return statisticsDao; }

    public ClusterDao getClusterDao() { return clusterDao; }

    public RuleDao getRuleDao() { return ruleDao; }

    public ReportDao getReportDao() { return reportDao; }

    public NNetDao getNNetDao() { return nnetDao; }

    public TupleDao getTupleDao() { return tupleDao; }

    public QueueDao getQueueDao() { return queueDao; }

    public KBDao getKBDao() { return KBDao; }

}

