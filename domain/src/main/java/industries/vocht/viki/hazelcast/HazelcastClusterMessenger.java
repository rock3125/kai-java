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

package industries.vocht.viki.hazelcast;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import industries.vocht.viki.ApplicationException;
import industries.vocht.viki.IHazelcast;
import industries.vocht.viki.hazelcast_messages.IHazelcastMessage;
import industries.vocht.viki.hazelcast_messages.IHazelcastMessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by peter on 2/06/16.
 *
 *
 */
@Component
public class HazelcastClusterMessenger implements MessageListener<IHazelcastMessage> {

    private final static Logger logger = LoggerFactory.getLogger(HazelcastClusterMessenger.class);

    @Autowired
    private IHazelcast hazelcast;

    @Autowired
    private IHazelcastMessageProcessor messageProcessor;

    // top topic of this listener
    private String topic;

    // the actual listener
    private ITopic<IHazelcastMessage> hazelcastTopic;

    // the queue for receiving objects to be checked

    public HazelcastClusterMessenger() {
    }

    public void init() throws ApplicationException {
        if ( topic == null ) {
            throw new ApplicationException("messenger topic not set");
        }
        hazelcastTopic = hazelcast.getInstance().getTopic(topic);
        hazelcastTopic.addMessageListener(this);
    }

    @Override
    public void onMessage(Message message) {
        IHazelcastMessage msg = (IHazelcastMessage)message.getMessageObject();
        if ( msg != null ) {
            messageProcessor.receive(msg);
        }
    }

    /**
     * publish a message to all nodes
     * @param message the message to publish
     */
    public void publish( IHazelcastMessage message ) {
        if ( message != null && hazelcastTopic != null ) {
            hazelcastTopic.publish(message);
        }
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

}

