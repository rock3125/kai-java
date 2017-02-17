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

import com.datastax.driver.core.*;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.exceptions.OperationTimedOutException;
import com.datastax.driver.core.exceptions.ReadTimeoutException;
import com.datastax.driver.core.exceptions.WriteTimeoutException;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.hazelcast.spi.Operation;
import industries.vocht.viki.ApplicationException;
import industries.vocht.viki.utility.StringUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Created by peter on 6/03/2016.
 *
 * Cassandra connection manager
 *
 *
 */
public class CassandraCluster {

    private String keySpace = "viki"; // defaults
    private int replicationFactor = 1;
    private String server = "localhost";
    private int timeoutRetryCount; // how many times to retry in case of a timeout occurring

    private Session session;

    // is this the administrrative system?
    private boolean isAdminServer;

    final Logger logger = LoggerFactory.getLogger(CassandraCluster.class);

    /**
     * setup driver / construct connection to Cassandra
     *
     * @param cassandraConfiguration the server's complete configuration set
     */
    public CassandraCluster( CassandraConfiguration cassandraConfiguration )
            throws InterruptedException
    {
        this.keySpace = cassandraConfiguration.getKeyspace();
        this.replicationFactor = cassandraConfiguration.getReplicationFactor();
        this.server = cassandraConfiguration.getServer();
        this.timeoutRetryCount = cassandraConfiguration.getTimeoutRetryCount();

        logger.info("Cassandra setup: keyspace=" + this.keySpace + ", rf=" + this.replicationFactor + ", server=" + this.server);

        boolean connected; // always wait for Cassandra - vital on servers
        do {
            try {
                Cluster cluster = Cluster.builder()
                        .addContactPoint(this.server)
                        .withCredentials(cassandraConfiguration.getUsername().trim(),
                                         cassandraConfiguration.getPassword().trim())
                        .build();

                Metadata metadata = cluster.getMetadata();
                if (metadata.getKeyspace(keySpace) == null) {
                    if ( cassandraConfiguration.isCanCreateCluster() ) {
                        createKeySpace(cluster);
                    } else {
                        while (metadata.getKeyspace(keySpace) == null) {
                            logger.info("waiting 30 seconds for the cluster to be created, this is not the main cluster node.");
                            Thread.sleep(30000);
                        }
                    }
                }

                session = cluster.connect(keySpace);

                // the admin system checks all the tables
                if ( cassandraConfiguration.isCanCreateCluster() ) {
                    createTables();
                } else {
                    logger.info("skipping check/create cassandra tables, this is not the main cluster node.");
                }

                connected = true;

            } catch (NoHostAvailableException ex) {
                connected = false;
                logger.info("cassandra not running (NoHostAvailableException), waiting five seconds before re-try");
                Thread.sleep(5000);
            } catch ( ApplicationException ex ) {
                connected = false;
                logger.info(ex.getMessage());
                Thread.sleep(5000);
            }

        } while ( !connected );
        logger.info("Cassandra setup: done");
    }


    /**
     * close cassandra's session
     */
    public void close() {
        if ( session != null )
            session.close();
    }

    /**
     * create a keyspace for this cluster if dne
     * @param cluster the cluster
     */
    private void createKeySpace( Cluster cluster ) {
        if ( cluster != null ) {
            boolean hasError; // this part shouldn't/can't fail - wait for it to succeed (or user intervention)
            do {
                try {
                    if (logger.isDebugEnabled()) logger.debug("createKeySpace() if not exists (" + keySpace + ")");
                    String exec = "CREATE KEYSPACE IF NOT EXISTS " + keySpace + " WITH replication = {'class':'SimpleStrategy', 'replication_factor':" + replicationFactor + "};";
                    cluster.connect().execute(exec);
                    hasError = false;
                } catch (Exception ex) {
                    logger.error("FAILED error creating KEYSPACE " + keySpace + ", waiting 5 seconds.");
                    hasError = true;
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex2) {
                        logger.error("interrupted:" + ex2.getMessage());
                        break;
                    }
                }
            } while (hasError);
        }
    }

    /**
     * setup the tables required for Cassandra work
     */
    public void createTables() throws ApplicationException {
        logger.info("checking cassandra database tables (this may take a few minutes if this is the first time)");
        List<String> databaseCreationInstructions = parseCassandraCreationScripts();
        boolean hasError; // this part shouldn't/can't fail - wait for it to succeed (or user intervention)
        do {
            try {
                for (String item : databaseCreationInstructions) {
                    session.execute(item);
                }
                hasError = false;
            } catch (Exception ex) {
                logger.error("FAILED error creating table, waiting 5 seconds (" + ex.getMessage() + ")");
                hasError = true;
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex2) {
                    logger.error("interrupted:" + ex2.getMessage());
                    break;
                }
            }
        } while (hasError);
    }

    /**
     * read the cassandra creation scripts from resources and split
     * the scripts into single items ); wise
     * @return the set of scripts to execute
     */
    private List<String> parseCassandraCreationScripts()  throws ApplicationException {
        String[] lines = new StringUtility().loadTextFileFromResource("/server/cassandra-create-script.cql").split("\n");

        List<String> instructions = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        for ( String line : lines ) {
            String lineStr = line.trim();
            if ( lineStr.length() > 0 && !lineStr.startsWith("//") ) {
                sb.append(lineStr);
                if (lineStr.endsWith(";")) {
                    instructions.add(sb.toString());
                    sb.setLength(0);
                }
            }
        }

        return instructions;
    }

    /**
     * insert warpper
     * @param cf the column family
     * @return an insert statement
     */
    public Insert insert(String cf) {
        return QueryBuilder.insertInto(keySpace, cf);
    }

    /**
     * delete wrapper
     * @param cf the column family
     * @return the delete statement
     */
    public RegularStatement delete(String cf, HashMap<String, Object> whereSet) {
        Delete.Where delete = null;
        for ( String key : whereSet.keySet() ) {
            Object value = whereSet.get(key);
            if ( delete == null ) {
                delete = QueryBuilder.delete().from(keySpace, cf)
                        .where(QueryBuilder.eq(key, value));
            } else {
                delete = delete.and(QueryBuilder.eq(key, value));
            }
        }
        return delete;
    }

    /**
     * setup a simple select for a column family
     * @param cf the column family to select from
     * @param columns a set of columns or null
     * @return the select statement
     */
    public Row selectOne(String cf, String[] columns, HashMap<String, Object> whereSet) {

        Select select;
        if ( columns != null && columns.length > 0 ) {
            select = QueryBuilder.select(columns).from(keySpace, cf);
        } else {
            select = QueryBuilder.select().from(keySpace, cf);
        }

        Select.Where statement = null;
        for ( String key : whereSet.keySet() ) {
            Object value = whereSet.get(key);
            if ( statement == null ) {
                statement = select.where(QueryBuilder.eq(key, value));
            } else {
                statement = statement.and(QueryBuilder.eq(key, value));
            }
        }

        ResultSet set = executeWithRetry(statement);
        if (set != null ) {
            return set.one();
        }
        return null;
    }

    /**
     * execute the statement with the allowance for re-tries
     * @param statement the statement to execute
     * @return the result set or throws an exception eventually after timeoutRetryCount reaches 0
     */
    public ResultSet executeWithRetry(Statement statement ) {
        int retryCount = timeoutRetryCount;
        do {
            try {
                return session.execute(statement);
            } catch (ReadTimeoutException | WriteTimeoutException | OperationTimedOutException ex) {
                retryCount = retryCount - 1;
                if (retryCount > 0) {
                    logger.warn("read timed-out, retrying " + retryCount + " more times");
                } else {
                    throw ex;
                }
            }
        } while ( retryCount > 0 );
        return null;
    }

    /**
     * setup a simple select all items for a column family
     * @param cf the column family to select from
     * @param columns a set of columns
     * @return the select statement
     */
    public ResultSet selectAll(String cf, String[] columns, Map<String, Object> whereSet) {
        Select select;
        if ( columns != null && columns.length > 0 ) {
            select = QueryBuilder.select(columns).from(keySpace, cf);
        } else {
            select = QueryBuilder.select().from(keySpace, cf);
        }
        Select.Where statement = null;
        for ( String key : whereSet.keySet() ) {
            Object value = whereSet.get(key);
            if ( statement == null ) {
                statement = select.where(QueryBuilder.eq(key, value));
            } else {
                statement = statement.and(QueryBuilder.eq(key, value));
            }
        }
        return executeWithRetry(statement);
    }

    /**
     * perform a delete on an item in a cf
     * @param cf the cf
     * @param whereSet the conditional set
     */
    public void deleteOne(String cf, HashMap<String, Object> whereSet) {
        Delete delete = QueryBuilder.delete().from(keySpace, cf);
        Delete.Where statement = null;
        for ( String key : whereSet.keySet() ) {
            Object value = whereSet.get(key);
            if ( statement == null ) {
                statement = delete.where(QueryBuilder.eq(key, value));
            } else {
                statement = statement.and(QueryBuilder.eq(key, value));
            }
        }
        executeWithRetry(statement);
    }

    public String getServer() {
        return server;
    }

}

