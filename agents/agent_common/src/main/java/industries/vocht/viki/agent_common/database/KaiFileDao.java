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

import industries.vocht.viki.agent_common.database.model.KaiFile;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by peter on 16/06/16.
 *
 * file object
 *
 */
@Component
public class KaiFileDao {

    @Autowired
    private DatabaseConnection databaseConnection;


    /**
     * create the kai_file table if it doesn't already exist
     * @throws SQLException
     */
    public void init() throws SQLException {
        // create the table itself
        {
            String sql = "create table if not exists kai_file (";
            sql = sql + "\"id\" SERIAL PRIMARY KEY,";
            sql = sql + "\"filename\" TEXT not null,";
            sql = sql + "\"parent_id\" INTEGER not null DEFAULT -1,";
            sql = sql + "\"hash\" TEXT not null,";
            sql = sql + "\"metadata_hash\" TEXT not null,";
            sql = sql + "\"agent_id\" INTEGER not null REFERENCES kai_agent(id),";
            sql = sql + "\"last_checked\" TIMESTAMP WITHOUT TIME ZONE not null,";
            sql = sql + "\"last_uploaded\" TIMESTAMP WITHOUT TIME ZONE not null,";
            sql = sql + "\"file_type\" VARCHAR(20) not null";
            sql = sql + ");";

            Statement statement = databaseConnection.getConnection().createStatement();
            statement.executeUpdate(sql);
            statement.close();
        }

        // index filename
        {
            String index1 = "create index if not exists kai_file_filename_index on kai_file(filename);";
            Statement statement1 = databaseConnection.getConnection().createStatement();
            statement1.executeUpdate(index1);
            statement1.close();
        }

        // index parent_id
        {
            String index1 = "create index if not exists kai_file_parentid_index on kai_file(parent_id);";
            Statement statement1 = databaseConnection.getConnection().createStatement();
            statement1.executeUpdate(index1);
            statement1.close();
        }

        // index agent_id
        {
            String index1 = "create index if not exists kai_file_agentid_index on kai_file(agent_id);";
            Statement statement1 = databaseConnection.getConnection().createStatement();
            statement1.executeUpdate(index1);
            statement1.close();
        }
    }

    /**
     * get a file from the store by filename for an agent
     * @param agent_id the agent in question
     * @param filename the filename
     * @return null if dne, otherwise the file object
     * @throws SQLException
     */
    public KaiFile getFileByFilename( int agent_id, String filename ) throws SQLException {
        if ( filename != null && agent_id > 0 ) {
            String selectStr = "select * from kai_file where filename = ? and agent_id = ?;";
            PreparedStatement statement = databaseConnection.getConnection().prepareStatement(selectStr);
            statement.setString(1, filename);
            statement.setInt(2, agent_id);
            ResultSet rs = statement.executeQuery();
            KaiFile kaiFile = null;
            if (rs.next()) {
                kaiFile = fromResultSet(rs);
            }
            rs.close();
            statement.close();
            return kaiFile;
        }
        return null;
    }

    /**
     * remove all files of a single agent
     * @param agent_id the agent whose files to rmove
     * @throws SQLException
     */
    public void removeFilesForAgent( int agent_id ) throws SQLException {
        if ( agent_id > 0 ) {
            String selectStr = "delete from kai_file where agent_id = ?;";
            PreparedStatement statement = databaseConnection.getConnection().prepareStatement(selectStr);
            statement.setInt(1, agent_id);
            statement.execute();
            statement.close();
        }
    }

    /**
     * save the kai file into the database (update or insert determined by existing id or not)
     * @param file the kai file in question
     * @return return the file's database id
     * @throws SQLException
     */
    public int saveFile( KaiFile file ) throws SQLException {
        if ( file != null ) {
            if ( file.getAgent_id() <= 0 || file.getFilename() == null ) {
                throw new SQLException("invalid kai_file, must have valid agent and filename");
            }

            // update or insert?
            boolean update = file.getId() > 0;
            if ( update ) {
                // simple update - change intensity and affect - all other linkages already exist
                String updateSql = "update kai_file set filename=?, parent_id=?, hash=?, metadata_hash=?, agent_id=?, last_checked=?, " +
                                   "last_uploaded=?, file_type=? where id=?";
                PreparedStatement statement = databaseConnection.getConnection().prepareStatement(updateSql);

                statement.setString(1, file.getFilename());
                statement.setInt(2, file.getParent_id());
                statement.setString(3, file.getHash());
                statement.setString(4, file.getMetadata_hash());
                statement.setInt(5, file.getAgent_id());
                statement.setDate(6, new java.sql.Date(file.getLast_checked().toDate().getTime()) );
                statement.setDate(7, new java.sql.Date(file.getLast_uploaded().toDate().getTime()) );
                statement.setString(8, file.getFile_type());
                statement.setInt(9, file.getId());

                statement.executeUpdate();
                statement.close();

                return file.getId();

            } else {

                // insert the file
                String insertSql = "insert into kai_file (filename, parent_id, hash, metadata_hash, agent_id, last_checked, last_uploaded, file_type) values(?,?,?,?,?,?,?,?);";
                PreparedStatement statement = databaseConnection.getConnection().prepareStatement(insertSql);

                statement.setString(1, file.getFilename());
                statement.setInt(2, file.getParent_id());
                statement.setString(3, file.getHash());
                statement.setString(4, file.getMetadata_hash());
                statement.setInt(5, file.getAgent_id());
                statement.setDate(6, new java.sql.Date(file.getLast_checked().toDate().getTime()) );
                statement.setDate(7, new java.sql.Date(file.getLast_uploaded().toDate().getTime()) );
                statement.setString(8, file.getFile_type());

                statement.executeUpdate();
                statement.close();

                // get its id
                int id = -1;
                PreparedStatement statement2 = databaseConnection.getConnection().prepareStatement("select id from kai_file where filename=?");
                statement2.setString(1, file.getFilename());
                ResultSet rs = statement2.executeQuery();
                if (rs.next()) {
                    id = rs.getInt(1);
                }
                rs.close();
                statement2.close();

                return id;
            }

        }
        return -1;
    }


    /**
     * turn a result item into a file
     * @param rs the result set
     * @return the kai file item
     */
    private KaiFile fromResultSet( ResultSet rs ) throws SQLException {
        if ( rs != null ) {
            KaiFile file = new KaiFile();
            file.setId( rs.getInt("id") );
            file.setFilename( rs.getString("filename") );
            file.setParent_id( rs.getInt("parent_id") );
            file.setHash( rs.getString("hash") );
            file.setMetadata_hash( rs.getString("metadata_hash") );
            file.setAgent_id( rs.getInt("agent_id") );
            file.setLast_checked( new DateTime(rs.getDate("last_checked")) );
            file.setLast_uploaded( new DateTime(rs.getDate("last_uploaded")) );
            file.setFile_type( rs.getString("file_type") );
            return file;
        }
        return null;
    }


}

