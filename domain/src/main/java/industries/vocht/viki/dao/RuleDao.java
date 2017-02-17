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

import industries.vocht.viki.EventTypeEnum;
import industries.vocht.viki.IDatabase;
import industries.vocht.viki.model.rules.RuleItem;
import industries.vocht.viki.model.rules.RuleItemBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by peter on 12/05/16.
 *
 * rule dao
 *
 */
public class RuleDao {

    private IDatabase db;

    public RuleDao(IDatabase db) {
        this.db = db;
    }

    /**
     * write a rule to the database
     * @param organisation_id the organisation the rule belongs to
     * @param rule_name the name of the rule
     * @param item the rule data-structure item
     * @throws IOException
     */
    public void saveRule( UUID organisation_id, String rule_name, RuleItem item ) throws IOException {
        db.saveRule(organisation_id, rule_name, item);
    }

    /**
     * load a rule by name
     * @param organisation_id the organisation
     * @param rule_name the name of the rule
     * @return a rule if found, otherwise null
     * @throws IOException
     */
    public RuleItem loadRuleByName( UUID organisation_id, String rule_name ) throws IOException {
        return db.loadRuleByName(organisation_id, rule_name);
    }

    /**
     * remove and existing rule by name
     * @param organisation_id the organisation
     * @param rule_name the name of the rule
     */
    public void deleteRule( UUID organisation_id, String rule_name ) {
        db.deleteRule(organisation_id, rule_name);
    }

    /**
     * read all rules paginated
     * @param organisation_id the organisation
     * @param prevRule the previous rule-name for pagination (null initially, otherwise last rule in list's name)
     * @param pageSize the number of pages to return each time
     * @return a paginated list of rule items
     * @throws IOException
     */
    public List<RuleItem> getRuleList(UUID organisation_id, String prevRule, int pageSize) throws IOException {
        return db.getRuleList(organisation_id, prevRule, pageSize);
    }

    /**
     * return all rules for a single organisation
     * @param organisation_id the organisation to get the rules for
     * @return a list of all rules
     * @throws IOException
     */
    public List<RuleItem> getAllRules( UUID organisation_id ) throws IOException {
        List<RuleItem> ruleList = new ArrayList<>();
        // read all the rules possible
        String prev = null;
        List<RuleItem> list;
        do {
            list = getRuleList(organisation_id, prev, 100);
            if ( list != null && list.size() > 0 ) {
                ruleList.addAll( list );
                prev = list.get( list.size() - 1).getRule_name();
            } else {
                break;
            }
        } while ( list.size() == 100 );
        return ruleList;
    }


    /**
     * return all rules for a single organisation for all time events
     * @param organisation_id the organisation to get the rules for
     * @return a list of all rules
     * @throws IOException
     */
    public List<RuleItem> getAllTimeEventRules( UUID organisation_id ) throws IOException {
        List<RuleItem> ruleList = new ArrayList<>();
        // read all the rules possible
        String prev = null;
        List<RuleItem> list;
        do {
            list = getRuleList(organisation_id, prev, 100);
            if ( list != null && list.size() > 0 ) {
                ruleList.addAll(list.stream().filter(this::isTimeScheduledItem).collect(Collectors.toList()));
                prev = list.get( list.size() - 1).getRule_name();
            } else {
                break;
            }
        } while ( list.size() == 100 );
        return ruleList;
    }


    /**
     * return all rules for a single organisation for new document arrival
     * @param organisation_id the organisation to get the rules for
     * @return a list of all rules
     * @throws IOException
     */
    public List<RuleItem> getAllOnNewDocumentRules( UUID organisation_id ) throws IOException {
        List<RuleItem> ruleList = new ArrayList<>();
        // read all the rules possible
        String prev = null;
        List<RuleItem> list;
        do {
            list = getRuleList(organisation_id, prev, 100);
            if ( list != null && list.size() > 0 ) {
                ruleList.addAll(list.stream().filter(this::isRunOnNewDocumentItem).collect(Collectors.toList()));
                prev = list.get( list.size() - 1).getRule_name();
            } else {
                break;
            }
        } while ( list.size() == 100 );
        return ruleList;
    }


    // is this item a time-schedule or interval event?
    private boolean isTimeScheduledItem( RuleItem item ) {
        if ( item != null ) {
            RuleItemBase eventItem = item.getEvent();
            if ( eventItem != null ) {
                return eventItem.getType().equals(EventTypeEnum.Interval.getValue()) ||
                        eventItem.getType().equals(EventTypeEnum.Schedule.getValue());
            }
        }
        return false; // ignore
    }


    // is this item a "new arrival" event?
    private boolean isRunOnNewDocumentItem( RuleItem item ) {
        if ( item != null ) {
            RuleItemBase eventItem = item.getEvent();
            if ( eventItem != null ) {
                return eventItem.getType().equals(EventTypeEnum.New_Document.getValue());
            }
        }
        return false; // ignore
    }


}

