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

package industries.vocht.viki.rules_engine;

import industries.vocht.viki.document.Document;
import industries.vocht.viki.rules_engine.condition.*;
import industries.vocht.viki.rules_engine.model.RuleConversionException;

import java.util.List;

/**
 * Created by peter on 28/05/16.
 *
 * process rule conditions
 *
 */
public class ConditionProcessor {

    public ConditionProcessor() {
    }

    /**
     * process the condition section of a rule and create a super query for it
     * @param conditionList the list of conditions to apply
     * @return a super query string that can be executed
     * @throws RuleConversionException
     */
    public String conditionListToSuperQuery( List<ICondition> conditionList ) throws RuleConversionException {
        if ( conditionList != null && conditionList.size() > 0 ) {

            String sQuery = "";
            ICondition prev = null;
            for ( ICondition condition : conditionList ) {

                if ( prev != null ) {
                    sQuery = sQuery + " " + prev.getLogic() + " ";
                }

                sQuery = sQuery + " (" + conditionToSuperQuery(condition) + ") ";

                prev = condition;
            }
            return sQuery;
        }
        return null;
    }

    /**
     * Convert a single condition to a super query
     * @param condition the condition to convert
     * @return the super query
     * @throws RuleConversionException
     */
    public String conditionToSuperQuery( ICondition condition ) throws RuleConversionException {
        if ( condition instanceof ConditionDateRange) {

            ConditionDateRange item = (ConditionDateRange)condition;
            return item.getLogic();

        } else if ( condition instanceof ConditionDuplicates) {

            throw new RuleConversionException("condition duplicates not yet implemented ");

        } else if ( condition instanceof ConditionMetadataNameContainsWord) {

            ConditionMetadataNameContainsWord item = (ConditionMetadataNameContainsWord)condition;
            return item.getLogic();

        } else if ( condition instanceof ConditionWordStats) {
            throw new RuleConversionException("condition word-stats not yet implemented ");
        } else {
            throw new RuleConversionException("unknown rule class " + condition.getClass());
        }
    }



}
