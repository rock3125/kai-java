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

import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.datastax.driver.core.querybuilder.Batch;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import industries.vocht.viki.ApplicationException;
import industries.vocht.viki.IDatabase;
import industries.vocht.viki.datastructures.IntList;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.hazelcast.DocumentAction;
import industries.vocht.viki.model.*;
import industries.vocht.viki.model.knowledge_base.KBEntry;
import industries.vocht.viki.model.emotions.EmotionalItem;
import industries.vocht.viki.model.emotions.EmotionalSet;
import industries.vocht.viki.model.group.Group;
import industries.vocht.viki.model.indexes.Index;
import industries.vocht.viki.model.indexes.TimeIndex;
import industries.vocht.viki.model.k_means.kMeansCluster;
import industries.vocht.viki.model.k_means.kMeansValue;
import industries.vocht.viki.model.nnet.NNetModelData;
import industries.vocht.viki.model.nnet.NNetTrainingSample;
import industries.vocht.viki.model.reports.Report;
import industries.vocht.viki.model.rules.RuleItem;
import industries.vocht.viki.model.semantics.Tuple;
import industries.vocht.viki.model.similar.SimilarDocument;
import industries.vocht.viki.model.similar.SimilarDocumentSet;
import industries.vocht.viki.model.summary.SummarisationSet;
import industries.vocht.viki.model.user.User;
import industries.vocht.viki.model.user.UserEmailList;
import industries.vocht.viki.utility.BinaryBlob;
import industries.vocht.viki.utility.Sha256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Created by peter on 6/03/16.
 *
 */
public class CassandraDatabase implements IDatabase {

    public static final int MAX_BLOB_SIZE_IN_BYTES = 1024 * 1000; // 1MB blob sizes

    private static final Logger logger = LoggerFactory.getLogger(CassandraDatabase.class);
    private CassandraCluster cluster;

    // index batch processing
    private List<RegularStatement> cassandraBatch;

    public CassandraDatabase(CassandraConfiguration cassandraConfiguration)
            throws InterruptedException {
        cluster = new CassandraCluster(cassandraConfiguration);
        cassandraBatch = new ArrayList<>();
    }

    /////////////////////////////////////////////////////////////////////////////////////////

    /**
     * helper for createUser and updateUser
     *
     * @param user the user object to update
     */
    private void insertUser(User user) {
        // id uuid, email text, first_name text, surname text, organisation_id uuid, salt uuid,
        // password_sha256 text, confirmed boolean
        Statement insert = cluster.insert("user")
                .values(new String[]{
                                "id", "email", "first_name", "surname", "organisation_id", "salt",
                                "password_sha256", "confirmed", "system_user"},
                        new Object[]{
                                user.getId(), user.getEmail(), user.getFirst_name(), user.getSurname(), user.getOrganisation_id(),
                                user.getSalt(), user.getPassword_sha256(), user.isConfirmed(), user.isSystem_user()
                        });
        cluster.executeWithRetry(insert);
    }

    @Override
    public User createUser(UUID organisation_id, User user, String userPassword)
            throws ApplicationException, NoSuchAlgorithmException, UnsupportedEncodingException {
        // setup the user
        user.setId(UUID.randomUUID());
        user.setOrganisation_id(organisation_id);
        user.setSalt(UUID.randomUUID());
        user.setPassword_sha256(new Sha256().generateSha256Password(user.getSalt(), userPassword));
        insertUser(user);

        // create activation code for this user
        createAccountActivation(user.getEmail());
        return user;
    }

    @Override
    public User readUser(String email) {
        HashMap<String, Object> whereSet = new HashMap<>();
        whereSet.put("email", email);
        Row row = cluster.selectOne("user",
                new String[]{"id", "email", "first_name", "surname", "organisation_id", "salt",
                        "password_sha256", "confirmed", "system_user"},
                whereSet);

        if (row != null) {
            User user = new User();
            user.setId(row.getUUID(0));
            user.setEmail(row.getString(1));
            user.setFirst_name(row.getString(2));
            user.setSurname(row.getString(3));
            user.setOrganisation_id(row.getUUID(4));
            user.setSalt(row.getUUID(5));
            user.setPassword_sha256(row.getString(6));
            user.setConfirmed(row.getBool(7));
            user.setSystem_user(row.getBool(8));
            return user;
        }
        return null;
    }

    /**
     * make sure no user data is lost in an update
     *
     * @param target   the user to insert
     * @param original the original / previous user
     */
    private void merge(User target, User original) {
        if (original != null && target != null) {
            if (target.getPassword_sha256() == null) {
                target.setPassword_sha256(original.getPassword_sha256());
            }
            if (target.getSalt() == null) {
                target.setSalt(original.getSalt());
            }
            if (!target.isConfirmed()) {
                target.setConfirmed(original.isConfirmed());
            }
            if (!target.isSystem_user()) {
                target.setSystem_user(original.isSystem_user());
            }
            if (target.getEmail() == null) {
                target.setEmail(original.getEmail());
            }
            if (target.getFirst_name() == null) {
                target.setFirst_name(original.getFirst_name());
            }
            if (target.getSurname() == null) {
                target.setSurname(original.getSurname());
            }
        }
    }

    @Override
    public void updateUser(User user, String userPassword) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        if (userPassword != null) {
            merge(user, readUser(user.getEmail())); // make sure no data is lost
            user.setSalt(UUID.randomUUID());
            user.setPassword_sha256(new Sha256().generateSha256Password(user.getSalt(), userPassword));
        }
        insertUser(user);
    }

    @Override
    public void deleteUser(UUID organisation_id, String email) {
        HashMap<String, Object> whereSet = new HashMap<>();
        whereSet.put("email", email);
        cluster.deleteOne("user", whereSet);
        UserEmailList list = readUserList(organisation_id);
        if (list != null) {
            list.remove(email);
            updateUserList(organisation_id, list);
        }
    }

    @Override
    public UserEmailList readUserList(UUID organisation_id) {
        HashMap<String, Object> whereSet = new HashMap<>();
        whereSet.put("organisation_id", organisation_id);
        Row row = cluster.selectOne("user_email_list", new String[]{"email_list"}, whereSet);
        if (row != null) {
            return new UserEmailList(organisation_id, row.getList(0, String.class));
        } else {
            return new UserEmailList();
        }
    }

    @Override
    public void updateUserList(UUID organisation_id, UserEmailList userEmailList) {
        if (userEmailList != null && userEmailList.getUser_list() != null) {
            Statement insert = cluster.insert("user_email_list")
                    .values(new String[]{"organisation_id", "email_list"},
                            new Object[]{
                                    organisation_id, userEmailList.getUser_list()
                            });
            cluster.executeWithRetry(insert);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////

    private void insertGroup(Group group) {
        // 	name text, organisation_id uuid, user_list list<text>,
        Statement insert = cluster.insert("group")
                .values(new String[]{
                                "name", "organisation_id", "user_list"},
                        new Object[]{
                                group.getName(), group.getOrganisation_id(), group.getUser_list()
                        });
        cluster.executeWithRetry(insert);
    }

    @Override
    public Group createGroup(UUID organisation_id, Group group) {
        group.setOrganisation_id(organisation_id);
        insertGroup(group);
        return group;
    }

    @Override
    public Group readGroup(UUID organisation_id, String name) {
        HashMap<String, Object> whereSet = new HashMap<>();
        whereSet.put("organisation_id", organisation_id);
        whereSet.put("name", name);
        Row row = cluster.selectOne("group",
                new String[]{"name", "organisation_id", "user_list"},
                whereSet);

        if (row != null) {
            Group group = new Group();
            group.setName(row.getString(0));
            group.setOrganisation_id(row.getUUID(1));
            group.setUser_list(row.getList(2, String.class));
            return group;
        }
        return null;
    }

    @Override
    public List<Group> readAllGroups(UUID organisation_id) {
        HashMap<String, Object> whereSet = new HashMap<>();
        whereSet.put("organisation_id", organisation_id);
        ResultSet resultSet = cluster.selectAll("group", new String[]{"name", "user_list"}, whereSet);
        if (resultSet != null) {
            List<Group> groupList = new ArrayList<>();
            for (Row row : resultSet) {
                Group group = new Group();
                group.setOrganisation_id(organisation_id);
                group.setName(row.getString(0));
                group.setUser_list(row.getList(1, String.class));
                groupList.add(group);
            }
            return groupList;
        }
        return null;
    }

    @Override
    public void updateGroup(UUID organisation_id, Group group) {
        group.setOrganisation_id(organisation_id);
        insertGroup(group);

    }

    @Override
    public void deleteGroup(UUID organisation_id, String name) {
        HashMap<String, Object> whereSet = new HashMap<>();
        whereSet.put("name", name);
        whereSet.put("organisation_id", organisation_id);
        cluster.deleteOne("group", whereSet);
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    // document images

    // insert a document's image into the document image store
    @Override
    public void saveDocumentImage(UUID organisation_id, String url, byte[] png_data) {
        if ( organisation_id != null && url != null && png_data != null && png_data.length > 100 ) {
            // 	organisation_id uuid, url text, data blob
            ByteBuffer buffer = ByteBuffer.wrap(png_data);
            Statement insert = cluster.insert("document_image")
                    .values(new String[]{"organisation_id", "url", "data"},
                            new Object[]{
                                    organisation_id, url, buffer
                            });
            cluster.executeWithRetry(insert);
        }
    }

    // remove a document's image from the store
    @Override
    public void removeDocumentImage(UUID organisation_id, String url) {
        HashMap<String, Object> whereSet = new HashMap<>();
        whereSet.put("url", url);
        whereSet.put("organisation_id", organisation_id);
        cluster.deleteOne("document_image", whereSet);
    }

    // retrieve a document's PNG image from the store
    @Override
    public byte[] getDocumentImage(UUID organisation_id, String url) {
        if ( organisation_id != null && url != null ) {
            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);
            whereSet.put("url", url);

            // organisation_id uuid, url text, block_id int, data blob,
            Row row = cluster.selectOne("document_image", new String[]{"data"}, whereSet);
            if ( row != null ) {
                ByteBuffer buffer = row.getBytes(0);
                if (buffer != null && buffer.remaining() > 0) {
                    int dataLoadSize = buffer.remaining();
                    byte[] bytes = new byte[dataLoadSize];
                    buffer.get(bytes, 0, bytes.length);
                    return bytes;
                }
            }
        }
        return null;
    }

    /////////////////////////////////////////////////////////////////////////////////////////

    private void insertDocument(Document document) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        // insert acls
        // 	organisation_id uuid, url text, acl_list uuid, date_time_uploaded bigint
        List<String> aclValuesList = new ArrayList<>();
        for (Acl acl : document.getAcl_set()) {
            aclValuesList.add(mapper.writeValueAsString(acl));
        }
        Statement insert = cluster.insert("document_registry")
                .values(new String[]{
                                "url", "organisation_id", "acl_list", "date_time_uploaded", "origin",
                                "title", "author", "created", "content_hash", "acl_hash", "processing_pipeline",
                                "ts_converted",
                                "ts_parsed",
                                "ts_vectorised",
                                "ts_summarised",
                                "ts_indexed",
                                "ts_clustered",
                                "ts_entity_analysed",
                                "ts_emotion_analysed",
                                "ts_knowledge_analysed"},

                        new Object[]{
                                document.getUrl(), document.getOrganisation_id(),
                                aclValuesList, document.getDate_time_uploaded(), document.getOrigin(),
                                document.getTitle(), document.getAuthor(), document.getCreated(),
                                document.getContent_hash(), document.getAclHash(), document.getProcessingPipeline(),
                                document.getTs_converted(), document.getTs_parsed(),
                                document.getTs_vectorised(), document.getTs_summarised(),
                                document.getTs_indexed(), document.getTs_clustered(),
                                document.getTs_entity_analysed(), document.getTs_emotion_analysed(),
                                document.getTs_knowledge_analysed()
                        });
        cluster.executeWithRetry(insert);

        // insert meta-data
        // 	organisation_id uuid, url text, name text, value text
        for (String name : document.getName_value_set().keySet()) {
            String value = document.getName_value_set().get(name);
            Statement insertmd = cluster.insert("document_metadata")
                    .values(new String[]{
                                    "url", "organisation_id", "name", "value"},
                            new Object[]{
                                    document.getUrl(), document.getOrganisation_id(), name, value
                            });
            cluster.executeWithRetry(insertmd);
        }
    }

    /**
     * helper function - write data from ByteBuffer into blobBuffer - copy
     *
     * @param blobBuffer the destination
     * @param data       the source
     * @return the number of bytes written into blobBuffer
     */
    private int getDocumentDataFromByteBuffer(BinaryBlob blobBuffer, ByteBuffer data) {
        if (blobBuffer != null && data != null && data.remaining() > 0) {
            int dataLoadSize = data.remaining();
            byte[] bytes = new byte[dataLoadSize];
            data.get(bytes, 0, bytes.length);
            blobBuffer.writeByteArray(bytes, 0, bytes.length);
            return dataLoadSize;
        }
        return 0;
    }

    @Override
    public Document createDocument(UUID organisation_id, Document document) throws IOException {
        document.setOrganisation_id(organisation_id);
        insertDocument(document);
        return document;
    }

    /**
     * cassandra row result to document
     *
     * @param row the row
     * @return the document
     */
    private Document rowToDocument(Row row) throws IOException {
        if (row != null) {
            ObjectMapper mapper = new ObjectMapper();
            Document document = new Document();

            document.setUrl(row.getString(0));
            document.setOrganisation_id(row.getUUID(1));

            HashSet<Acl> newAclSet = new HashSet<>();
            List<String> serialisedList = row.getList(2, String.class);
            if (serialisedList != null) {
                for (String item : serialisedList) {
                    newAclSet.add(mapper.readValue(item, Acl.class));
                }
            }
            document.setAcl_set(newAclSet);
            document.setOrigin(row.getString(3));

            document.setTitle(row.getString(4));
            document.setAuthor(row.getString(5));
            document.setCreated(row.getLong(6));
            document.setContent_hash(row.getString(7));
            document.setAclHash(row.getInt(8));

            document.setProcessingPipeline(row.getLong(9));
            document.setDate_time_uploaded(row.getLong(10));

            document.setTs_converted(row.getLong(11));
            document.setTs_parsed(row.getLong(12));
            document.setTs_vectorised(row.getLong(13));
            document.setTs_summarised(row.getLong(14));
            document.setTs_indexed(row.getLong(15));
            document.setTs_clustered(row.getLong(16));
            document.setTs_entity_analysed(row.getLong(17));
            document.setTs_emotion_analysed(row.getLong(18));
            document.setTs_knowledge_analysed(row.getLong(19));

            return document;
        }
        return null;
    }

    @Override
    public Document readDocument(UUID organisation_id, String url) throws IOException {
        HashMap<String, Object> whereSet = new HashMap<>();
        whereSet.put("url", url);
        whereSet.put("organisation_id", organisation_id);
        Row row = cluster.selectOne("document_registry",
                new String[]{"url", "organisation_id", "acl_list", "origin",
                        "title", "author", "created", "content_hash", "acl_hash",
                        "processing_pipeline", "date_time_uploaded",
                        "ts_converted",
                        "ts_parsed",
                        "ts_vectorised",
                        "ts_summarised",
                        "ts_indexed",
                        "ts_clustered",
                        "ts_entity_analysed",
                        "ts_emotion_analysed",
                        "ts_knowledge_analysed"},
                whereSet);

        if (row != null) {

            Document document = rowToDocument(row);

            // read the meta-data
            ResultSet resultSet = cluster.selectAll("document_metadata", new String[]{"name", "value"}, whereSet);
            if (resultSet != null) {
                for (Row row2 : resultSet) {
                    document.getName_value_set().put(row2.getString(0), row2.getString(1));
                }
            }

            return document;
        }

        return null;
    }

    /**
     * copy across items from the target to the original where the original is lacking
     *
     * @param target   the target document
     * @param original the previous / original document
     */
    private void merge(Document target, Document original) {
        if (original != null && target != null) {
            if (target.getAcl_set() == null) {
                target.setAcl_set(original.getAcl_set());
            }
            if (target.getName_value_set() == null) {
                target.setName_value_list(original.getName_value_set());
            }
            if (target.getOrigin() == null) {
                target.setOrigin(original.getOrigin());
            }
            if (target.getTs_converted() == 0) {
                target.setTs_converted(original.getTs_converted());
            }
            if (target.getTs_emotion_analysed() == 0) {
                target.setTs_emotion_analysed(original.getTs_emotion_analysed());
            }
            if (target.getTs_parsed() == 0) {
                target.setTs_parsed(original.getTs_parsed());
            }
            if (target.getTs_entity_analysed() == 0) {
                target.setTs_entity_analysed(original.getTs_entity_analysed());
            }
            if (target.getTs_clustered() == 0) {
                target.setTs_clustered(original.getTs_clustered());
            }
            if (target.getTs_indexed() == 0) {
                target.setTs_indexed(original.getTs_indexed());
            }
            if (target.getTs_vectorised() == 0) {
                target.setTs_vectorised(original.getTs_vectorised());
            }
            if (target.getTs_summarised() == 0) {
                target.setTs_summarised(original.getTs_summarised());
            }
            if (target.getTs_knowledge_analysed() == 0) {
                target.setTs_knowledge_analysed(original.getTs_knowledge_analysed());
            }
            if (target.getDate_time_uploaded() == 0) {
                target.setDate_time_uploaded(original.getDate_time_uploaded());
            }
            if (target.getAclHash() == 0) {
                target.setAclHash(original.getAclHash());
            }
        }
    }

    @Override
    public void updateDocument(UUID organisation_id, Document document) throws IOException {
        document.setOrganisation_id(organisation_id);
        merge(document, readDocument(organisation_id, document.getUrl()));
        insertDocument(document);
    }

    @Override
    public void deleteDocument(UUID organisation_id, String url) {
        HashMap<String, Object> whereSet = new HashMap<>();
        whereSet.put("url", url);
        whereSet.put("organisation_id", organisation_id);
        cluster.deleteOne("document_registry", whereSet);
        cluster.deleteOne("document_binary", whereSet);
        cluster.deleteOne("document_author", whereSet);
        cluster.deleteOne("document_parsed_binary", whereSet);
        cluster.deleteOne("document_vector", whereSet);

        // remove the tuples for this document
        deleteTuplesByUrl(organisation_id, url);
    }

    /**
     * select a paginated sub-set of documents
     *
     * @param organisation_id uuid of the organisation
     * @param prevUrl         previous (optional) token url
     * @param pageSize        the size of the set to return
     * @return a list of documents
     */
    public List<Document> getDocumentList(UUID organisation_id, String prevUrl, int pageSize) throws IOException {
        List<Document> documentList = new ArrayList<>();

        Statement statement;
        if (prevUrl != null) {
            statement = QueryBuilder.select(new String[]{"url", "organisation_id", "acl_list", "origin",
                    "title", "author", "created", "content_hash", "acl_hash",
                    "processing_pipeline", "date_time_uploaded",
                    "ts_converted",
                    "ts_parsed",
                    "ts_vectorised",
                    "ts_summarised",
                    "ts_indexed",
                    "ts_clustered",
                    "ts_entity_analysed",
                    "ts_emotion_analysed",
                    "ts_knowledge_analysed"})
                    .from("document_registry")
                    .where(QueryBuilder.eq("organisation_id", organisation_id))
                    .and(QueryBuilder.gt("url", prevUrl));
        } else {
            statement = QueryBuilder.select(new String[]{"url", "organisation_id", "acl_list", "origin",
                    "title", "author", "created", "content_hash", "acl_hash",
                    "processing_pipeline", "date_time_uploaded",
                    "ts_converted",
                    "ts_parsed",
                    "ts_vectorised",
                    "ts_summarised",
                    "ts_indexed",
                    "ts_clustered",
                    "ts_entity_analysed",
                    "ts_emotion_analysed",
                    "ts_knowledge_analysed"})
                    .from("document_registry")
                    .where(QueryBuilder.eq("organisation_id", organisation_id));
        }

        statement.setFetchSize(pageSize);

        ResultSet results = cluster.executeWithRetry(statement);
        if (results != null) {
            for (Iterator<Row> iterator = results.iterator(); iterator.hasNext() && documentList.size() < pageSize; ) {
                Row row = iterator.next();
                Document document = rowToDocument(row);
                documentList.add(document);
            }
        }
        return documentList;
    }

    /**
     * get a list of the urls in the document registry - just the urls
     *
     * @param organisation_id the organsiation
     * @param prevUrl         pagination, the previous url
     * @param limit           page size
     * @return list of urls
     */
    public List<String> getDocumentUrlList(UUID organisation_id, String prevUrl, int limit) {
        List<String> documentUrlList = new ArrayList<>();

        Statement statement;
        if (prevUrl != null) {
            statement = QueryBuilder.select(new String[]{"url"})
                    .from("document_registry")
                    .where(QueryBuilder.eq("organisation_id", organisation_id))
                    .and(QueryBuilder.gt("url", prevUrl));
        } else {
            statement = QueryBuilder.select(new String[]{"url"})
                    .from("document_registry")
                    .where(QueryBuilder.eq("organisation_id", organisation_id));
        }

        statement.setFetchSize(limit);

        ResultSet results = cluster.executeWithRetry(statement);
        if (results != null) {
            for (Iterator<Row> iterator = results.iterator(); iterator.hasNext() && documentUrlList.size() < limit; ) {
                Row row = iterator.next();
                documentUrlList.add(row.getString(0));
            }
        }
        return documentUrlList;
    }


    /**
     * save a block of data - split it into blobs for size management
     *
     * @param organisation_id the organisation this document belongs to
     * @param url             the url of the document, a unique id for the owner
     * @param data            the binary data to be saved
     */
    @Override
    public void uploadDocumentBinary(UUID organisation_id, String url, byte[] data) {
        if (organisation_id != null && url != null && url.length() > 0 && data != null && data.length > 0) {
            int size = data.length;
            int index = 0;
            int blockid = 0;
            while (size > 0) {

                // fit the data neatly into blobs of MAX_BLOB_SIZE_IN_BYTES
                byte[] tempBlock;
                if (size > MAX_BLOB_SIZE_IN_BYTES) {
                    tempBlock = new byte[MAX_BLOB_SIZE_IN_BYTES];
                    System.arraycopy(data, index, tempBlock, 0, MAX_BLOB_SIZE_IN_BYTES);
                    index = index + MAX_BLOB_SIZE_IN_BYTES;
                    size = size - MAX_BLOB_SIZE_IN_BYTES;
                } else {
                    tempBlock = new byte[size];
                    System.arraycopy(data, index, tempBlock, 0, size);
                    index = index + size;
                    size = 0;
                }

                // wrap the block into a byte buffer for Cassandra
                ByteBuffer buffer = ByteBuffer.wrap(tempBlock);

                // organisation_id uuid, url text, block_id int, data blob,
                Insert insert = cluster.insert("document_binary")
                        .values(new String[]{"organisation_id", "url", "block_id", "data"},
                                new Object[]{organisation_id, url, blockid, buffer});

                cluster.executeWithRetry(insert);

                // unique column name for the item inserted
                blockid = blockid + 1;
            }
        }
    }

    @Override
    public byte[] getDocumentBinary(UUID organisation_id, String url) {
        if (url != null && organisation_id != null) {
            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);
            whereSet.put("url", url);

            BinaryBlob stringBuffer = new BinaryBlob(MAX_BLOB_SIZE_IN_BYTES);
            int blockid = 0;
            int dataLoadSize;
            do {
                // organisation_id uuid, url text, block_id int, data blob,
                whereSet.put("block_id", blockid);
                Row row = cluster.selectOne("document_binary", new String[]{"data "}, whereSet);
                dataLoadSize = getDocumentDataFromByteBuffer(stringBuffer, row.getBytes(0));
                blockid = blockid + 1;

            } while (dataLoadSize == MAX_BLOB_SIZE_IN_BYTES);

            if (stringBuffer.getSize() > 0) {
                return stringBuffer.getData();
            }
        }
        return null;
    }

    // document_parsed_binary
    public Map<String, byte[]> getDocumentParseTreeMap(UUID organisation_id, String url) {
        HashMap<String, byte[]> documentMap = new HashMap<>();
        HashMap<String, Object> whereSet = new HashMap<>();
        whereSet.put("organisation_id", organisation_id);
        whereSet.put("url", url);
        ResultSet resultSet = cluster.selectAll("document_parsed_binary", new String[]{"meta_data", "data"}, whereSet);
        if (resultSet != null) {
            for (Row row : resultSet) {
                BinaryBlob binary = new BinaryBlob(65536);
                getDocumentDataFromByteBuffer(binary, row.getBytes(1));
                documentMap.put(row.getString(0), binary.getData());
            }
        }
        if (documentMap.size() > 0) {
            return documentMap;
        }
        return null;
    }

    // document_parsed_binary
    public void saveDocumentParseTreeMap(UUID organisation_id, String url, Map<String, byte[]> map) {
        // organisation_id uuid, url text, meta_data text, block_id int, data blob
        for (String key : map.keySet()) {
            byte[] value = map.get(key);
            ByteBuffer buffer = ByteBuffer.wrap(value);
            Insert insert = cluster.insert("document_parsed_binary")
                    .value("organisation_id", organisation_id)
                    .value("url", url)
                    .value("meta_data", key)
                    .value("data", buffer);
            cluster.executeWithRetry(insert);
        }
        // save document author if they exist
        if (map.containsKey(Document.META_AUTHOR)) {
            byte[] value = map.get(Document.META_AUTHOR);
            ByteBuffer buffer = ByteBuffer.wrap(value);
            Insert insert = cluster.insert("document_author")
                    .value("organisation_id", organisation_id)
                    .value("url", url)
                    .value("data", buffer);
            cluster.executeWithRetry(insert);
        }
    }

    // return a map of url -> author parsed data
    public Map<String, byte[]> getAuthorsForUrlList(UUID organisation_id, List<String> urlList ) {
        Map<String, byte[]> returnData = new HashMap<>();
        if ( urlList != null && organisation_id != null ) {
            for ( String url :  urlList ) {
                HashMap<String, Object> whereSet = new HashMap<>();
                whereSet.put("organisation_id", organisation_id);
                whereSet.put("url", url);
                ResultSet resultSet = cluster.selectAll("document_author", new String[]{"data"}, whereSet);
                if (resultSet != null) {
                    for (Row row : resultSet) {
                        BinaryBlob binary = new BinaryBlob(65536);
                        getDocumentDataFromByteBuffer(binary, row.getBytes(0));
                        returnData.put(url, binary.getData());
                    }
                }
            }
        }
        return returnData;
    }

    public void saveDocumentHistogram(UUID organisation_id, String url, CompressedVector compressedVector) throws IOException {
        String vectorStr = new ObjectMapper().writeValueAsString(compressedVector);
        Insert insert = cluster.insert("document_vector")
                .value("organisation_id", organisation_id)
                .value("url", url)
                .value("vector_type", "histogram")
                .value("vector", vectorStr);
        cluster.executeWithRetry(insert);
    }

    // load a document histogram
    public CompressedVector loadDocumentHistogram(UUID organisation_id, String url) throws IOException {
        HashMap<String, Object> whereSet = new HashMap<>();
        whereSet.put("organisation_id", organisation_id);
        whereSet.put("url", url);
        whereSet.put("vector_type", "histogram");
        Row row = cluster.selectOne("document_vector", new String[]{"vector"}, whereSet);
        if (row != null) {
            String jsonStr = row.getString(0);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonStr, CompressedVector.class);
        }
        return null;
    }


    public void saveDocumentSummarizationWordSet(UUID organisation_id, String url, SummarisationSet fragmentSet) throws IOException {
        String fragmentSetStr = new ObjectMapper().writeValueAsString(fragmentSet);
        Insert insert = cluster.insert("document_summarize_word")
                .value("organisation_id", organisation_id)
                .value("url", url)
                .value("fragment", fragmentSetStr);
        cluster.executeWithRetry(insert);
    }

    // read back a summarisation word set
    public SummarisationSet loadDocumentSummarizationWordSet(UUID organisation_id, String url) throws IOException {
        HashMap<String, Object> whereSet = new HashMap<>();
        whereSet.put("organisation_id", organisation_id);
        whereSet.put("url", url);
        Row row = cluster.selectOne("document_summarize_word", new String[]{"fragment"}, whereSet);
        if (row != null) {
            String jsonStr = row.getString(0);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonStr, SummarisationSet.class);
        }
        return null;
    }

    public void saveDocumentSummarizationSentenceSet(UUID organisation_id, String url, Sentence sentence) throws IOException {
        String sentence_json = new ObjectMapper().writeValueAsString(sentence);
        Insert insert = cluster.insert("document_summarize_sentence")
                .value("organisation_id", organisation_id)
                .value("url", url)
                .value("sentence_json", sentence_json);
        cluster.executeWithRetry(insert);
    }

    // read back a summarisation sentence
    public Sentence loadDocumentSummarizationSentenceSet(UUID organisation_id, String url) throws IOException {
        HashMap<String, Object> whereSet = new HashMap<>();
        whereSet.put("organisation_id", organisation_id);
        whereSet.put("url", url);
        Row row = cluster.selectOne("document_summarize_sentence", new String[]{"sentence_json"}, whereSet);
        if (row != null) {
            String jsonStr = row.getString(0);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonStr, Sentence.class);
        }
        return null;
    }

    // save a list of similar documents - all possible combinations
    public void saveDocumentSimilarityMap(UUID organisation_id, List<SimilarDocument> similarDocumentList) {
        if (organisation_id != null && similarDocumentList != null) {

            for (int i = 0; i < similarDocumentList.size(); i++) {
                SimilarDocument doc1 = similarDocumentList.get(i);
                saveSimilarity(organisation_id, doc1);
                for (int j = 0; j < similarDocumentList.size(); j++) {
                    if (i != j) {
                        SimilarDocument doc2 = similarDocumentList.get(j);
                        saveSimilarity(organisation_id, doc2);

                        double dist = (doc1.getSimilarity() + doc2.getSimilarity()) / 2.0;
                        saveSimilarity(organisation_id, doc1.getUrl1(), doc2.getUrl1(), dist);
                        saveSimilarity(organisation_id, doc1.getUrl1(), doc2.getUrl2(), dist);
                        saveSimilarity(organisation_id, doc1.getUrl2(), doc2.getUrl1(), dist);
                        saveSimilarity(organisation_id, doc1.getUrl2(), doc2.getUrl2(), dist);
                    }
                }
            }

        } // if valid parameters
    }

    private void saveSimilarity(UUID organisation_id, SimilarDocument document) {
        String url1 = document.getUrl1();
        String url2 = document.getUrl2();
        double similarity = document.getSimilarity();
        saveSimilarity(organisation_id, url1, url2, similarity);
    }

    private void saveSimilarity(UUID organisation_id, String url1, String url2, double similarity) {
        {
            Insert insert = cluster.insert("document_similars")
                    .value("organisation_id", organisation_id)
                    .value("url1", url1)
                    .value("url2", url2)
                    .value("similarity", similarity);
            cluster.executeWithRetry(insert);
        }

        {
            Insert insert = cluster.insert("document_similars")
                    .value("organisation_id", organisation_id)
                    .value("url1", url2)
                    .value("url2", url1)
                    .value("similarity", similarity);
            cluster.executeWithRetry(insert);
        }
    }

    // return all similar documents
    public List<SimilarDocument> loadSimilarDocuments(UUID organisation_id, String url) {

        if (organisation_id != null && url != null) {

            List<SimilarDocument> similarDocumentList = new ArrayList<>();

            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);
            whereSet.put("url1", url);

            ResultSet resultSet = cluster.selectAll("document_similars", new String[]{"url2", "similarity"}, whereSet);
            if (resultSet != null) {
                for (Row row : resultSet) {
                    String url2 = row.getString(0);
                    double value = row.getDouble(1);
                    similarDocumentList.add(new SimilarDocument(url, url2, value));
                }
            }

            if (similarDocumentList.size() > 0) {
                return similarDocumentList;
            }

        } // if valid parameters

        return null;
    }


    // return all similar documents
    public List<SimilarDocumentSet> loadSimilarDocuments(UUID organisation_id) {

        if (organisation_id != null) {

            List<SimilarDocumentSet> similarDocumentList = new ArrayList<>();

            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);

            Map<String, SimilarDocumentSet> map = new HashMap<>();
            ResultSet resultSet = cluster.selectAll("document_similars", new String[]{"url1", "url2", "similarity"}, whereSet);
            if (resultSet != null) {
                for (Row row : resultSet) {
                    String url1 = row.getString(0);
                    String url2 = row.getString(1);
                    double value = row.getDouble(2);
                    addSimilar(url1, url2, value, map);
                }
            }

            if (map.size() > 0) {
                similarDocumentList.addAll(map.values());
                return similarDocumentList;
            }

        } // if valid parameters

        return null;
    }

    // helper for loadSimilarDocuments() above
    private void addSimilar(String url1, String url2, double value, Map<String, SimilarDocumentSet> map) {
        SimilarDocumentSet similarDocument = map.get(url1);
        if (similarDocument == null) {
            similarDocument = new SimilarDocumentSet();
            similarDocument.setUrl(url1);
            map.put(url1, similarDocument);
        }
        similarDocument.getSimilarDocumentList().add(new SimilarDocument(url1, url2, value));
    }

    /////////////////////////////////////////////
    // case tuples

    // read tuples for a given set of ids
    public List<Tuple> readTuples(UUID organisation_id, List<UUID> idArray) throws IOException {
        if (organisation_id != null && idArray != null && idArray.size() > 0) {
            List<Tuple> tupleList = new ArrayList<>();

            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);

            for (UUID id : idArray) {
                whereSet.put("id", id);
                ResultSet resultSet = cluster.selectAll("tuple", new String[]{"json"}, whereSet);
                if (resultSet != null) {
                    for (Row row : resultSet) {
                        String jsonStr = row.getString(0);
                        tupleList.add(new ObjectMapper().readValue(jsonStr, Tuple.class));
                    }
                }
            }
            return tupleList;
        }
        return null;
    }


    // write a set of tuples to the database
    public void writeTuple(UUID organisation_id, Tuple tuple) throws IOException {
        if (organisation_id != null && tuple != null) {
            int sentence = tuple.getSentence_id();
            String url = tuple.getUrl();
            UUID tupleID = tuple.getId();
            if (tupleID == null) {
                tupleID = UUID.randomUUID();
            }
            String jsonStr = new ObjectMapper().writeValueAsString(tuple);

            {
                Insert statement = cluster.insert("tuple")
                        .value("organisation_id", organisation_id)
                        .value("id", tupleID)
                        .value("url", url)
                        .value("sentence_id", sentence)
                        .value("json", jsonStr);
                cluster.executeWithRetry(statement);
            }

            {
                // record the tuple belonging to this document
                Insert statement = cluster.insert("tuple_by_url")
                        .value("organisation_id", organisation_id)
                        .value("id", tupleID)
                        .value("url", url);
                cluster.executeWithRetry(statement);
            }

        }
    }

    // read all tuples from the database for a given document
    public List<Tuple> readTuplesForDocument(UUID organisation_id, String url) throws IOException {
        if (organisation_id != null && url != null) {
            // get all tuples for that url
            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);
            whereSet.put("url", url);
            ResultSet resultSet = cluster.selectAll("tuple_by_url", new String[]{"id"}, whereSet);
            List<UUID> tupleIdList = new ArrayList<>();
            if (resultSet != null) {
                for (Row row : resultSet) {
                    tupleIdList.add(row.getUUID(0));
                }
            }
            if (tupleIdList.size() > 0) {
                List<Tuple> tupleList = readTuples(organisation_id, tupleIdList);
                if ( tupleList.size() > 0 ) {
                    return tupleList;
                }
            }
        }
        return null;
    }

    // remove a set of tuples for a given document
    public void deleteTuplesByUrl(UUID organisation_id, String url) {
        if (organisation_id != null && url != null) {

            // get all tuples for that url
            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);
            whereSet.put("url", url);
            ResultSet resultSet = cluster.selectAll("tuple_by_url", new String[]{"id"}, whereSet);
            List<UUID> tupleIdList = new ArrayList<>();
            if (resultSet != null) {
                for (Row row : resultSet) {
                    tupleIdList.add(row.getUUID(0));
                }
            }

            // delete the tuples for this document
            HashMap<String, Object> deleteWhereSet = new HashMap<>();
            deleteWhereSet.put("organisation_id", organisation_id);
            for (UUID id : tupleIdList) {
                deleteWhereSet.put("id", id);
                cluster.deleteOne("tuple", deleteWhereSet);
            }

            // and delete the last entry - the url -> tuple id[]
            cluster.deleteOne("tuple_by_url", whereSet);
        }
    }


    /////////////////////////////////////////////////////////////////////////////////////////

    private void insertOrganisation(Organisation organisation) {
        // 	id uuid, name text, primary_user uuid,
        Statement insert = cluster.insert("organisation")
                .values(new String[]{
                                "id", "name", "primary_user"},
                        new Object[]{
                                organisation.getId(), organisation.getName(), organisation.getPrimary_user()
                        });
        cluster.executeWithRetry(insert);
    }

    @Override
    public Organisation createOrganisation(Organisation organisation) {
        organisation.setId(UUID.randomUUID());
        insertOrganisation(organisation);

        // lookup by id: id uuid, name text
        Statement insert = cluster.insert("organisation_by_id")
                .values(new String[]{
                                "id", "name"},
                        new Object[]{
                                organisation.getId(), organisation.getName()
                        });
        cluster.executeWithRetry(insert);

        return organisation;
    }

    @Override
    public Organisation readOrganisation(String name) {
        HashMap<String, Object> whereSet = new HashMap<>();
        whereSet.put("name", name);
        // 	id uuid, name text, primary_user uuid,
        Row row = cluster.selectOne("organisation", new String[]{"id", "name", "primary_user"}, whereSet);
        if (row != null) {
            return new Organisation(row.getUUID(0), row.getString(1), row.getUUID(2));
        }
        return null;
    }

    @Override
    public void updateOrganisation(Organisation organisation) {
        insertOrganisation(organisation);
    }

    @Override
    public String getOrganisationName(UUID id) {
        HashMap<String, Object> whereSet = new HashMap<>();
        whereSet.put("id", id);
        // 	id uuid, name text, primary_user uuid,
        Row row = cluster.selectOne("organisation_by_id", new String[]{"name"}, whereSet);
        if (row != null) {
            return row.getString(0);
        }
        return null;
    }

    /**
     * @return list of all organisations
     */
    public List<Organisation> getOrganisationList() {
        List<Organisation> organisationList = new ArrayList<>();
        Statement statement = QueryBuilder.select(new String[]{"id", "name", "primary_user"}).from("organisation");
        ResultSet results = cluster.executeWithRetry(statement);
        if (results != null) {
            for (Row row : results) {
                organisationList.add(new Organisation(row.getUUID(0), row.getString(1), row.getUUID(2)));
            }
        }
        return organisationList;
    }

    /////////////////////////////////////////////////////////////////////////////////////////

    private void insertSession(Session session) {
        {
            // id uuid, email text, ip_address text, last_access bigint,
            Insert insert = cluster.insert("user_session")
                    .values(new String[]{
                                    "id", "email", "ip_address", "last_access"
                            },
                            new Object[]{
                                    session.getId(), session.getEmail(), session.getIp_address(), session.getLast_access()
                            });
            cluster.executeWithRetry(insert);
        }
        {
            // id uuid, email text, ip_address text, last_access bigint,
            Insert insert = cluster.insert("user_session_by_email")
                    .values(new String[]{
                                    "id", "email", "ip_address", "last_access"
                            },
                            new Object[]{
                                    session.getId(), session.getEmail(), session.getIp_address(), session.getLast_access()
                            });
            cluster.executeWithRetry(insert);
        }
    }

    @Override
    public Session createSession(String userEmail, String ipAddress) {
        UUID sessionID = UUID.randomUUID();
        Session session = new Session();
        session.setId(sessionID);
        session.setIp_address(ipAddress);
        session.setEmail(userEmail);
        session.setLast_access(System.currentTimeMillis());
        insertSession(session);
        return session;
    }

    @Override
    public Session getSession(UUID sessionID) {
        Session session = null;
        HashMap<String, Object> whereSet = new HashMap<>();
        whereSet.put("id", sessionID);
        // 	id uuid, name text, primary_user uuid,
        Row row = cluster.selectOne("user_session", new String[]{"id", "email", "ip_address", "last_access"}, whereSet);
        if (row != null) {
            session = new Session(row.getUUID(0), row.getString(1), row.getString(2), row.getLong(3));
        }

        if (session != null) {
            if (session.getLast_access() < (System.currentTimeMillis() - Session.SESSION_TIMEOUT_IN_MS)) {
                logger.info("session timed out");
                cluster.deleteOne("user_session", whereSet);
                {
                    HashMap<String, Object> whereSet2 = new HashMap<>();
                    whereSet2.put("email", session.getEmail());
                    cluster.deleteOne("user_session_by_email", whereSet2);
                }
                return null;
            }
            // refresh the valid session
            session.setLast_access(System.currentTimeMillis());
            insertSession(session);
        }
        return session;
    }

    @Override
    public List<Session> getActiveSessions(UUID organisation_id) {
        List<Session> sessionList = new ArrayList<>();
        if (organisation_id != null) {
            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);
            // 	id uuid, name text, primary_user uuid,
            ResultSet resultSet = cluster.selectAll("active_session", new String[]{"id", "email", "ip_address", "last_access"}, whereSet);
            if (resultSet != null) {
                for (Row row : resultSet.all()) {
                    long timeout = row.getLong(3);
                    UUID id = row.getUUID(0);
                    // session still valid?
                    if (timeout < (System.currentTimeMillis() - Session.SESSION_TIMEOUT_IN_MS)) {
                        removeActiveSession(organisation_id, id);
                    } else {
                        sessionList.add(new Session(id, row.getString(1), row.getString(2), timeout));
                    }
                }
            } // if resultSet
        }
        return sessionList;
    }

    /**
     * create an active session by organisation id
     *
     * @param organisation_id the organisation's id
     * @param session         the full session to store
     */
    @Override
    public void createActiveSession(UUID organisation_id, Session session) {
        if (organisation_id != null && session != null && session.getId() != null) {
            Insert insert = cluster.insert("active_session")
                    .values(new String[]{
                                    "organisation_id", "id", "email", "ip_address", "last_access"
                            },
                            new Object[]{
                                    organisation_id, session.getId(), session.getEmail(), session.getIp_address(), session.getLast_access()
                            });
            cluster.executeWithRetry(insert);
        }
    }

    /**
     * create an active session by organisation id
     *
     * @param organisation_id the organisation's id
     * @param session_id      the id of the session to remove
     */
    @Override
    public void removeActiveSession(UUID organisation_id, UUID session_id) {
        if (organisation_id != null && session_id != null) {
            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);
            whereSet.put("id", session_id);
            cluster.deleteOne("active_session", whereSet);
        }
    }

    @Override
    public Session getExistingSessionByEmail(String email) {
        Session session = null;
        HashMap<String, Object> whereSet = new HashMap<>();
        whereSet.put("email", email);
        // 	id uuid, name text, primary_user uuid,
        Row row = cluster.selectOne("user_session_by_email", new String[]{"id", "email", "ip_address", "last_access"}, whereSet);
        if (row != null) {
            session = new Session(row.getUUID(0), row.getString(1), row.getString(2), row.getLong(3));
        }

        if (session != null) {
            // invalid session?
            if (session.getLast_access() < (System.currentTimeMillis() - Session.SESSION_TIMEOUT_IN_MS)) {
                logger.info("session timed out");
                cluster.deleteOne("user_session_by_email", whereSet);
                {
                    HashMap<String, Object> whereSet2 = new HashMap<>();
                    whereSet2.put("id", session.getId());
                    cluster.deleteOne("user_session", whereSet2);
                }
                return null;
            }
            // refresh the valid session
            session.setLast_access(System.currentTimeMillis());
            insertSession(session);
        }
        return session;
    }

    @Override
    public void clearSession(UUID sessionID) {
        Session session = getSession(sessionID);
        if (session != null) {
            {
                HashMap<String, Object> whereSet = new HashMap<>();
                whereSet.put("id", sessionID);
                cluster.deleteOne("user_session", whereSet);
            }
            {
                HashMap<String, Object> whereSet = new HashMap<>();
                whereSet.put("email", session.getEmail());
                cluster.deleteOne("user_session_by_email", whereSet);
            }
        }
    }

    // user_activate_account
    // email text, activation_id uuid, created bigint,
    public UUID createAccountActivation(String email) throws ApplicationException {
        User user = readUser(email);
        if (user == null) {
            throw new ApplicationException("no such user: " + email);
        }
        UUID activationID = UUID.randomUUID();
        Statement statement = cluster.insert("user_activate_account")
                .value("email", email)
                .value("activation_id", activationID)
                .value("created", System.currentTimeMillis());
        cluster.executeWithRetry(statement);
        return activationID;
    }

    public UUID getAccountActivation(String email) {
        HashMap<String, Object> whereSet = new HashMap<>();
        whereSet.put("email", email);
        Row row = cluster.selectOne("user_activate_account", new String[]{"activation_id"}, whereSet);
        if (row != null) {
            return row.getUUID(0);
        }
        return null;
    }

    @Override
    public void confirmAccount(String email)
            throws ApplicationException, NoSuchAlgorithmException, UnsupportedEncodingException {
        User user = this.readUser(email);
        if (user != null) {
            user.setConfirmed(true);
            this.updateUser(user, null);
        } else {
            throw new ApplicationException("user does not exist: " + email);
        }
    }

    // user_password_reset
    // email text, reset_id uuid, created bigint,
    public UUID createPasswordResetRequest(String email) {
        UUID resetID = UUID.randomUUID();
        Statement statement = cluster.insert("user_password_reset")
                .value("email", email)
                .value("reset_id", resetID)
                .value("created", System.currentTimeMillis());
        cluster.executeWithRetry(statement);
        return resetID;
    }

    public UUID getPasswordResetRequest(String email) {
        HashMap<String, Object> whereSet = new HashMap<>();
        whereSet.put("email", email);
        Row row = cluster.selectOne("user_password_reset", new String[]{"reset_id"}, whereSet);
        if (row != null) {
            return row.getUUID(0);
        }
        return null;
    }

    @Override
    public void resetPassword(String email, String newPassword)
            throws ApplicationException, NoSuchAlgorithmException, UnsupportedEncodingException {
        User user = this.readUser(email);
        if (user != null) {
            this.updateUser(user, newPassword);

            // delete the password reset request so it can't be used twice
            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("email", email);
            cluster.deleteOne("user_password_reset", whereSet);

        } else {
            throw new ApplicationException("user does not exist: " + email);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////

    /**
     * check the batch is not overflowing
     */
    private void startIndexBatch() {
        if (cassandraBatch.size() >= MAX_BATCH_SIZE) { // batch overflow?
            saveBatch();
        }
    }

    /**
     * save batch with retry
     */
    private void saveBatch() {
        try {
            // setup a new batch
            Batch batch = QueryBuilder.batch();
            cassandraBatch.forEach(batch::add);
            cluster.executeWithRetry(batch); // save the batch

        } catch (InvalidQueryException ex) {
            // batch exceeded size or batch error?
            // execute them one-by one instead
            logger.error("batch size exceeded, inserting items individually");
            for (RegularStatement statement : cassandraBatch) {
                try {
                    cluster.executeWithRetry(statement);
                } catch (InvalidQueryException ex2) {
                    logger.error("dropping index " + ex2.getMessage());
                }
            }

        }
        cassandraBatch.clear();
    }

    @Override
    public synchronized void addIndex(UUID organisation_id, Index index) {
        if (organisation_id != null && index != null && index.getWord() != null &&
                index.getUrl() != null && index.getMeta_data() != null) {

            startIndexBatch(); // check init and batch size

            // these can never be null - since they're part of the indexes
            if (index.getWord_origin() == null) {
                index.setWord_origin("");
            }

            // add a new item to the batches for indexing
            // organisation_id uuid, word text, word_origin text, url text, meta_data text, offset int,
            cassandraBatch.add(cluster.insert("word_index")
                    .value("organisation_id", organisation_id)
                    .value("word", index.getWord().toLowerCase())
                    .value("word_origin", index.getWord_origin())
                    .value("shard", index.getShard())
                    .value("synset", index.getSynset())
                    .value("url", index.getUrl())
                    .value("tag", index.getTag())
                    .value("meta_data", index.getMeta_data())
                    .value("acl_hash", index.getAcl_hash())
                    .value("meta_c_type", index.getMeta_c_type())
                    .value("offset", index.getOffset()));

            // organisation_id uuid, url text, word text,
            cassandraBatch.add(cluster.insert("word_unindex")
                    .value("organisation_id", organisation_id)
                    .value("url", index.getUrl())
                    .value("shard", index.getShard())
                    .value("meta_data", index.getMeta_data())
                    .value("word", index.getWord().toLowerCase()));
        }
    }

    @Override
    public synchronized void removeIndex(UUID organisation_id, String url, String metadata) {
        if (organisation_id != null && url != null && url.length() > 0) {

            // fetch the words that are to be deleted and put them in a list
            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);
            whereSet.put("url", url);
            whereSet.put("meta_data", metadata);
            ResultSet resultSet = cluster.selectAll("word_unindex", new String[]{"word", "shard"}, whereSet);
            if (resultSet != null) {
                List<String> wordList = new ArrayList<>();
                IntList shardList = new IntList();
                for (Row row : resultSet.all()) {
                    wordList.add(row.getString(0));
                    shardList.add(row.getInt(1));
                }

                // do the deletes from the indexes
                for (int i = 0; i < wordList.size(); i++) {
                    whereSet = new HashMap<>();
                    whereSet.put("word", wordList.get(i));
                    whereSet.put("shard", shardList.get(i));
                    whereSet.put("url", url);
                    whereSet.put("organisation_id", organisation_id);
                    whereSet.put("meta_data", metadata);
                    cluster.deleteOne("word_index", whereSet);
                }

            } // if resultSet
        } // if valid parameters
    }

    @Override
    public List<Index> readIndex(UUID organisation_id, String word, int shard, String metadata) {
        if (word != null && organisation_id != null) {
            List<Index> indexList = new ArrayList<>();
            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);
            whereSet.put("word", word.toLowerCase());
            whereSet.put("shard", shard);
            whereSet.put("meta_data", metadata);
            ResultSet resultSet = cluster.selectAll("word_index",
                    new String[]{"url", "word_origin", "synset", "meta_data", "acl_hash", "meta_c_type", "tag", "offset"}, whereSet);
            if (resultSet != null) {
                for (Row row : resultSet) {
                    indexList.add(
                            new Index(row.getString(0), word, shard, row.getString(1), row.getInt(2),
                                    row.getString(3), row.getInt(4), row.getInt(5), row.getString(6), row.getInt(7)));
                }
                if (indexList.size() > 0) {
                    return indexList;
                }
            }
        }
        return null;
    }

    @Override
    public synchronized void flushIndexes() {
        if (cassandraBatch.size() > 0) {
            saveBatch();
        }
    }

    @Override
    public synchronized void indexEmotion(UUID organisation_id, String url, int sentence_id, double value, int acl_hash) {
        startIndexBatch(); // check init and batch size

        // add a new item to the batches for indexing
        // organisation_id uuid, word text, word_origin text, url text, meta_data text, offset int,
        cassandraBatch.add(cluster.insert("emotional_index")
                .value("organisation_id", organisation_id)
                .value("url", url)
                .value("sentence_id", sentence_id)
                .value("value", value)
                .value("acl_hash", acl_hash));
    }

    @Override
    public EmotionalSet getEmotionSet(UUID organisation_id, String url) {
        if (url != null && organisation_id != null) {
            EmotionalSet emotionalSet = new EmotionalSet(organisation_id, url);
            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);
            whereSet.put("url", url);
            ResultSet resultSet = cluster.selectAll("emotional_index",
                    new String[]{"sentence_id", "value", "acl_hash"}, whereSet);
            if (resultSet != null) {
                for (Row row : resultSet) {
                    emotionalSet.getEmotional_list().add(new EmotionalItem(row.getInt(0), row.getDouble(1), row.getInt(2)));
                }
            }
            return emotionalSet;
        }
        return null;
    }

    /**
     * index a set of times
     *
     * @param organisation_id the organisation
     * @param dateTimeList    a complete set of indexes
     */
    @Override
    public void addTimeIndexes(UUID organisation_id, List<TimeIndex> dateTimeList) {
        if (dateTimeList != null && organisation_id != null) {

            Batch batch = QueryBuilder.batch();
            int numItemsInBatch = 0;

            for (TimeIndex timeIndex : dateTimeList) {

                DateTime dateTime = new DateTime(timeIndex.getDate_time());

                // organisation_id uuid, url text, entity_id uuid, entity_name text, year int, month int, day int, hour int
                Insert statement = cluster.insert("time_index")
                        .value("organisation_id", organisation_id)
                        .value("url", timeIndex.getUrl())
                        .value("year", dateTime.getYear())
                        .value("month", dateTime.getMonthOfYear())
                        .value("day", dateTime.getDayOfMonth())
                        .value("hour", dateTime.getHourOfDay())
                        .value("acl_hash", timeIndex.getAcl_hash())
                        .value("offset", timeIndex.getOffset());

                batch.add(statement);

                numItemsInBatch++;
                if (numItemsInBatch > MAX_BATCH_SIZE) {
                    cluster.executeWithRetry(batch);
                    numItemsInBatch = 0;
                    batch = QueryBuilder.batch();
                }

            } // for each index

            // done!
            if (numItemsInBatch > 0) {
                cluster.executeWithRetry(batch);
            }

        } // if parameters valid
    }

    /**
     * return all time indexes for the where clause
     *
     * @param whereSet the where clause
     * @return the set of indexes for that clause
     */
    private List<TimeIndex> readTimeIndexes(Map<String, Object> whereSet) {
        // organisation_id uuid, url text, year int, month int, day int, hour int
        ResultSet resultSet = cluster.selectAll("time_index",
                new String[]{"year", "month", "day", "hour", "url", "acl_hash", "offset"}, whereSet);

        if (resultSet != null) {

            List<TimeIndex> indexList = new ArrayList<>();
            for (Row row : resultSet) {

                int readYear = row.getInt(0);
                int readMonth = row.getInt(1);
                int readDay = row.getInt(2);
                int readHour = row.getInt(3);
                String url = row.getString(4);
                int acl_hash = row.getInt(5);
                int offset = row.getInt(6);

                DateTime dateTime = new DateTime(readYear, readMonth, readDay, readHour, 0);

                indexList.add(new TimeIndex(url, offset, dateTime.toDate().getTime(), acl_hash));
            }
            if (indexList.size() > 0) {
                return indexList;
            }
        }
        return null;
    }

    /**
     * read all time indexes for the specified parameters
     *
     * @param organisation_id the organisation to read from
     * @param year            the year
     * @param month           the month of the year
     * @return a list of indexes (or null if dne) for the specified range
     */
    public List<TimeIndex> readTimeIndexes(UUID organisation_id, int year, int month) {
        if (organisation_id != null) {
            Map<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);
            whereSet.put("year", year);
            whereSet.put("month", month);
            return readTimeIndexes(whereSet);
        }
        return null;
    }

    /**
     * read all time indexes for the specified parameters
     *
     * @param organisation_id the organisation to read from
     * @param year            the year
     * @param month           the month of the year
     * @param day             the day of the month
     * @return a list of indexes (or null if dne) for the specified range
     */
    public List<TimeIndex> readTimeIndexes(UUID organisation_id, int year, int month, int day) {
        if (organisation_id != null) {
            Map<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);
            whereSet.put("year", year);
            whereSet.put("month", month);
            whereSet.put("day", day);
            return readTimeIndexes(whereSet);
        }
        return null;
    }

    /**
     * read all time indexes for the specified parameters
     *
     * @param organisation_id the organisation to read from
     * @param year            the year
     * @param month           the month of the year
     * @param day             the day of the month
     * @param hour            the hour of the day
     * @return a list of indexes (or null if dne) for the specified range
     */
    public List<TimeIndex> readTimeIndexes(UUID organisation_id, int year, int month, int day, int hour) {
        if (organisation_id != null) {
            Map<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);
            whereSet.put("year", year);
            whereSet.put("month", month);
            whereSet.put("day", day);
            whereSet.put("hour", hour);
            return readTimeIndexes(whereSet);
        }
        return null;
    }

    /**
     * read all time indexes for the specified parameters
     *
     * @param organisation_id the organisation to read from
     * @param year            the year
     * @param month           the month of the year
     * @param day             the day of the month
     * @param hour            the hour of the day
     * @param minute          the minute of the day
     * @return a list of indexes (or null if dne) for the specified range
     */
    public List<TimeIndex> readTimeIndexes(UUID organisation_id, int year, int month, int day, int hour, int minute) {
        if (organisation_id != null) {
            Map<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);
            whereSet.put("year", year);
            whereSet.put("month", month);
            whereSet.put("day", day);
            whereSet.put("hour", hour);
            whereSet.put("minute", minute);
            return readTimeIndexes(whereSet);
        }
        return null;
    }


    /////////////////////////////////////////////////////////////////////////////////////////
    // hazelcast store items


    public Long hazelcastWordMapLoad(String key) {
        if (key != null) {
            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("word", key);
            Row row = cluster.selectOne("word_count", new String[]{"count"}, whereSet);
            if (row != null) {
                return row.getLong(0);
            }
        }
        return null;
    }

    public List<String> hazelcastWordMapLoadAllKeys() {
        List<String> keyList = new ArrayList<>();
        Statement statement = QueryBuilder.select(new String[]{"word"})
                .from("word_count");
        ResultSet results = cluster.executeWithRetry(statement);
        if (results != null) {
            for (Row row : results) {
                keyList.add(row.getString(0));
            }
        }
        return keyList;
    }

    public Map<String, Long> hazelcastWordMapLoadAll(Collection<String> keys) {
        HashSet<String> hitSet = new HashSet<>();
        hitSet.addAll(keys);
        Map<String, Long> set = new HashMap<>();
        Statement statement = QueryBuilder.select(new String[]{"word", "count"})
                .from("word_count");
        ResultSet results = cluster.executeWithRetry(statement);
        if (results != null) {
            for (Row row : results) {
                String wordStr = row.getString(0);
                if (hitSet.contains(wordStr)) {
                    long count = row.getLong(1);
                    set.put(wordStr, count);
                }
            }
        }
        return set;
    }

    public void hazelcastWordMapStore(String key, Long value) {
        Insert statement = cluster.insert("word_count")
                .value("word", key)
                .value("count", value);
        cluster.executeWithRetry(statement);
    }

    public void hazelcastWordMapStoreAll(Map<String, Long> map) {
        Batch batch = QueryBuilder.batch();
        int numItemsInBatch = 0;
        for (String key : map.keySet()) {

            Insert statement = cluster.insert("word_count")
                    .value("word", key)
                    .value("count", map.get(key));

            batch.add(statement);

            numItemsInBatch++;
            if (numItemsInBatch > MAX_BATCH_SIZE) {
                cluster.executeWithRetry(batch);
                numItemsInBatch = 0;
                batch = QueryBuilder.batch();
            }

        } // for each index
        // done!
        if (numItemsInBatch > 0) {
            cluster.executeWithRetry(batch);
        }
    }

    public void hazelcastWordMapDelete(String key) {
        HashMap<String, Object> whereSet = new HashMap<>();
        whereSet.put("word", key);
        cluster.deleteOne("word_count", whereSet);
    }

    public void hazelcastWordMapDeleteAll(Collection<String> keys) {
        keys.forEach(this::hazelcastWordMapDelete);
    }


    public List<String> hazelcastAclMapLoad(Integer key) {
        if (key != null) {
            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("hash", key);
            Row row = cluster.selectOne("acl_set", new String[]{"acl_list"}, whereSet);
            if (row != null) {
                return row.getList(0, String.class);
            }
        }
        return null;
    }

    public IntList hazelcastAclMapLoadAllKeys() {
        IntList keyList = new IntList();
        Statement statement = QueryBuilder.select(new String[]{"hash"})
                .from("acl_set");
        ResultSet results = cluster.executeWithRetry(statement);
        if (results != null) {
            for (Row row : results) {
                keyList.add(row.getInt(0));
            }
        }
        return keyList;
    }

    public Map<Integer, List<String>> hazelcastAclMapLoadAll(Collection<Integer> keys) {
        HashSet<Integer> hitSet = new HashSet<>();
        hitSet.addAll(keys);
        Map<Integer, List<String>> set = new HashMap<>();
        Statement statement = QueryBuilder.select(new String[]{"hash", "acl_list"})
                .from("acl_set");
        ResultSet results = cluster.executeWithRetry(statement);
        if (results != null) {
            for (Row row : results) {
                Integer key = row.getInt(0);
                if (hitSet.contains(key)) {
                    List<String> value = row.getList(1, String.class);
                    set.put(key, value);
                }
            }
        }
        return set;
    }

    public void hazelcastAclMapStore(Integer key, List<String> value) {
        Insert statement = cluster.insert("acl_set")
                .value("hash", key)
                .value("acl_list", value);
        cluster.executeWithRetry(statement);
    }

    public void hazelcastAclMapStoreAll(Map<Integer, List<String>> map) {
        Batch batch = QueryBuilder.batch();
        int numItemsInBatch = 0;
        for (Integer key : map.keySet()) {

            Insert statement = cluster.insert("acl_set")
                    .value("hash", key)
                    .value("acl_list", map.get(key));

            batch.add(statement);

            numItemsInBatch++;
            if (numItemsInBatch > MAX_BATCH_SIZE) {
                cluster.executeWithRetry(batch);
                numItemsInBatch = 0;
                batch = QueryBuilder.batch();
            }

        } // for each index
        // done!
        if (numItemsInBatch > 0) {
            cluster.executeWithRetry(batch);
        }
    }

    public void hazelcastAclMapDelete(Integer key) {
        HashMap<String, Object> whereSet = new HashMap<>();
        whereSet.put("hash", key);
        cluster.deleteOne("acl_set", whereSet);
    }

    public void hazelcastAclMapDeleteAll(Collection<Integer> keys) {
        keys.forEach(this::hazelcastAclMapDelete);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    // document action persistence (queue)

    @Override
    public DocumentAction hazelcastDocumentActionLoad(Long key) {
        if (key != null) {
            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("key", key);
            Row row = cluster.selectOne("document_queue", new String[]{"organisation_id", "url"}, whereSet);
            if (row != null) {
                return new DocumentAction(row.getUUID(0), row.getString(1));
            }
        }
        return null;
    }

    @Override
    public Set<Long> hazelcastDocumentActionLoadAllKeys() {
        Set<Long> keySet = new HashSet<Long>();
        Statement statement = QueryBuilder.select(new String[]{"key"})
                .from("document_queue");
        ResultSet results = cluster.executeWithRetry(statement);
        if (results != null) {
            for (Row row : results) {
                keySet.add(row.getLong(0));
            }
        }
        return keySet;
    }

    @Override
    public Map<Long, DocumentAction> hazelcastDocumentActionLoadAll(Collection<Long> keys) {
        HashSet<Long> hitSet = new HashSet<>();
        hitSet.addAll(keys);
        Map<Long, DocumentAction> set = new HashMap<>();
        Statement statement = QueryBuilder.select(new String[]{"key", "organisation_id", "url"})
                .from("document_queue");
        ResultSet results = cluster.executeWithRetry(statement);
        if (results != null) {
            for (Row row : results) {
                Long key = row.getLong(0);
                if (hitSet.contains(key)) {
                    UUID organisation_id = row.getUUID(1);
                    String url = row.getString(2);
                    set.put(key, new DocumentAction(organisation_id, url));
                }
            }
        }
        return set;
    }

    @Override
    public void hazelcastDocumentActionStore(Long key, DocumentAction value) {
        Insert statement = cluster.insert("document_queue")
                .value("key", key)
                .value("organisation_id", value.getOrganisation_id())
                .value("url", value.getUrl());
        cluster.executeWithRetry(statement);
    }

    @Override
    public void hazelcastDocumentActionStoreAll(Map<Long, DocumentAction> map) {
        Batch batch = QueryBuilder.batch();
        int numItemsInBatch = 0;
        for (Long key : map.keySet()) {

            DocumentAction da = map.get(key);
            Insert statement = cluster.insert("document_queue")
                    .value("key", key)
                    .value("organisation_id", da.getOrganisation_id())
                    .value("url", da.getUrl());

            batch.add(statement);

            numItemsInBatch++;
            if (numItemsInBatch > MAX_BATCH_SIZE) {
                cluster.executeWithRetry(batch);
                numItemsInBatch = 0;
                batch = QueryBuilder.batch();
            }

        } // for each index

        if (numItemsInBatch > 0) {
            cluster.executeWithRetry(batch);
        }
    }

    @Override
    public void hazelcastDocumentActionDelete(Long key) {
        HashMap<String, Object> whereSet = new HashMap<>();
        whereSet.put("key", key);
        cluster.deleteOne("document_queue", whereSet);
    }

    @Override
    public void hazelcastDocumentActionDeleteAll(Collection<Long> keys) {
        keys.forEach(this::hazelcastDocumentActionDelete);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * get paginated anomaly set
     *
     * @param organisation_id the organisation to get it for
     * @param prevUrl         the previous url paginator
     * @param pageSize        the size of the pages
     * @return a list of urls
     */
    public List<String> getDocumentAnomaliesPaginated(UUID organisation_id, String prevUrl, int pageSize) {
        Statement statement;
        if (prevUrl != null) {
            statement = QueryBuilder.select(new String[]{"url"})
                    .from("document_anomaly")
                    .where(QueryBuilder.eq("organisation_id", organisation_id))
                    .and(QueryBuilder.gt("url", prevUrl));
        } else {
            statement = QueryBuilder.select(new String[]{"url"})
                    .from("document_anomaly")
                    .where(QueryBuilder.eq("organisation_id", organisation_id));
        }
        statement.setFetchSize(pageSize);
        ResultSet results = cluster.executeWithRetry(statement);
        List<String> urlList = new ArrayList<>();
        if (results != null) {
            for (Iterator<Row> iterator = results.iterator(); iterator.hasNext() && urlList.size() < pageSize; ) {
                Row row = iterator.next();
                urlList.add(row.getString(0));
            }
        }
        return urlList;
    }

    /**
     * save a list of url anomaly items
     *
     * @param organisation_id the organisation they belong to
     * @param urlList         list of urls
     */
    public void saveDocumentAnomalies(UUID organisation_id, List<String> urlList) {
        Batch batch = QueryBuilder.batch();
        int numItemsInBatch = 0;
        for (String url : urlList) {

            // organisation_id uuid, url text, entity_id uuid, entity_name text, year int, month int, day int, hour int
            Insert statement = cluster.insert("document_anomaly")
                    .value("organisation_id", organisation_id)
                    .value("url", url);
            batch.add(statement);
            numItemsInBatch++;
            if (numItemsInBatch > MAX_BATCH_SIZE) {
                cluster.executeWithRetry(batch);
                numItemsInBatch = 0;
                batch = QueryBuilder.batch();
            }

        } // for each index

        // done!
        if (numItemsInBatch > 0) {
            cluster.executeWithRetry(batch);
        }
    }

    // return a document's index count for maintaining statistics
    public long getDocumentIndexCount(UUID organisation_id, String url) {
        if (organisation_id != null && url != null) {
            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);
            whereSet.put("url", url);
            Row row = cluster.selectOne("url_index_count", new String[]{"index_count"}, whereSet);
            if (row != null) {
                return row.getLong(0);
            }
        }
        return 0;
    }

    // return a document's index count for maintaining statistics
    public void setDocumentIndexCount(UUID organisation_id, String url, long index_count) {
        if (organisation_id != null && url != null) {
            Insert statement = cluster.insert("url_index_count")
                    .value("organisation_id", organisation_id)
                    .value("url", url)
                    .value("index_count", index_count);
            cluster.executeWithRetry(statement);
        }
    }

    // return a document's index count for maintaining statistics
    public void deleteDocumentIndexCount(UUID organisation_id, String url) {
        if (organisation_id != null && url != null) {
            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);
            whereSet.put("url", url);
            cluster.deleteOne("url_index_count", whereSet);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // cluster

    public void saveCluster(UUID organisation_id, int id, kMeansCluster clusterData) throws IOException {
        if (organisation_id != null && clusterData != null && id >= 0) {

            // save them separately
            List<kMeansValue> valueList = clusterData.getClusterContents();
            clusterData.setCluster_id(id);
            clusterData.setNumEntries(valueList.size());
            clusterData.setClusterContents(null);

            // save the bare min data
            {
                ObjectMapper mapper = new ObjectMapper();
                String jsonStr = mapper.writeValueAsString(clusterData);

                Insert statement = cluster.insert("cluster_data")
                        .value("organisation_id", organisation_id)
                        .value("cluster", id)
                        .value("json_data", jsonStr);

                cluster.executeWithRetry(statement);
            }

            // save the cluster's urls separately
            {
                ObjectMapper mapper = new ObjectMapper();
                clusterData.setClusterContents(valueList); // put data back
                String jsonStr = mapper.writeValueAsString(clusterData);

                Insert statement = cluster.insert("cluster_contents")
                        .value("organisation_id", organisation_id)
                        .value("cluster", id)
                        .value("json_data", jsonStr);

                cluster.executeWithRetry(statement);
            }

        }
    }

    // get cluster by id (# 1..20) - the full single set urls and all
    public kMeansCluster loadFullClusterItem(UUID organisation_id, int id) throws IOException {
        if (organisation_id != null && id >= 0) {
            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);
            whereSet.put("cluster", id);
            Row row = cluster.selectOne("cluster_contents",
                    new String[]{"json_data"},
                    whereSet);

            if (row != null) {
                String jsonStr = row.getString(0);
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(jsonStr, kMeansCluster.class);
            }

        }
        return null;
    }

    // get cluster by id (# 1..20) - load a summary item set (faster, no urls)
    public kMeansCluster loadSummaryClusterItem(UUID organisation_id, int id) throws IOException {
        if (organisation_id != null && id >= 0) {
            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);
            whereSet.put("cluster", id);
            Row row = cluster.selectOne("cluster_data",
                    new String[]{"json_data"},
                    whereSet);

            if (row != null) {
                String jsonStr = row.getString(0);
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(jsonStr, kMeansCluster.class);
            }

        }
        return null;
    }


    public long getClusterLastClustered(UUID organisation_id) {
        if (organisation_id != null) {
            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);
            Row row = cluster.selectOne("cluster_up_to_date",
                    new String[]{"last_clustered"},
                    whereSet);

            if (row != null) {
                return row.getLong(0);
            }

        }
        return 0L;
    }

    public void setClusterLastClustered(UUID organisation_id, long dateTime) {
        if (organisation_id != null) {
            Insert statement = cluster.insert("cluster_up_to_date")
                    .value("organisation_id", organisation_id)
                    .value("last_clustered", dateTime);

            cluster.executeWithRetry(statement);
        }
    }

    // when did the system last have a change that required re-clustering of the data?
    public long getClusterLastChange(UUID organisation_id) {
        if (organisation_id != null) {
            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);
            Row row = cluster.selectOne("cluster_up_to_date",
                    new String[]{"last_change"},
                    whereSet);

            if (row != null) {
                return row.getLong(0);
            }

        }
        return 0L;
    }

    public void setClusterLastChange(UUID organisation_id, long dateTime) {
        if (organisation_id != null) {
            Insert statement = cluster.insert("cluster_up_to_date")
                    .value("organisation_id", organisation_id)
                    .value("last_change", dateTime);

            cluster.executeWithRetry(statement);
        }
    }

    // when did the system last have a change that required re-clustering of the data?
    public long getCosineLastChange(UUID organisation_id) {
        if (organisation_id != null) {
            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);
            Row row = cluster.selectOne("cluster_up_to_date",
                    new String[]{"last_cosine_change"},
                    whereSet);

            if (row != null) {
                return row.getLong(0);
            }

        }
        return 0L;
    }

    public void setCosineLastChange(UUID organisation_id, long dateTime) {
        if (organisation_id != null) {
            Insert statement = cluster.insert("cluster_up_to_date")
                    .value("organisation_id", organisation_id)
                    .value("last_cosine_change", dateTime);

            cluster.executeWithRetry(statement);
        }
    }

    // save a document's emotion status if it has exceeded a certain threshold
    public void setDocumentEmotion(UUID organisation_id, String url, int positive_sentence_id, double positive,
                                   int negative_sentence_id, double negative) {
        if (organisation_id != null && url != null) {
            if (positive != 0.0) {
                Insert statement = cluster.insert("document_emotion")
                        .value("organisation_id", organisation_id)
                        .value("url", url)
                        .value("sentence_id", positive_sentence_id)
                        .value("score", positive);

                cluster.executeWithRetry(statement);
            }
            if (negative != 0.0) {
                Insert statement = cluster.insert("document_emotion")
                        .value("organisation_id", organisation_id)
                        .value("url", url)
                        .value("sentence_id", negative_sentence_id)
                        .value("score", negative);

                cluster.executeWithRetry(statement);
            }
        }
    }

    /**
     * return the emotions in either negative / positive first ordering
     *
     * @param organisation_id the organisation to do it for
     * @param positive        if true, return the most positive -> negative, otherwise reverse
     * @return a list of urls ranked by emotion
     */
    public List<UrlValue> getDocumentEmotion(UUID organisation_id, boolean positive, int offset, int pageSize) {
        List<UrlValue> resultList = new ArrayList<>();
        Statement statement;
        // organisation_id uuid, id uuid, name text, isa text
        if (positive) {
            statement = QueryBuilder.select(new String[]{"organisation_id", "url", "score", "sentence_id"})
                    .from("document_emotion")
                    .where(QueryBuilder.eq("organisation_id", organisation_id))
                    .orderBy(QueryBuilder.desc("score"));
        } else {
            statement = QueryBuilder.select(new String[]{"organisation_id", "url", "score", "sentence_id"})
                    .from("document_emotion")
                    .where(QueryBuilder.eq("organisation_id", organisation_id))
                    .orderBy(QueryBuilder.asc("score"));
        }

        ResultSet results = cluster.executeWithRetry(statement);
        if (results != null) {
            int index = 0;
            for (Iterator<Row> iterator = results.iterator(); iterator.hasNext() && resultList.size() < pageSize; ) {
                Row row = iterator.next();
                if (offset <= index && index < (offset + pageSize)) {
                    resultList.add(new UrlValue(row.getString(1), row.getDouble(2), row.getInt(3)));
                }
                index++;
            }
        }
        return resultList;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // rules

    public void saveRule(UUID organisation_id, String rule_name, RuleItem item) throws IOException {
        if (organisation_id != null && rule_name != null && item != null) {

            ObjectMapper mapper = new ObjectMapper();
            String jsonStr = mapper.writeValueAsString(item);

            Insert statement = cluster.insert("rules")
                    .value("organisation_id", organisation_id)
                    .value("rule_name", rule_name)
                    .value("json", jsonStr);

            cluster.executeWithRetry(statement);
        }
    }

    public RuleItem loadRuleByName(UUID organisation_id, String rule_name) throws IOException {
        if (organisation_id != null && rule_name != null) {

            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);
            whereSet.put("rule_name", rule_name);
            Row row = cluster.selectOne("rules",
                    new String[]{"json"},
                    whereSet);

            if (row != null) {
                String jsonStr = row.getString(0);
                if (jsonStr != null) {
                    ObjectMapper mapper = new ObjectMapper();
                    RuleItem item = mapper.readValue(jsonStr, RuleItem.class);
                    if (item != null) {
                        item.setOrganisation_id(organisation_id);
                    }
                    return item;
                }
            }

        }
        return null;
    }

    public void deleteRule(UUID organisation_id, String rule_name) {
        HashMap<String, Object> whereSet = new HashMap<>();
        whereSet.put("organisation_id", organisation_id);
        whereSet.put("rule_name", rule_name);
        cluster.deleteOne("rules", whereSet);
    }


    public List<RuleItem> getRuleList(UUID organisation_id, String prevRule, int pageSize) throws IOException {
        Statement statement;
        // organisation_id uuid, id uuid, name text, isa text
        if (prevRule != null) {
            statement = QueryBuilder.select(new String[]{"json"})
                    .from("rules")
                    .where(QueryBuilder.eq("organisation_id", organisation_id))
                    .and(QueryBuilder.gt("prevRule", prevRule));
        } else {
            statement = QueryBuilder.select(new String[]{"json"})
                    .from("rules")
                    .where(QueryBuilder.eq("organisation_id", organisation_id));
        }

        statement.setFetchSize(pageSize);

        ObjectMapper mapper = new ObjectMapper();
        ResultSet results = cluster.executeWithRetry(statement);
        List<RuleItem> ruleList = new ArrayList<>();
        if (results != null) {
            for (Iterator<Row> iterator = results.iterator(); iterator.hasNext() && ruleList.size() < pageSize; ) {
                Row row = iterator.next();
                RuleItem item = mapper.readValue(row.getString(0), RuleItem.class);
                item.setOrganisation_id(organisation_id);
                ruleList.add(item);
            }
        }
        return ruleList;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // report access

    public Report createReport(UUID organisation_id, Report report) {
        if (organisation_id != null && report != null && report.getReport_name() != null) {
            report.setOrganisation_id(organisation_id);
            Insert statement = cluster.insert("reports")
                    .value("organisation_id", organisation_id)
                    .value("report_name", report.getReport_name())
                    .value("creator", report.getCreator())
                    .value("report_id", report.getReport_id())
                    .value("last_run", report.getLast_run());

            cluster.executeWithRetry(statement);
        }
        return report;
    }

    public Report readReport(UUID organisation_id, String report_name) {
        HashMap<String, Object> whereSet = new HashMap<>();
        whereSet.put("organisation_id", organisation_id);
        whereSet.put("report_name", report_name);
        Row row = cluster.selectOne("reports",
                new String[]{"creator", "report_id", "last_run"},
                whereSet);

        if (row != null) {
            return new Report(organisation_id, report_name, row.getString(0), row.getInt(1), row.getLong(2));
        }
        return null;
    }

    public List<Report> readReportList(UUID organisation_id) {
        HashMap<String, Object> whereSet = new HashMap<>();
        whereSet.put("organisation_id", organisation_id);
        ResultSet resultSet = cluster.selectAll("reports", new String[]{"report_name", "creator", "report_id", "last_run"}, whereSet);
        if (resultSet != null) {
            List<Report> reportList = new ArrayList<>();
            for (Row row : resultSet) {
                reportList.add(new Report(organisation_id, row.getString(0), row.getString(1), row.getInt(2), row.getLong(3)));
            }
            return reportList;
        }
        return null;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // neural networks

    /**
     * add a training example to the neural network learning system
     *
     * @param organisation_id the organisation of the system
     * @param word            the word to train
     * @param synset_id       its synset id
     * @param training_sample the data
     * @throws IOException exception
     */
    public void addNNetTrainingSample(UUID organisation_id, String word, int synset_id,
                                      NNetTrainingSample training_sample) throws IOException {
        if (organisation_id != null && word != null && synset_id >= 0 && training_sample != null &&
                training_sample.getId() != null && training_sample.getTraining_data() != null) {
            String jsonStr = new ObjectMapper().writeValueAsString(training_sample);
            // organisation_id uuid, word text, synset_id int, training_data_id uuid, training_data text
            Statement insert = cluster.insert("nnet_training")
                    .values(new String[]{
                                    "organisation_id", "word", "synset_id", "training_data_id", "training_data"},
                            new Object[]{
                                    organisation_id, word, synset_id, training_sample.getId(), jsonStr
                            });
            cluster.executeWithRetry(insert);
        }
    }

    public List<NNetTrainingSample> getNNetTrainingSamples(UUID organisation_id, String word, int synset_id,
                                                           UUID prev_id, int pageSize) throws IOException {

        if (organisation_id != null && word != null && synset_id >= 0 && pageSize > 0) {
            Statement statement;
            // organisation_id uuid, id uuid, name text, isa text
            if (prev_id != null) {
                statement = QueryBuilder.select(new String[]{"training_data"})
                        .from("nnet_training")
                        .where(QueryBuilder.eq("organisation_id", organisation_id))
                        .and(QueryBuilder.eq("word", word))
                        .and(QueryBuilder.eq("synset_id", synset_id))
                        .and(QueryBuilder.gt("training_data_id", prev_id));
            } else {
                statement = QueryBuilder.select(new String[]{"training_data"})
                        .from("nnet_training")
                        .where(QueryBuilder.eq("organisation_id", organisation_id))
                        .and(QueryBuilder.eq("word", word))
                        .and(QueryBuilder.eq("synset_id", synset_id));
            }

            statement.setFetchSize(pageSize);

            ResultSet results = cluster.executeWithRetry(statement);
            List<NNetTrainingSample> trainingSetList = new ArrayList<>();
            if (results != null) {
                for (Iterator<Row> iterator = results.iterator(); iterator.hasNext() && trainingSetList.size() < pageSize; ) {
                    Row row = iterator.next();
                    String jsonStr = row.getString(0);
                    NNetTrainingSample trainingSample = new ObjectMapper().readValue(jsonStr, NNetTrainingSample.class);
                    if (trainingSample != null) {
                        trainingSetList.add(trainingSample);
                    }
                }
            }
            return trainingSetList;
        }
        return null;
    }

    public void saveModel(UUID organisation_id, String word, NNetModelData model) throws IOException {
        if (organisation_id != null && word != null && model != null) {
            String jsonStr = new ObjectMapper().writeValueAsString(model);
            // organisation_id uuid, word text, model_data text
            Statement insert = cluster.insert("nnet_store")
                    .values(new String[]{
                                    "organisation_id", "word", "last_updated", "model_data"},
                            new Object[]{
                                    organisation_id, word, model.getLast_updated(), jsonStr
                            });
            cluster.executeWithRetry(insert);
        }
    }

    public long getModelLastUpdated(UUID organisation_id, String word) throws IOException {
        if (organisation_id != null && word != null) {
            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);
            whereSet.put("word", word);

            Row row = cluster.selectOne("nnet_store",
                    new String[]{"last_updated"},
                    whereSet);

            if (row != null) {
                return row.getLong(0);
            }
        }
        return 0;
    }

    public NNetModelData loadModel(UUID organisation_id, String word) throws IOException {
        if (organisation_id != null && word != null) {

            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);
            whereSet.put("word", word);

            Row row = cluster.selectOne("nnet_store",
                    new String[]{"model_data"},
                    whereSet);

            if (row != null) {
                return new ObjectMapper().readValue(row.getString(0), NNetModelData.class);
            }

        }
        return null;
    }


    /////////////////////////////////////////////////////////////////////////////////////////
    // address book database

    // insert or update an entry
    public void saveKBEntry(KBEntry KBEntry) {
        if ( KBEntry != null && KBEntry.getType() != null &&
                KBEntry.getOrganisation_id() != null ) {
            if (KBEntry.getId() == null) {
                KBEntry.setId(UUID.randomUUID());
            }
            Insert insert = cluster.insert("knowledge_base")
                    .values(new String[]{ "organisation_id",
                                    "id", "type", "origin", "json_data"},
                            new Object[]{
                                    KBEntry.getOrganisation_id(),
                                    KBEntry.getId(), KBEntry.getType(),
                                    KBEntry.getOrigin(), KBEntry.getJson_data()
                            });
            cluster.executeWithRetry(insert);
        }
    }

    // get a list of entries matching name
    public KBEntry getKBEntry(UUID organisation_id, String type, UUID id) {
        if ( id != null && organisation_id != null && type != null ) {
            Statement statement = QueryBuilder.select(new String[]{"origin", "json_data"})
                    .from("knowledge_base")
                    .where(QueryBuilder.eq("id", id))
                    .and(QueryBuilder.eq("type", type))
                    .and(QueryBuilder.eq("organisation_id", organisation_id));

            ResultSet results = cluster.executeWithRetry(statement);
            if (results != null) {
                Iterator<Row> iterator = results.iterator();
                if (iterator.hasNext()) {
                    Row row = iterator.next();
                    return new KBEntry(organisation_id, id, type, row.getString(0), row.getString(1));
                }
            }
        }
        return null;
    }

    // remove an entry using its keys
    public void deleteKBEntry(UUID organisation_id, String type, UUID id) {
        if ( id != null && organisation_id != null && type != null ) {
            HashMap<String, Object> whereSet = new HashMap<>();
            whereSet.put("organisation_id", organisation_id);
            whereSet.put("id", id);
            whereSet.put("type", type);
            cluster.deleteOne("knowledge_base", whereSet);
        }
    }


    /**
     * paginated access to an entity
     *
     * @param organisation_id the organisation of the entity
     * @param type the type of the object to fetch
     * @param prevEntityId    the pagination indicator (or null)
     * @param pageSize        size of the page
     * @return a list of entity items
     */
    @Override
    public List<KBEntry> getEntityList(UUID organisation_id, String type, UUID prevEntityId, int pageSize) {
        Statement statement;
        // organisation_id uuid, id uuid, name text, isa text
        if (prevEntityId != null) {
            statement = QueryBuilder.select(new String[]{"organisation_id",
                    "id", "type", "origin", "json_data"})
                    .from("knowledge_base")
                    .where(QueryBuilder.eq("organisation_id", organisation_id))
                    .and(QueryBuilder.eq("type", type))
                    .and(QueryBuilder.gt("id", prevEntityId));
        } else {
            statement = QueryBuilder.select(new String[]{"organisation_id",
                    "id", "type", "origin", "json_data"})
                    .from("knowledge_base")
                    .where(QueryBuilder.eq("organisation_id", organisation_id))
                    .and(QueryBuilder.eq("type", type));
        }

        statement.setFetchSize(pageSize);

        ResultSet results = cluster.executeWithRetry(statement);
        List<KBEntry> entityList = new ArrayList<>();
        if (results != null) {
            for (Iterator<Row> iterator = results.iterator(); iterator.hasNext() && entityList.size() < pageSize; ) {
                Row row = iterator.next();
                KBEntry entity = new KBEntry(row.getUUID(0), row.getUUID(1), row.getString(2),
                        row.getString(3), row.getString(4));
                entityList.add(entity);
            }
        }
        return entityList;
    }



}





