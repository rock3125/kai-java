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

package industries.vocht.viki.hazelcast_messages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created by peter on 13/06/16.
 *
 * simple unit test do nothing message processor
 *
 */
@Component
public class UnitTestMessageProcessor implements IHazelcastMessageProcessor {

    private final Logger logger = LoggerFactory.getLogger(UnitTestMessageProcessor.class);
    /**
     * action a hazelcast message on this node
     * @param message the message to implement
     */
    @Override
    public void receive(IHazelcastMessage message) {
        if ( message != null ) {
            if (message instanceof HMsgLoadEntities) {
                logger.info("hazelcast: message HMsgLoadEntities() ignored");
            } else if (message instanceof HMsgRemoveEntity) {
                logger.info("hazelcast: message HMsgRemoveEntity() ignored");
            } else if (message instanceof HMsgLoadEntity) {
                logger.info("hazelcast: message HMsgLoadEntity() ignored");
            } else if ( message instanceof HMsgNNetResetCaches ) {
                logger.info("hazelcast: message HMsgNNetResetCaches() ignored");
            } else if ( message instanceof HMsgSecurityUpdate ) {
                logger.info("hazelcast: message HMsgSecurityUpdate() ignored");
            } else {
                logger.error("unknown / unimplemented Hazelcast message: " + message.getClass());
            }
        }
    }

    /**
     * publish a message to all nodes
     * @param message the message
     * @throws IOException
     */
    @Override
    public void publish(IHazelcastMessage message) throws IOException {
        if ( message != null ) {
            logger.info("hazelcast: message publish ignored in unit tests");
        }
    }


}

