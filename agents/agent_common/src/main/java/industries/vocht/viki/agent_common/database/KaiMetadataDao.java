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

import industries.vocht.viki.agent_common.database.model.KaiMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 16/06/16.
 *
 * kai_metadata table
 *
 */
@Component
public class KaiMetadataDao {

    @Autowired
    private DatabaseConnection databaseConnection;

    /**
     * create the kai_metadata table if it doesn't already exist
     * @throws SQLException
     */
    public void init() throws SQLException {
        // create the table itself
        {
            String sql = "create table if not exists kai_metadata (";
            sql = sql + "\"id\" SERIAL PRIMARY KEY,";
            sql = sql + "\"file_id\" INTEGER not null REFERENCES kai_file(id),";
            sql = sql + "\"name\" TEXT not null,";
            sql = sql + "\"value\" TEXT not null";
            sql = sql + ");";

            Statement statement = databaseConnection.getConnection().createStatement();
            statement.executeUpdate(sql);
            statement.close();
        }

        // index file_id
        {
            String index1 = "create index if not exists kai_metadata_fileid_index on kai_metadata(file_id);";
            Statement statement1 = databaseConnection.getConnection().createStatement();
            statement1.executeUpdate(index1);
            statement1.close();
        }
    }

    /**
     * get a metadata list by file_id
     * @param file_id the id of the file to get metadata for
     * @throws SQLException
     */
    public List<KaiMetadata> getMetadataByFileId(int file_id) throws SQLException {
        if ( file_id > 0 ) {
            String selectStr = "select * from kai_metadata where file_id = ?;";
            PreparedStatement statement = databaseConnection.getConnection().prepareStatement(selectStr);
            statement.setInt(1, file_id);
            ResultSet rs = statement.executeQuery();
            List<KaiMetadata> metadataList = new ArrayList<>();
            while (rs.next()) {
                KaiMetadata KaiMetadata = fromResultSet(rs);
                if ( KaiMetadata != null ) {
                    metadataList.add(KaiMetadata);
                }
            }
            rs.close();
            statement.close();
            return metadataList;
        }
        return null;
    }

    /**
     * save the kai metadata into the database (update or insert determined by existing id or not)
     * @param metadata the metadata to save
     * @throws SQLException
     */
    public void saveMetadata( KaiMetadata metadata ) throws SQLException {
        if ( metadata != null ) {
            if ( metadata.getName() == null || metadata.getValue() == null || metadata.getFile_id() <= 0 ) {
                throw new SQLException("invalid kai_metadata, must have valid name, value and file_id");
            }

            // update or insert?
            boolean update = metadata.getId() > 0;
            if ( update ) {
                // simple update - change intensity and affect - all other linkages already exist
                String updateSql = "update kai_metadata set \"name\"=?, \"value\"=?, file_id=? where id=?";
                PreparedStatement statement = databaseConnection.getConnection().prepareStatement(updateSql);

                statement.setString(1, metadata.getName());
                statement.setString(2, metadata.getValue());
                statement.setInt(3, metadata.getFile_id());
                statement.setInt(4, metadata.getId());

                statement.executeUpdate();
                statement.close();

            } else {

                // insert the file
                String insertSql = "insert into kai_metadata (\"name\", \"value\", file_id) values(?,?,?);";
                PreparedStatement statement = databaseConnection.getConnection().prepareStatement(insertSql);

                statement.setString(1, metadata.getName());
                statement.setString(2, metadata.getValue());
                statement.setInt(3, metadata.getFile_id());

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
    private KaiMetadata fromResultSet( ResultSet rs ) throws SQLException {
        if ( rs != null ) {
            KaiMetadata metadata = new KaiMetadata();
            metadata.setId(rs.getInt("id"));
            metadata.setName(rs.getString("name"));
            metadata.setValue(rs.getString("value"));
            metadata.setFile_id(rs.getInt("file_id"));
            return metadata;
        }
        return null;
    }

}


