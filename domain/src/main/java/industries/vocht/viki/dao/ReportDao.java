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

package industries.vocht.viki.dao;

import industries.vocht.viki.IDatabase;
import industries.vocht.viki.model.reports.Report;

import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 28/04/16.
 *
 * report related data access
 *
 */
public class ReportDao {

    private IDatabase db;

    public ReportDao(IDatabase db) {
        this.db = db;
    }


    public Report createReport(UUID organisation_id, Report report) {
        return db.createReport(organisation_id, report);
    }

    public Report readReport(UUID organisation_id, String report_name) {
        return db.readReport(organisation_id, report_name);
    }

    public List<Report> readReportList(UUID organisation_id) {
        return db.readReportList(organisation_id);
    }


}



