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

import industries.vocht.viki.model.cluster.ClusterAddress;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 27/07/16.
 *
 * an infrastructure confer message
 *
 */
public class HMsgInfrastructure extends IHazelcastMessage implements Serializable {

    private List<ClusterAddress> clusterAddressList;

    public HMsgInfrastructure() {
        clusterAddressList = new ArrayList<>();
    }


    public List<ClusterAddress> getClusterAddressList() {
        return clusterAddressList;
    }

    public void setClusterAddressList(List<ClusterAddress> clusterAddressList) {
        this.clusterAddressList = clusterAddressList;
    }

}

