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

package industries.vocht.viki.cassandra_db;

/**
 * Created by peter on 12/03/16.
 * configuration settings for Cassandra
 */
public class CassandraConfiguration {
    private String server;
    private String username;
    private String password;
    private String keyspace;
    private int replicationFactor;
    private boolean canCreateCluster;
    private int timeoutRetryCount;

    public CassandraConfiguration() {
    }

    public CassandraConfiguration(String server, String keyspace, String username, String password,
                                  int replicationFactor, int timeoutRetryCount, boolean canCreateCluster) {
        this.server = server;
        this.keyspace = keyspace;
        this.username = username;
        this.password = password;
        this.replicationFactor = replicationFactor;
        this.setTimeoutRetryCount(timeoutRetryCount);
        this.setCanCreateCluster(canCreateCluster);
    }

    public String getServer() {
        return server;
    }

    /**
     * perform a health check on this configuration
     * @return null if all is well, or an error string if something is wrong
     */
    public String performHealthCheck() {
        if ( getPassword() == null || getPassword().trim().length() == 0 ) {
            return "cassandra configuration password cannot be null or empty";
        }
        if ( getUsername() == null || getUsername().trim().length() == 0 ) {
            return "cassandra configuration username cannot be null or empty";
        }
        if ( getKeyspace() == null || getKeyspace().trim().length() == 0 ) {
            return "cassandra configuration keyspace cannot be null or empty";
        }
        if ( getServer() == null || getServer().trim().length() == 0 ) {
            return "cassandra configuration server cannot be null or empty";
        }
        if ( getReplicationFactor() < 1 || getReplicationFactor() > 5 ) {
            return "cassandra configuration replication-factor value must be between one and five";
        }
        return null;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getKeyspace() {
        return keyspace;
    }

    public void setKeyspace(String keyspace) {
        this.keyspace = keyspace;
    }

    public int getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(int replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

    public boolean isCanCreateCluster() {
        return canCreateCluster;
    }

    public void setCanCreateCluster(boolean canCreateCluster) {
        this.canCreateCluster = canCreateCluster;
    }

    public int getTimeoutRetryCount() {
        return timeoutRetryCount;
    }

    public void setTimeoutRetryCount(int timeoutRetryCount) {
        this.timeoutRetryCount = timeoutRetryCount;
    }
}
