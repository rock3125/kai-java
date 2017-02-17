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

package industries.vocht.viki.document_orchestrator;

import com.google.common.util.concurrent.RateLimiter;
import industries.vocht.viki.ApplicationException;
import industries.vocht.viki.IHazelcast;
import industries.vocht.viki.hazelcast.DocumentAction;
import industries.vocht.viki.hazelcast.Hazelcast;
import industries.vocht.viki.infrastructure.ClusterInfrastructure;
import industries.vocht.viki.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by peter on 2/04/16.
 *
 * orchestrate document processing with other nodes
 * there are a few weaknesses that needs to be fixed, that is,
 *
 * (a) the static nature of the configuration (no dynamic nodes)
 * (b) the non-persistence of objects in progress (SAF queue?)
 *
 */
@Component
public class DocumentOrchestrator {

    private final Logger logger = LoggerFactory.getLogger(DocumentOrchestrator.class);

    // the number of threads listening for documents to process and dish them out
    @Value("${document.queue.rate.documents.per.second:10}")
    private int rateActionsPerSecond;

    @Value("${document.queue.thread.size:1}")
    private int queueThreadSize;

    @Value("${document.queue.wait.timeout.in.ms:5}")
    private long messageWaitTimeoutInMs;

    @Autowired
    private UserService userService;

    @Autowired
    private IHazelcast hazelcast;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private ClusterInfrastructure clusterInfrastructure;

    private RateLimiter sharedRateLimiter;

    // the session ids for the organisations submitting documents - their system user accounts
    private Map<UUID, String> organisationSessionID;

    public DocumentOrchestrator() {
    }

    /**
     * setup the thread
     */
    public void init() {
        // a lookup for org-id -> session of system user
        organisationSessionID = new HashMap<>();
        // setup a SHARED rate limiter for all these threads
        sharedRateLimiter = RateLimiter.create(rateActionsPerSecond);

        for (int i = 0; i < queueThreadSize; i++) {
            DocumentProcessor processor = appContext.getBean(DocumentProcessor.class);
            Thread thread = new Thread(processor);
            thread.setName("document processor " + (i + 1));
            thread.start();
        }
    }

    public RateLimiter getSharedRateLimiter() {
        return sharedRateLimiter;
    }

    /**
     * get the system user account's session for a given organisation
     * @param organisation_id the organisation in question
     * @return a valid session if it exists, null otherwise
     */
    public String getSystemUserSessionForOrganisation( UUID organisation_id ) throws ApplicationException {
        return checkLogin(organisation_id);
    }

    /**
     * offer a document for processing to the blocking systesm
     * @param queueType the type of operation to queue the item for
     * @param document the document to process
     */
    public void offer(Hazelcast.QueueType queueType, DocumentAction document) throws ApplicationException, InterruptedException {
        if ( queueType != null && document != null && document.getOrganisation_id() != null && document.getUrl() != null ) {
            // check we have a login for this entity
            checkLogin(document.getOrganisation_id());
            hazelcast.queueDocumentAction(queueType, document);
        }
    }

    /**
     * check this organisation has a valid login for its services account
     * @param organisation_id the organisation to check
     * @throws ApplicationException something is wrong with the system account
     */
    private synchronized String checkLogin( UUID organisation_id ) throws ApplicationException {
        if ( !organisationSessionID.containsKey(organisation_id) ) {
            // login the system user and create a session on their behalf
            UUID session_id = userService.loginSystemUser( organisation_id + UserService.SYSTEM_USER_EMAIL_POSTFIX, UserService.SYSTEM_IP_ADDRESS);
            if ( session_id == null ) {
                throw new ApplicationException("invalid session_id, could not login system user");
            }
            organisationSessionID.put( organisation_id, session_id.toString() );
            return session_id.toString();
        }
        return organisationSessionID.get(organisation_id);
    }


    /**
     * remove a message from the blocking queue for processing
     * @return null if none available, otherwise a message to process
     */
    public synchronized DocumentAction getNextMessageFromQueue(Hazelcast.QueueType queueType) {
        try {
            DocumentAction document = hazelcast.getNextDocumentAction(queueType, messageWaitTimeoutInMs);
            if (document != null) {
                return document;
            }
        } catch (InterruptedException ex) {
            logger.debug("DocumentOrchestrator:" + ex.getMessage());
        }
        return null; // no message to offer
    }

}





