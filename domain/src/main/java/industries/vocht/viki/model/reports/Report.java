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

package industries.vocht.viki.model.reports;

import java.util.UUID;

/**
 * Created by peter on 14/05/16.
 *
 * a report descriptor
 *
 */
public class Report {

    private UUID organisation_id;
    private String report_name;
    private String creator;
    private int report_id;
    private long last_run;

    public Report() {
    }

    public Report( UUID organisation_id, String report_name, String creator, int report_id, long last_run ) {
        this.organisation_id = organisation_id;
        this.report_name = report_name;
        this.creator = creator;
        this.report_id = report_id;
        this.last_run = last_run;
    }

    public String getReport_name() {
        return report_name;
    }

    public void setReport_name(String report_name) {
        this.report_name = report_name;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public int getReport_id() {
        return report_id;
    }

    public void setReport_id(int report_id) {
        this.report_id = report_id;
    }

    public long getLast_run() {
        return last_run;
    }

    public void setLast_run(long last_run) {
        this.last_run = last_run;
    }

    public UUID getOrganisation_id() {
        return organisation_id;
    }

    public void setOrganisation_id(UUID organisation_id) {
        this.organisation_id = organisation_id;
    }
}
