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

/**
 * Created by peter on 29/05/16.
 *
 */
public class SingleDateRange {

    private String operation; // before, after, between, or exact
    private String time_1;
    private String time_2;
    private String logic;

    public SingleDateRange() {
    }

    // type, d1, d2, [logic]
    public SingleDateRange( String csv ) throws RuleConversionException {
        String[] items = csv.split(",");
        if ( items.length == 2 ) {
            operation = items[0];
            time_1 = items[1];
            time_2 = null;
        } else if ( items.length == 3 ) {
            operation = items[0];
            time_1 = items[1];
            time_2 = items[2];
            if ( time_2 != null && time_2.length() == 0 ) {
                time_2 = null;
            }
        } else if ( items.length == 4 ) {
            operation = items[0];
            time_1 = items[1];
            time_2 = items[2];
            if ( time_2 != null && time_2.length() == 0 ) {
                time_2 = null;
            }
            logic = items[3];
        } else {
            throw new RuleConversionException("invalid csv: " + csv);
        }
    }

    public SingleDateRange( String operation, String time_1, String time_2, String logic ) {
        this.operation = operation;
        this.time_1 = time_1;
        this.time_2 = time_2;
        this.logic = logic;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getTime_1() {
        return time_1;
    }

    public void setTime_1(String time_1) {
        this.time_1 = time_1;
    }

    public String getTime_2() {
        return time_2;
    }

    public void setTime_2(String time_2) {
        this.time_2 = time_2;
    }

    public String getLogic() {
        return logic;
    }

    public void setLogic(String logic) {
        this.logic = logic;
    }


}
