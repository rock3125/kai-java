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

package industries.vocht.viki.agent_common.database;

import industries.vocht.viki.agent_common.AgentAESEncryption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Created by peter on 12/05/16.
 *
 * connect to a postgres database
 *
 */
@Component
public class DatabaseConnection {

    public static final UUID pepper = UUID.fromString("93df76f4-e86c-4436-881f-e87de03e0792");

    @Value("${postgres.db.server:localhost}")
    private String server;

    @Value("${postgres.db.port:5432}")
    private int port;

    @Value("${postgres.db.name:kai_agents}")
    private String dbName;

    @Value("${postgres.db.username:kai}")
    private String username;

    @Value("${postgres.db.password:not-set}")
    private String password;

    @Autowired
    private AgentAESEncryption aes;

    // the postgres connection object
    private Connection connection;

    public DatabaseConnection() {
    }

    public void init() throws Exception {
        if ( password.equals("not-set") ) {
            throw new IOException("postgres password not set (\"postgres.db.password\")");
        }
        if ( password.toLowerCase().startsWith("aes:") ) {
            password = aes.decrypt(password.substring(4).trim());
        }
        Class.forName("org.postgresql.Driver");
        String driverStr = String.format("jdbc:postgresql://%s:%d/%s", server, port, dbName );
        connection = DriverManager.getConnection(driverStr, username, password);
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() throws SQLException {
        connection.close();
        connection = null;
    }

}

