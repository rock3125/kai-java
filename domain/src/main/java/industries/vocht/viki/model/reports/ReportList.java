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

import java.util.List;

/**
 * Created by peter on 14/05/16.
 *
 * a list of reports
 *
 */
public class ReportList {

    private List<Report> reportList;

    public ReportList() {
    }

    public ReportList( List<Report> reportList ) {
        this.setReportList(reportList);
    }

    public List<Report> getReportList() {
        return reportList;
    }

    public void setReportList(List<Report> reportList) {
        this.reportList = reportList;
    }
}
