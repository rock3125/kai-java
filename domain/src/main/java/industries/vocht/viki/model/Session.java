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

package industries.vocht.viki.model;

import java.util.UUID;

/**
 * Created by peter on 4/03/16.
 *
 */
public class Session {

    // 30 minute session timeout
    public final static long SESSION_TIMEOUT_IN_MS = 30 * 60 * 1000;

    private UUID id;
    private String email;
    private String ip_address;
    private long last_access;

    private String prettyDate; // for UI display

    public Session() {
    }

    public Session(UUID id, String email, String ip_address, long last_access ) {
        this.id = id;
        this.email = email;
        this.ip_address = ip_address;
        this.last_access = last_access;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public long getLast_access() {
        return last_access;
    }

    public void setLast_access(long last_access) {
        this.last_access = last_access;
    }

    public String getIp_address() {
        return ip_address;
    }

    public void setIp_address(String ip_address) {
        this.ip_address = ip_address;
    }

    public String getPrettyDate() {
        return prettyDate;
    }

    public void setPrettyDate(String prettyDate) {
        this.prettyDate = prettyDate;
    }
}

