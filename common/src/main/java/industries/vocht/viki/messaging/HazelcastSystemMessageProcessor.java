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

package industries.vocht.viki.messaging;

import industries.vocht.viki.IDao;
import industries.vocht.viki.hazelcast.HazelcastClusterMessenger;
import industries.vocht.viki.hazelcast_messages.*;
import industries.vocht.viki.infrastructure.ClusterInfrastructure;
import industries.vocht.viki.lexicon.Lexicon;
import industries.vocht.viki.model.knowledge_base.KBEntry;
import industries.vocht.viki.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created by peter on 2/06/16.
 *
 * implement return messages, and allow sending of new ones for the node01 micro architecture node
 *
 */
@Component
public class HazelcastSystemMessageProcessor implements IHazelcastMessageProcessor {

    private final Logger logger = LoggerFactory.getLogger(HazelcastSystemMessageProcessor.class);

    @Autowired
    private IDao dao;

    @Autowired
    private UserService userService;

    @Autowired
    private ClusterInfrastructure infrastructure;

    // the lexicon access, when available
    @Autowired
    private Lexicon lexicon;

    @Autowired
    private HazelcastClusterMessenger messenger;


    /**
     * action a hazelcast message on this node
     * @param message the message to implement
     */
    @Override
    public void receive(IHazelcastMessage message) {
        if ( message != null ) {

            if (message instanceof HMsgLoadEntities) {

                logger.info("hazelcast: message HMsgLoadEntities()");
                try {
                    lexicon.loadSemanticEntities();
                } catch (IOException ex) {
                    logger.error("loadSemanticEntities:" + ex.getMessage());
                }

            } else if (message instanceof HMsgRemoveEntity) {

                logger.info("hazelcast: message HMsgRemoveEntity()");
                HMsgRemoveEntity msg = (HMsgRemoveEntity) message;
                KBEntry entity = dao.getKBDao().getKBEntry(msg.getOrganisation_id(), "entity", msg.getEntity_id());
                if (entity != null) {
                    try {
                        lexicon.removeSemanticEntity(entity);
                    } catch (IOException ex) {
                        logger.error("loadSemanticEntities:" + ex.getMessage());
                    }
                }

            } else if (message instanceof HMsgLoadEntity) {

                logger.info("hazelcast: message HMsgLoadEntity()");
                HMsgLoadEntity msg = (HMsgLoadEntity) message;
                KBEntry entity = dao.getKBDao().getKBEntry(msg.getOrganisation_id(), "entity", msg.getEntity_id());
                if (entity != null) {
                    try {
                        lexicon.addSemanticEntity(entity);
                    } catch (IOException ex) {
                        logger.error("loadSemanticEntities:" + ex.getMessage());
                    }
                }

            } else if ( message instanceof HMsgSecurityUpdate ) {

                logger.info("hazelcast: message HMsgSecurityUpdate() master node");
                try {
                    HMsgSecurityUpdate msg = (HMsgSecurityUpdate) message;
                    userService.updateActiveSessionSecurity(msg.getOrganisation_id());
                } catch (Exception ex) {
                    logger.error("HMsgSecurityUpdate:", ex.getMessage());
                }

            } else if (message instanceof HMsgInfrastructure) {

                try {
                    HMsgInfrastructure msg = (HMsgInfrastructure) message;
                    infrastructure.updateClusterSetup(msg.getClusterAddressList());
                } catch (Exception ex) {
                    logger.error("HMsgInfrastructure:", ex.getMessage());
                }

            } else {
                logger.error("unknown / unimplemented Hazelcast message: " + message.getClass());
            }
        }
    }

    /**
     * publish a message to all nodes
     * @param message the message
     */
    @Override
    public void publish(IHazelcastMessage message) throws IOException {
        if ( message != null ) {
            messenger.publish(message);
        }
    }

}

