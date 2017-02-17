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

import industries.vocht.viki.ApplicationException;
import industries.vocht.viki.IDao;
import industries.vocht.viki.client.RuleClientInterface;
import industries.vocht.viki.model.cluster.KAIActionType;
import industries.vocht.viki.infrastructure.ClusterInfrastructure;
import industries.vocht.viki.model.Organisation;
import industries.vocht.viki.model.rules.RuleItem;
import industries.vocht.viki.rules_engine.model.ExecutableRule;
import industries.vocht.viki.rules_engine.model.RuleConversionException;
import industries.vocht.viki.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * Created by peter on 15/05/16.
 *
 * take the rules that need scheduling - refresh them periodically
 * and fire the ones that need running now through the client interfaces
 *
 * !!! NB. there should only be ONE RuleTimeScheduler for the entire cluster!!!
 * todo: clean this up, use hazelcast to coordinate rule-time schedulers
 *
 */
@Component
public class RuleTimeScheduler implements Runnable {

    private Logger logger = LoggerFactory.getLogger(RuleTimeScheduler.class);

    @Autowired
    private IDao dao;

    @Autowired
    private ClusterInfrastructure clusterInfrastructure;

    @Autowired
    private UserService userService;

    @Value("${rule.time.scheduler.wait.in.minutes:5}")
    private int waitInMinutes;

    // the session ids for the organisations submitting rules - their system user accounts
    private Map<UUID, UUID> organisationSessionID;

    private HashMap<String, ExecutableRule> timeBasedRuleSet;

    public RuleTimeScheduler() {
        timeBasedRuleSet = new HashMap<>();
    }


    // setup client connection points and start running as a thread
    public void init() throws ApplicationException {
        organisationSessionID = new HashMap<>();
        Thread thread = new Thread(this);
        thread.setName("rule-time scheduler");
        thread.start();
    }


    @Override
    public void run() {

        do {

            try {

                // re-get stuff - keep the old
                reGetData();

                for ( String ruleName : timeBasedRuleSet.keySet() ) {
                    ExecutableRule rule = timeBasedRuleSet.get(ruleName);
                    if ( rule.canExecuteOnInterval() || rule.canExecuteOnSchedule() ) {

                        RuleClientInterface clientInterface = (RuleClientInterface)clusterInfrastructure.getNextClientRoundRobin(KAIActionType.Rule);
                        clientInterface.executeRuleByName( getSessionForOrganisation( rule.getOrganisation_id()), ruleName );
                    }
                }

            } catch (Exception ex) {
                logger.error("RuleTimeScheduler", ex);
            }


            try {
                Thread.sleep(waitInMinutes * 60_0000);
            } catch ( InterruptedException ex ) {
                break;
            }

        } while ( true );
    }

    /**
     * convert a list of rules to other rules
     */
    private void reGetData() throws IOException {
        List<RuleItem> list = getTimebasedRuleSet();
        if ( list != null ) {
            HashMap<String, ExecutableRule> set = new HashMap<>();
            for ( RuleItem item : list ) {
                try {
                    ExecutableRule rule = new ConvertFromRuleItem().convert(item);
                    if ( rule != null && rule.getRule_name() != null ) {

                        // setup interval time
                        if ( rule.isIntervalEvent() ) {
                            rule.setTimeStart(System.currentTimeMillis());
                        } else {
                            rule.setTimeStart(0L); // scheduled time
                        }

                        if ( timeBasedRuleSet.containsKey(rule.getRule_name()) ) {
                            set.put( rule.getRule_name(), timeBasedRuleSet.get(rule.getRule_name()) );
                        } else {
                            set.put( rule.getRule_name(), rule );
                        }

                    }
                    timeBasedRuleSet = set;

                } catch (RuleConversionException ex) {
                    logger.error("convert", ex);
                }
            }
        }
    }

    /**
     * get the time based rules to process periodically
     * @return the set if it exists or an empty array
     * @throws IOException
     */
    private List<RuleItem> getTimebasedRuleSet() throws IOException {
        List<RuleItem> timeBasedRuleList = new ArrayList<>();
        List<Organisation> organisationList = dao.getOrganisationDao().getOrganisationList();
        if ( organisationList != null ) {

            // for each organisation
            for ( Organisation organisation : organisationList ) {

                UUID organisation_id = organisation.getId();
                List<RuleItem> ruleItemList = dao.getRuleDao().getAllTimeEventRules(organisation_id);
                if ( ruleItemList != null ) {
                    timeBasedRuleList.addAll(ruleItemList);
                }

            } // for each organisation

        } // of has organisations

        return timeBasedRuleList;
    }


    /**
     * check this organisation has a valid login for its services account
     * @param organisation_id the organisation to check
     * @throws ApplicationException something is wrong with the system account
     */
    private synchronized UUID getSessionForOrganisation( UUID organisation_id ) throws ApplicationException {
        if ( !organisationSessionID.containsKey(organisation_id) ) {
            // login the system user and create a session on their behalf
            UUID session_id = userService.loginSystemUser( organisation_id + UserService.SYSTEM_USER_EMAIL_POSTFIX, UserService.SYSTEM_IP_ADDRESS);
            if ( session_id == null ) {
                throw new ApplicationException("invalid session_id, could not login system user");
            }
            organisationSessionID.put( organisation_id, session_id );
        }
        return organisationSessionID.get(organisation_id);
    }


}

