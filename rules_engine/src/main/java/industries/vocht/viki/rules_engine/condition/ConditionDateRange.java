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

package industries.vocht.viki.rules_engine.condition;

import industries.vocht.viki.ApplicationException;
import industries.vocht.viki.rules_engine.model.RuleConversionException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 15/05/16.
 *
 */
public class ConditionDateRange implements ICondition {

    private String metadata_name;
    private List<SingleDateRange> date_range_list;

    public ConditionDateRange() {
        this.date_range_list = new ArrayList<>();
    }

    public ConditionDateRange( String metadata_name, String time_csv ) throws RuleConversionException {
        this.metadata_name = metadata_name;
        this.date_range_list = new ArrayList<>();
        String[] items = time_csv.split("\\|");
        for ( String item : items ) {
            this.date_range_list.add( new SingleDateRange(item) );
        }
    }

    public String getLogic() {
        String str = "";
        for ( SingleDateRange range : date_range_list ) {
            if ( range.getTime_2() == null || range.getTime_2().length() > 0 ) {
                str = str + " (" + range.getOperation() + " " + range.getTime_1();
                if ( range.getLogic() != null && range.getLogic().length() > 0 ) {
                    str = str + ") " + range.getLogic();
                } else {
                    str = str + ")";
                }
            } else {
                str = str + " (" + range.getTime_1() + " " + range.getOperation() + " " + range.getTime_2();
                if ( range.getLogic() != null && range.getLogic().length() > 0 ) {
                    str = str + ") " + range.getLogic();
                } else {
                    str = str + ")";
                }
            }
        }
        return str.trim();
    }

    public String getMetadata_name() {
        return metadata_name;
    }

    public void setMetadata_name(String metadata_name) {
        this.metadata_name = metadata_name;
    }

    public List<SingleDateRange> getDate_range_list() {
        return date_range_list;
    }

    public void setDate_range_list(List<SingleDateRange> date_range_list) {
        this.date_range_list = date_range_list;
    }

}


