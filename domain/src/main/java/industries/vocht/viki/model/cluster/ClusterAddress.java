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

package industries.vocht.viki.model.cluster;

import java.io.Serializable;

/**
 * Created by peter on 29/05/16.
 *
 * a system endpoint, an ip address with port and a node-type
 *
 */
public class ClusterAddress implements Serializable {

    private KAIActionType type;
    private String host;
    private int port;

    public ClusterAddress() {
    }

    public ClusterAddress(KAIActionType type, String host, int port) {
        this.type = type;
        this.host = host;
        this.port = port;
    }

    public KAIActionType getType() {
        return type;
    }

    public void setType(KAIActionType type) {
        this.type = type;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

}
