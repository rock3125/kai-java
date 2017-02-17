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

import industries.vocht.viki.dao.*;

/**
 * Created by peter on 4/03/16.
 *
 */
public interface IDao {
    DocumentDao getDocumentDao();
    GroupDao getGroupDao();
    OrganisationDao getOrganisationDao();
    UserDao getUserDao();
    IndexDao getIndexDao();
    StatisticsDao getStatisticsDao();
    QueueDao getQueueDao();
    ClusterDao getClusterDao();
    RuleDao getRuleDao();
    ReportDao getReportDao();
    NNetDao getNNetDao();
    TupleDao getTupleDao();
    KBDao getKBDao();

    // only for creation purposes - not to be used for anything else
    IDatabase getDatabase();
}

