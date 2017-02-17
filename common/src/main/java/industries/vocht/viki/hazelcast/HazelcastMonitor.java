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

import industries.vocht.viki.IDao;
import industries.vocht.viki.IHazelcast;
import industries.vocht.viki.hazelcast_messages.HMsgInfrastructure;
import industries.vocht.viki.infrastructure.ClusterInfrastructure;
import industries.vocht.viki.model.cluster.ClusterAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by peter on 7/05/16.
 *
 * simple little monitor to broadcast one's presence to other
 * hazelcast nodes - and what we've got under the good in terms of
 * available services
 *
 *
 */
@Component
public class HazelcastMonitor implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(HazelcastMonitor.class);

    @Value("${hazelcast.monitor.sleep.between.tests.in.seconds:30}")
    private int sleepInSeconds;

    @Autowired
    private ClusterInfrastructure infrastructure;

    @Autowired
    private IHazelcast hazelcast;

    @Autowired
    private HazelcastClusterMessenger messenger;


    public HazelcastMonitor() {
    }

    public void init() {
        Thread thread = new Thread(this);
        thread.setName("hazelcast monitor");
        thread.start();
    }

    @Override
    public void run() {

        while ( true ) {

            int numItemsInCluster = hazelcast.getMemberCount();
            if ( numItemsInCluster > 1 ) {
                // send my infrastructure details to all nodes
                List<ClusterAddress> clusterAddressList = infrastructure.getAllExternalNodes();
                if (clusterAddressList != null && clusterAddressList.size() > 0) {
                    HMsgInfrastructure msg = new HMsgInfrastructure();
                    msg.setClusterAddressList(clusterAddressList);
                    messenger.publish(msg);
                }
            }

            try {
                Thread.sleep(sleepInSeconds * 1000);
            } catch (InterruptedException ex){
                logger.error("interrupted", ex);
                break;
            }

        } // while true

    }



}


