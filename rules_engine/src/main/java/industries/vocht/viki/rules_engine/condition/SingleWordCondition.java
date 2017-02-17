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

import industries.vocht.viki.rules_engine.model.RuleConversionException;

/**
 * Created by peter on 29/05/16.
 *
 *
 *
 */
public class SingleWordCondition {

    private String word;
    private String filter; // empty = any
    private boolean exact;
    private String logic;

    public SingleWordCondition() {
    }

    // type, d1, d2, [logic]
    public SingleWordCondition( String csv ) throws RuleConversionException {
        String[] items = csv.split(",");
        if ( items.length == 4 ) {
            word = items[0];
            filter = items[1];
            if ( filter == null || filter.length() == 0 ) {
                filter = null;
            }
            exact = items[2] != null && items[2].equals("exact");
            logic = items[3];
            if ( logic != null && (logic.length() == 0 || logic.equals("eol")) ) {
                logic = null;
            }
        } else {
            throw new RuleConversionException("invalid csv: " + csv);
        }
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public boolean isExact() {
        return exact;
    }

    public void setExact(boolean exact) {
        this.exact = exact;
    }

    public String getLogic() {
        return logic;
    }

    public void setLogic(String logic) {
        this.logic = logic;
    }

}

