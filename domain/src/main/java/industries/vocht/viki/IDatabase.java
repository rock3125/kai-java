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

package industries.vocht.viki;

import industries.vocht.viki.datastructures.IntList;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.hazelcast.DocumentAction;
import industries.vocht.viki.model.*;
import industries.vocht.viki.model.knowledge_base.KBEntry;
import industries.vocht.viki.model.emotions.EmotionalSet;
import industries.vocht.viki.model.group.Group;
import industries.vocht.viki.model.indexes.Index;
import industries.vocht.viki.model.indexes.TimeIndex;
import industries.vocht.viki.model.k_means.kMeansCluster;
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Created by peter on 4/03/16.
 *
 */
public interface IDatabase {

    int MAX_BATCH_SIZE = 50; // max batch size
    int MAX_TUPLE_BATCH_SIZE = 2; // max batch size for tuple lists
    String pepper = "0ca5785c-0854-4533-b8f8-89e2dcf82465";

    ///////////////////////////////////////////////////////////////////////////////////////////
    // users
    User createUser(UUID organisation_id, User user, String userPassword)
            throws ApplicationException, NoSuchAlgorithmException, UnsupportedEncodingException;

    User readUser(String email);

    void updateUser(User user, String userPassword) throws NoSuchAlgorithmException, UnsupportedEncodingException;

    void deleteUser(UUID organisation_id, String email);

    UserEmailList readUserList(UUID organisation_id);

    void updateUserList(UUID organisation_id, UserEmailList userEmailList);

    ///////////////////////////////////////////////////////////////////////////////////////////
    // groups
    Group createGroup(UUID organisation_id, Group group);

    Group readGroup(UUID organisation_id, String name);

    List<Group> readAllGroups(UUID organisation_id);

    void updateGroup(UUID organisation_id, Group group);

    void deleteGroup(UUID organisation_id, String name);

    ///////////////////////////////////////////////////////////////////////////////////////////
    // document images
    void saveDocumentImage(UUID organisation_id, String url, byte[] png_data);

    void removeDocumentImage(UUID organisation_id, String url);

    byte[] getDocumentImage(UUID organisation_id, String url);

    ///////////////////////////////////////////////////////////////////////////////////////////
    // documents
    Document createDocument(UUID organisation_id, Document document) throws IOException;

    Document readDocument(UUID organisation_id, String url) throws IOException;

    void updateDocument(UUID organisation_id, Document document) throws IOException;

    void deleteDocument(UUID organisation_id, String url);

    void uploadDocumentBinary(UUID organisation_id, String url, byte[] data);

    byte[] getDocumentBinary(UUID organisation_id, String url);

    Map<String, byte[]> getDocumentParseTreeMap(UUID organisation_id, String url);

    void saveDocumentParseTreeMap(UUID organisation_id, String url, Map<String, byte[]> map);

    void saveDocumentHistogram(UUID organisation_id, String url, CompressedVector compressedVector) throws IOException;

    CompressedVector loadDocumentHistogram(UUID organisation_id, String url) throws IOException;

    void saveDocumentSummarizationWordSet(UUID organisation_id, String url, SummarisationSet fragmentSet) throws IOException;

    void saveDocumentSummarizationSentenceSet(UUID organisation_id, String url, Sentence sentence) throws IOException;

    List<Document> getDocumentList(UUID organisation_id, String prevUrl, int pageSize) throws IOException;

    List<String> getDocumentUrlList(UUID organisation_id, String prevUrl, int limit );

    Map<String, byte[]> getAuthorsForUrlList(UUID organisation_id, List<String> urlList );

    SummarisationSet loadDocumentSummarizationWordSet(UUID organisation_id, String url) throws IOException;

    Sentence loadDocumentSummarizationSentenceSet(UUID organisation_id, String url) throws IOException;

    void saveDocumentSimilarityMap(UUID organisation_id, List<SimilarDocument> similarDocumentList);

    List<SimilarDocument> loadSimilarDocuments(UUID organisation_id, String url);

    // load all relationships
    List<SimilarDocumentSet> loadSimilarDocuments(UUID organisation_id );


    ///////////////////////////////////////////////////////////////////////////////////////////
    // semantic tuples

    // retrieve a list of case tuples by UUID list
    List<Tuple> readTuples(UUID organisation_id, List<UUID> idArray ) throws IOException;

    // write a set of case tuples to the database
    void writeTuple(UUID organisation_id, Tuple tuple) throws IOException;

    // remove case tuples for a document
    void deleteTuplesByUrl(UUID organisation_id, String url);

    // read all case tuples for a given document
    List<Tuple> readTuplesForDocument(UUID organisation_id, String url) throws IOException;

    ///////////////////////////////////////////////////////////////////////////////////////////
    // organisation
    Organisation createOrganisation(Organisation organisation);

    Organisation readOrganisation(String name);

    void updateOrganisation(Organisation organisation);

    String getOrganisationName(UUID id);

    // return a list of all organisations
    List<Organisation> getOrganisationList();

    ///////////////////////////////////////////////////////////////////////////////////////////
    // session management
    Session createSession(String userEmail, String ipAddress);

    void clearSession(UUID sessionID);

    Session getSession(UUID sessionID);

    // retrieve existing session by user's email
    Session getExistingSessionByEmail(String email);

    // active session management
    List<Session> getActiveSessions(UUID organisation_id);
    void createActiveSession(UUID organisation_id, Session session);
    void removeActiveSession(UUID organisation_id, UUID session_id);

    ///////////////////////////////////////////////////////////////////////////////////////////
    // account activation management
    UUID createAccountActivation(String email) throws ApplicationException;

    UUID getAccountActivation(String email);

    void confirmAccount(String email)
            throws ApplicationException, NoSuchAlgorithmException, UnsupportedEncodingException;

    ///////////////////////////////////////////////////////////////////////////////////////////
    // password reset management
    UUID createPasswordResetRequest(String email);

    UUID getPasswordResetRequest(String email);

    void resetPassword(String email, String newPassword)
            throws ApplicationException, NoSuchAlgorithmException, UnsupportedEncodingException;

    ///////////////////////////////////////////////////////////////////////////////////////////
    // indexes
    void addIndex(UUID organisation_id, Index index);

    void removeIndex(UUID organisation_id, String url, String metadata);

    List<Index> readIndex(UUID organisation_id, String word, int shard, String metadata);

    void flushIndexes();

    void indexEmotion(UUID organisation_id, String url, int sentence_id, double value, int acl_hash);

    EmotionalSet getEmotionSet(UUID organisation_id, String url);

    void addTimeIndexes(UUID organisation_id, List<TimeIndex> dateTimeList);

    List<TimeIndex> readTimeIndexes(UUID organisation_id, int year, int month);

    List<TimeIndex> readTimeIndexes(UUID organisation_id, int year, int month, int day);

    List<TimeIndex> readTimeIndexes(UUID organisation_id, int year, int month, int day, int hour);

    List<TimeIndex> readTimeIndexes(UUID organisation_id, int year, int month, int day, int hour, int minute);

    ///////////////////////////////////////////////////////////////////////////////////////////
    // statistics

    // key / offline stats management
    Long hazelcastWordMapLoad(String key);
    List<String> hazelcastWordMapLoadAllKeys();
    Map<String, Long> hazelcastWordMapLoadAll(Collection<String> keys);
    void hazelcastWordMapStore(String key, Long value);
    void hazelcastWordMapStoreAll(Map<String, Long> map);
    void hazelcastWordMapDelete(String key);
    void hazelcastWordMapDeleteAll(Collection<String> keys);

    // key / offline stats management
    List<String> hazelcastAclMapLoad(Integer key);
    IntList hazelcastAclMapLoadAllKeys();
    Map<Integer, List<String>> hazelcastAclMapLoadAll(Collection<Integer> keys);
    void hazelcastAclMapStore(Integer key, List<String> value);
    void hazelcastAclMapStoreAll(Map<Integer, List<String>> map);
    void hazelcastAclMapDelete(Integer key);
    void hazelcastAclMapDeleteAll(Collection<Integer> keys);

    // index counts for documents
    void setDocumentIndexCount(UUID organisation_id, String url, long index_count);
    long getDocumentIndexCount(UUID organisation_id, String url);
    void deleteDocumentIndexCount(UUID organisation_id, String url);

    // anomalies
    List<String> getDocumentAnomaliesPaginated( UUID organisation_id, String prevUrl, int pageSize );
    void saveDocumentAnomalies( UUID organisation_id, List<String> urlList );

    ///////////////////////////////////////////////////////////////////////////////////////////
    // work queue

    // persistent queue management
    DocumentAction hazelcastDocumentActionLoad(Long key);
    Set<Long> hazelcastDocumentActionLoadAllKeys();
    Map<Long, DocumentAction> hazelcastDocumentActionLoadAll(Collection<Long> keys);
    void hazelcastDocumentActionStore(Long key, DocumentAction value);
    void hazelcastDocumentActionStoreAll(Map<Long, DocumentAction> map);
    void hazelcastDocumentActionDelete(Long key);
    void hazelcastDocumentActionDeleteAll(Collection<Long> keys);
    
    ///////////////////////////////////////////////////////////////////////////////////////////
    // cluster

    void saveCluster( UUID organsiation_id, int id, kMeansCluster cluster ) throws IOException;

    kMeansCluster loadFullClusterItem(UUID organisation_id, int id) throws IOException;
    kMeansCluster loadSummaryClusterItem(UUID organisation_id, int id) throws IOException;

    long getClusterLastClustered( UUID organisation_id );
    long getClusterLastChange( UUID organisation_id );
    long getCosineLastChange( UUID organisation_id );

    void setClusterLastClustered( UUID organisation_id, long dateTime );
    void setClusterLastChange( UUID organisation_id, long dateTime );
    void setCosineLastChange( UUID organisation_id, long dateTime );

    // save a document's emotion status if it has exceeded a certain threshold
    void setDocumentEmotion( UUID organisation_id, String url, int positive_sentence_id, double positive,
                             int negative_sentence_id, double negative );
    List<UrlValue> getDocumentEmotion( UUID organisation_id, boolean positive, int offset, int pageSize );


    ///////////////////////////////////////////////////////////////////////////////////////////
    // rules

    void saveRule( UUID organisation_id, String rule_name, RuleItem item ) throws IOException;
    RuleItem loadRuleByName( UUID organisation_id, String rule_name ) throws IOException;
    List<RuleItem> getRuleList(UUID organisation_id, String prevRule, int pageSize) throws IOException;
    void deleteRule( UUID organisation_id, String rule_name );

    ///////////////////////////////////////////////////////////////////////////////////////////
    // reports

    Report createReport(UUID organisation_id, Report report);
    Report readReport(UUID organisation_id, String name);
    List<Report> readReportList(UUID organisation_id);

    ///////////////////////////////////////////////////////////////////////////////////////////
    // neural networks

    void addNNetTrainingSample( UUID organisation_id, String word, int synset_id,
                                NNetTrainingSample training_set ) throws IOException;

    List<NNetTrainingSample> getNNetTrainingSamples( UUID organisation_id, String word, int synset_id,
                                                     UUID prev_id, int pageSize ) throws IOException;

    void saveModel( UUID organisation_id, String word, NNetModelData model ) throws IOException;

    long getModelLastUpdated( UUID organisation_id, String word ) throws IOException;

    NNetModelData loadModel( UUID organisation_id, String word ) throws IOException;

    /////////////////////////////////////////////////////////////////////////////////////////////
    // knowledge base CRUD

    void saveKBEntry(KBEntry KBEntry);

    KBEntry getKBEntry(UUID organisation_id, String type, UUID id);

    void deleteKBEntry(UUID organisation_id, String type, UUID id);

    List<KBEntry> getEntityList(UUID organisation_id, String type, UUID prev, int page_size);

}


