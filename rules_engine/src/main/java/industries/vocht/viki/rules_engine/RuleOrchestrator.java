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

import com.google.common.util.concurrent.RateLimiter;
import industries.vocht.viki.ApplicationException;
import industries.vocht.viki.rules_engine.model.OrganisationRuleName;
import industries.vocht.viki.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by peter on 15/05/16.
 *
 * the thing that runs the rules
 *
 */
@Component
public class RuleOrchestrator {

    @Autowired
    private UserService userService;

    @Autowired
    private ApplicationContext appContext;

    // the size of the blocking queue - how many messages it can hold
    @Value("${rules.blocking.queue.capacity:1000}")
    private int blockingQueueCapacity;

    // the number of threads listening for rules to process and dish them out
    @Value("${rules.queue.rate.processing.per.second:1000}")
    private int rateActionsPerSecond;

    @Value("${rules.processor.pool.size:1}")
    private int ruleProcessorPoolSize;

    // the blocking queue with rule-names waiting to be processed
    private BlockingQueue<OrganisationRuleName> ruleBlockingQueue;

    // what is in the queue effectively
    private HashSet<String> activeRuleNameSet;

    // the session ids for the organisations submitting rules - their system user accounts
    private Map<UUID, UUID> organisationSessionID;

    // the rate limiter shared by all rule executors
    private RateLimiter sharedRateLimiter;

    public RuleOrchestrator() {
    }

    /**
     * setup the thread
     */
    public void init() {
        // a lookup for org-id -> session of system user
        organisationSessionID = new HashMap<>();
        activeRuleNameSet = new HashSet<>();

        // setup a SHARED rate limiter for all these threads
        sharedRateLimiter = RateLimiter.create(rateActionsPerSecond);

        ruleBlockingQueue = new ArrayBlockingQueue<>(blockingQueueCapacity);

        for (int i = 0; i < ruleProcessorPoolSize; i++ ) {
            RuleProcessor processor = appContext.getBean(RuleProcessor.class);
            Thread thread = new Thread(processor);
            thread.setName("rule processor " + (i+1) );
            thread.start();
        }
    }

    public RateLimiter getSharedRateLimiter() {
        return sharedRateLimiter;
    }

    /**
     * offer a new message into the blocking queue - duplicate messages (messages already in the queue) are dropped
     * @param rule the message to put into the queue
     * @throws ApplicationException if queue full
     */
    public synchronized void offer( OrganisationRuleName rule ) throws ApplicationException {
        if ( rule != null && rule.getRule_name() != null && rule.getOrganisation_id() != null ) {

            // check we have a login for this entity
            getSessionForOrganisation(rule.getOrganisation_id());

            if ( ruleBlockingQueue.size() + 1 >= blockingQueueCapacity ) {
                throw new ApplicationException("RuleOrchestrator queue full.  System busy.  Please try again later.");
            }

            if ( !activeRuleNameSet.contains(rule.toString()) ) {
                activeRuleNameSet.add( rule.toString() );
                ruleBlockingQueue.offer(rule);
            }
        }
    }

    /**
     * remove a message from the blocking queue for processing
     * @return null if none available, otherwise a message to process
     */
    public synchronized OrganisationRuleName getNextMessageFromQueue() {
        OrganisationRuleName rule = ruleBlockingQueue.peek();
        if ( rule != null ) {
            rule = ruleBlockingQueue.remove();
            activeRuleNameSet.remove(rule.toString());
            return rule;
        }
        return null; // no message to offer
    }

    /**
     * check this organisation has a valid login for its services account
     * @param organisation_id the organisation to check
     * @throws ApplicationException something is wrong with the system account
     */
    public synchronized UUID getSessionForOrganisation(UUID organisation_id ) throws ApplicationException {
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

