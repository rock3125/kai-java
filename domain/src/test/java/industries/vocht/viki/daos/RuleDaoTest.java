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

package industries.vocht.viki.daos;

import industries.vocht.viki.EventTypeEnum;
import industries.vocht.viki.IDao;
import industries.vocht.viki.model.rules.RuleItem;
import industries.vocht.viki.model.rules.RuleItemBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.*;

/**
 * Created by peter on 20/06/16.
 *
 * test the rule dao system
 *
 */
@SuppressWarnings("ConstantConditions")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/domain/test-context.xml"})
public class RuleDaoTest {

    private final Logger logger = LoggerFactory.getLogger(RuleDaoTest.class);


    @Autowired
    private IDao dao;

    // save and re-read
    @Test
    public void testCrud1() throws IOException {
        UUID org = UUID.randomUUID();
        RuleItem ruleItem1 = createRule(org, "rule1");
        dao.getRuleDao().saveRule(org, "rule1", ruleItem1);

        RuleItem ruleItem2 = dao.getRuleDao().loadRuleByName(org, "rule1");
        Assert.notNull(ruleItem2);
        Assert.isTrue(org.equals(ruleItem2.getOrganisation_id()));
        Assert.isTrue("rule1".equals(ruleItem2.getRule_name()));
    }

    // save, read, delete, no read
    @Test
    public void testCrud2() throws IOException {
        // create and save
        UUID org = UUID.randomUUID();
        RuleItem ruleItem1 = createRule(org, "rule1");
        dao.getRuleDao().saveRule(org, "rule1", ruleItem1);

        // re-read
        RuleItem ruleItem2 = dao.getRuleDao().loadRuleByName(org, "rule1");
        Assert.notNull(ruleItem2);
        Assert.isTrue(org.equals(ruleItem2.getOrganisation_id()));
        Assert.isTrue("rule1".equals(ruleItem2.getRule_name()));

        // delete
        dao.getRuleDao().deleteRule(org, "rule1");

        // check it doesn't exist anymore
        RuleItem ruleItem3 = dao.getRuleDao().loadRuleByName(org, "rule1");
        Assert.isNull(ruleItem3);
    }

    // save many, get lists
    @Test
    public void testCrud3() throws IOException {
        // create and save
        UUID org = UUID.randomUUID();
        String[] nameList = new String[1000];
        for ( int i = 0; i < nameList.length; i++ ) {
            nameList[i] = "rule" + (i+1);
        }
        createRules(org, nameList);

        // get all rules
        List<RuleItem> ruleItemList = dao.getRuleDao().getAllRules(org);
        Assert.notNull(ruleItemList);
        Assert.isTrue(ruleItemList.size() == nameList.length);
        HashSet<String> names = new HashSet<>();
        for ( RuleItem ruleItem : ruleItemList ) {
            names.add( ruleItem.getRule_name() );
        }
        Assert.isTrue(names.size() == nameList.length);

        // check paginated get
        String prev = null;
        names = new HashSet<>();
        for ( int i = 0; i < 100; i++ ) {
            List<RuleItem> paginated = dao.getRuleDao().getRuleList(org, prev, 10);
            Assert.isTrue(paginated != null && paginated.size() == 10);
            prev = paginated.get(paginated.size() - 1).getRule_name();
            for ( RuleItem ruleItem : paginated ) {
                names.add( ruleItem.getRule_name() );
            }
        }
        Assert.isTrue(names.size() == nameList.length);
    }

    // save many, get lists of specific event types
    @Test
    public void testCrud4() throws IOException {
        // create and save
        UUID org = UUID.randomUUID();

        createRulesWithEventType(org, EventTypeEnum.Manual, "a0", "b0", "c0");
        createRulesWithEventType(org, EventTypeEnum.New_Document, "a1", "b1", "c1");
        createRulesWithEventType(org, EventTypeEnum.Schedule, "a2", "b2", "c2");
        createRulesWithEventType(org, EventTypeEnum.Interval, "a3", "b3", "c3");

        // get all rules
        List<RuleItem> ruleItemList = dao.getRuleDao().getAllRules(org);
        Assert.notNull(ruleItemList);
        Assert.isTrue(ruleItemList.size() == 12);

        // get rule sets of types
        List<RuleItem> ruleItemList2 = dao.getRuleDao().getAllOnNewDocumentRules(org);
        Assert.isTrue( ruleItemList2 != null && ruleItemList2.size() == 3);

        List<RuleItem> ruleItemList3 = dao.getRuleDao().getAllTimeEventRules(org);
        Assert.isTrue( ruleItemList3 != null && ruleItemList3.size() == 6);
    }


    ////////////////////////////////////////////////////////////////
    // helpers

    // create a new rule
    private RuleItem createRule( UUID org, String name ) {
        RuleItem ruleItem = new RuleItem();
        ruleItem.setOrganisation_id(org);
        ruleItem.setRule_name(name);
        return ruleItem;
    }

    // create a new set of rules
    private void createRules( UUID org, String ... names ) throws IOException {
        for ( String name : names ) {
            RuleItem ruleItem = new RuleItem();
            ruleItem.setOrganisation_id(org);
            ruleItem.setRule_name(name);
            dao.getRuleDao().saveRule(org, name, ruleItem);
        }
    }

    // create a new set of rules with events
    private void createRulesWithEventType(UUID org, EventTypeEnum eventType, String ... names ) throws IOException {
        for ( String name : names ) {
            RuleItem ruleItem = new RuleItem();
            ruleItem.setOrganisation_id(org);
            ruleItem.setRule_name(name);
            List<RuleItemBase> eventList = new ArrayList<>();
            RuleItemBase item = new RuleItemBase();
            item.setType(eventType.getValue());
            eventList.add(item);
            ruleItem.setEvent_list(eventList);
            dao.getRuleDao().saveRule(org, name, ruleItem);
        }
    }

}
