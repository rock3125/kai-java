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

import industries.vocht.viki.agent_common.database.model.KaiAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by peter on 16/06/16.
 *
 * kai_agent table
 *
 */
@Component
public class KaiAgentDao {

    @Autowired
    private DatabaseConnection databaseConnection;

    /**
     * create the kai_metadata table if it doesn't already exist
     * @throws SQLException
     */
    public void init() throws SQLException {
        // create the table itself
        {
            String sql = "create table if not exists kai_agent (";
            sql = sql + "\"id\" SERIAL PRIMARY KEY,";
            sql = sql + "\"name\" TEXT not null,";
            sql = sql + "\"username\" TEXT not null,";
            sql = sql + "\"password\" TEXT not null,";
            sql = sql + "\"domain\" TEXT ,";
            sql = sql + "\"server\" TEXT ,";
            sql = sql + "\"path\" TEXT ,";
            sql = sql + "\"schedule\" TEXT not null,";
            sql = sql + "\"files_per_second\" INTEGER not null CHECK (files_per_second > 0),";
            sql = sql + "\"remote_token\" TEXT ,";
            sql = sql + "\"kai_username\" TEXT not null,";
            sql = sql + "\"kai_password\" TEXT not null,";
            sql = sql + "\"kai_login_server\" TEXT not null,";
            sql = sql + "\"kai_login_port\" INTEGER not null,";
            sql = sql + "\"kai_document_server\" TEXT not null,";
            sql = sql + "\"kai_document_port\" INTEGER not null";
            sql = sql + ");";

            Statement statement = databaseConnection.getConnection().createStatement();
            statement.executeUpdate(sql);
            statement.close();
        }
    }


    /**
     * get an agent from the store by name
     * @param name the name of the agent
     * @return null if dne, otherwise the agent object
     * @throws SQLException
     */
    public KaiAgent getAgentByName(String name ) throws SQLException {
        if ( name != null ) {
            String selectStr = "select * from kai_agent where name = ?;";
            PreparedStatement statement = databaseConnection.getConnection().prepareStatement(selectStr);
            statement.setString(1, name);
            ResultSet rs = statement.executeQuery();
            KaiAgent kaiAgent = null;
            if (rs.next()) {
                kaiAgent = fromResultSet(rs);
            }
            rs.close();
            statement.close();
            return kaiAgent;
        }
        return null;
    }

    /**
     * save the kai agent into the database (update or insert determined by existing id or not)
     * @param agent the kai agent to save
     * @throws SQLException
     */
    public void saveAgent( KaiAgent agent ) throws SQLException {
        if ( agent != null ) {
            if ( agent.getName() == null ) {
                throw new SQLException("invalid kai_agent, must have valid name");
            }

            // update or insert?
            boolean update = agent.getId() > 0;
            if ( update ) {
                // simple update - change intensity and affect - all other linkages already exist
                String updateSql = "update kai_agent set \"name\"=?, username=?, password=?, \"domain\"=?, server=?, path=?, schedule=?, " +
                                   "files_per_second=?, remote_token=?, kai_username=?, kai_password=?, " +
                                   "kai_login_server=?, kai_login_port=?, kai_document_server=?, kai_document_port=? where id=?";
                PreparedStatement statement = databaseConnection.getConnection().prepareStatement(updateSql);

                statement.setString(1, agent.getName());
                statement.setString(2, agent.getUsername());
                statement.setString(3, agent.getPassword());
                statement.setString(4, agent.getDomain());
                statement.setString(5, agent.getServer());
                statement.setString(6, agent.getPath());
                statement.setString(7, agent.getSchedule());
                statement.setInt(8, agent.getFiles_per_second() );
                statement.setString(9, agent.getRemote_token());
                statement.setString(10, agent.getKai_username());
                statement.setString(11, agent.getKai_password());
                statement.setString(12, agent.getKai_login_server());
                statement.setInt(13, agent.getKai_login_port());
                statement.setString(14, agent.getKai_document_server());
                statement.setInt(15, agent.getKai_document_port());
                statement.setInt(16, agent.getId());

                statement.executeUpdate();
                statement.close();

            } else {

                // insert the file
                String insertSql = "insert into kai_agent (\"name\", username, password, \"domain\", server, path, schedule, files_per_second, remote_token, " +
                                   "kai_username, kai_password, kai_login_server, kai_login_port, kai_document_server, kai_document_port) " +
                                   "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
                PreparedStatement statement = databaseConnection.getConnection().prepareStatement(insertSql);

                statement.setString(1, agent.getName());
                statement.setString(2, agent.getUsername());
                statement.setString(3, agent.getPassword());
                statement.setString(4, agent.getDomain());
                statement.setString(5, agent.getServer());
                statement.setString(6, agent.getPath());
                statement.setString(7, agent.getSchedule());
                statement.setInt(8, agent.getFiles_per_second() );
                statement.setString(9, agent.getRemote_token());
                statement.setString(10, agent.getKai_username());
                statement.setString(11, agent.getKai_password());
                statement.setString(12, agent.getKai_login_server());
                statement.setInt(13, agent.getKai_login_port());
                statement.setString(14, agent.getKai_document_server());
                statement.setInt(15, agent.getKai_document_port());

                statement.executeUpdate();
                statement.close();
            }

        } // if agent != null
    }


    /**
     * turn a result item into a file
     * @param rs the result set
     * @return the kai file item
     */
    private KaiAgent fromResultSet( ResultSet rs ) throws SQLException {
        if ( rs != null ) {
            KaiAgent agent = new KaiAgent();
            agent.setId(rs.getInt(1));
            agent.setName(rs.getString(2));
            agent.setUsername(rs.getString(3));
            agent.setPassword(rs.getString(4));
            agent.setDomain(rs.getString(5));
            agent.setServer(rs.getString(6));
            agent.setPath(rs.getString(7));
            agent.setSchedule(rs.getString(8));
            agent.setFiles_per_second(rs.getInt(9));
            agent.setRemote_token(rs.getString(10));
            agent.setKai_username(rs.getString(11));
            agent.setKai_password(rs.getString(12));
            agent.setKai_login_server(rs.getString(13));
            agent.setKai_login_port(rs.getInt(14));
            agent.setKai_document_server(rs.getString(15));
            agent.setKai_document_port(rs.getInt(16));
            return agent;
        }
        return null;
    }


}

