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

package industries.vocht.viki.memory_db;

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
import industries.vocht.viki.utility.Sha256;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by peter on 4/03/16.
 *
 * an in memory database (for unit testing)
 *
 */
public class MemoryDatabase implements IDatabase {

    private static final Logger logger = LoggerFactory.getLogger(MemoryDatabase.class);

    private Map<String, User> userMap;
    private Map<String, Organisation> organisationMap;
    private Map<UUID, String> organisationNameMap;
    private Map<String, Group> groupMap;
    private Map<String, Document> documentMap;
    private Map<String, byte[]> documentBinaryContentMap;
    private Map<String, Map<String, byte[]>> documentParseTreeMap;
    private Map<String, byte[]> documentAuthorMap;
    private Map<UUID, Session> sessionMap;
    private Map<String, Session> activeSessionMap;
    private Map<String, Session> sessionEmailMap;
    private Map<UUID, UserEmailList> userListMap;
    private Map<String, Map<String, Index>> indexMap;
    private Map<String, List<String>> unIndexMap;
    private Map<String, String> documentHistogramMap;
    private Map<String, String> documentSummarisationMap;
    private Map<String, String> documentSummarisationSentenceMap;
    private Map<String, UUID> accountActivationMap;
    private Map<String, UUID> passwordResetMap;
    private Map<String, List<TimeIndex>> timeIndexMap;
    private Map<String, Double> documentSimilarityMap;
    private Map<String, Long> documentIndexCount;
    private Map<String, String> clusterDataMap;
    private Map<UUID, Long> lastClustered;
    private Map<UUID, Long> lastChanged;
    private Map<UUID, Long> lastCosined;
    private Map<String, String> ruleMap;
    private Map<String, String> reportMap;
    private Map<UUID, List<UrlValue>> emotionSet;
    private Map<UUID, List<String>> anomalySet;
    private Map<String, String> nnetTrainingSet;
    private Map<String, String> nnetStore;
    private Map<String, String> caseTupleStore;
    private Map<String, List<UUID>> caseTupleStoreByUrl;
    private Map<String, List<EmotionalItem>> indexEmotionalSet;
    private Map<String, KBEntry> kbSet;

    public MemoryDatabase() {
        userMap = new HashMap<>();
        organisationMap = new HashMap<>();
        organisationNameMap = new HashMap<>();
        groupMap = new HashMap<>();
        documentMap = new HashMap<>();
        sessionMap = new HashMap<>();
        sessionEmailMap = new HashMap<>();
        activeSessionMap = new HashMap<>();
        userListMap = new HashMap<>();
        documentParseTreeMap = new HashMap<>();
        documentAuthorMap = new HashMap<>();
        documentBinaryContentMap = new HashMap<>();
        indexMap = new HashMap<>();
        unIndexMap = new HashMap<>();
        accountActivationMap = new HashMap<>();
        passwordResetMap = new HashMap<>();
        documentHistogramMap = new HashMap<>();
        documentSummarisationMap = new HashMap<>();
        documentSummarisationSentenceMap = new HashMap<>();
        timeIndexMap = new HashMap<>();
        documentSimilarityMap = new HashMap<>();
        documentIndexCount = new HashMap<>();
        clusterDataMap = new HashMap<>();
        lastClustered = new HashMap<>();
        lastChanged = new HashMap<>();
        lastCosined = new HashMap<>();
        ruleMap = new HashMap<>();
        reportMap = new HashMap<>();
        emotionSet = new HashMap<>();
        anomalySet = new HashMap<>();
        nnetTrainingSet = new HashMap<>();
        nnetStore = new HashMap<>();
        caseTupleStore = new HashMap<>();
        caseTupleStoreByUrl = new HashMap<>();
        indexEmotionalSet = new HashMap<>();
        kbSet = new HashMap<>();
    }

    ///////////////////////////////////////////////////////////////////////////

    private String getUserPK(User user) {
        if (user.getEmail() == null) {
            throw new InvalidParameterException("invalid user primary key");
        }
        return getUserPK(user.getEmail());
    }

    private String getUserPK(String email) {
        return email;
    }

    private User copyUser(User user) {
        if (user != null) {
            try {
                String json = new ObjectMapper().writeValueAsString(user);
                return new ObjectMapper().readValue(json, User.class);
            } catch (IOException ex) {
                logger.error("copyUser, json:", ex);
            }
        }
        return null;
    }

    public User createUser(UUID organisation_id, User user, String userPassword)
            throws ApplicationException, NoSuchAlgorithmException, UnsupportedEncodingException {
        user.setId(UUID.randomUUID());
        user.setOrganisation_id(organisation_id);
        user.setSalt(UUID.randomUUID());
        user.setPassword_sha256(new Sha256().generateSha256Password(user.getSalt(), userPassword));
        userMap.put(getUserPK(user), copyUser(user));

        // create activation code for this user
        createAccountActivation(user.getEmail());
        return user;
    }

    /**
     * return a copy of the user object
     *
     * @param email the id of the user
     * @return a user
     */
    public User readUser(String email) {
        return copyUser(userMap.get(getUserPK(email)));
    }

    public void updateUser(User user, String userPassword) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        if (userPassword != null) {
            user.setSalt(UUID.randomUUID());
            user.setPassword_sha256(new Sha256().generateSha256Password(user.getSalt(), userPassword));
        }
        userMap.put(getUserPK(user), copyUser(user));
    }

    public void deleteUser(UUID organisation_id, String email) {
        userMap.remove(getUserPK(email));
        UserEmailList list = readUserList(organisation_id);
        if (list != null) {
            list.remove(email);
            updateUserList(organisation_id, list);
        }
    }

    private UserEmailList copyUserList(UserEmailList userEmailList) {
        if (userEmailList != null) {
            try {
                String json = new ObjectMapper().writeValueAsString(userEmailList);
                return new ObjectMapper().readValue(json, UserEmailList.class);
            } catch (IOException ex){
                logger.error("copyUserList: json", ex);
            }
        }
        return null;
    }

    public UserEmailList readUserList(UUID organisation_id) {
        UserEmailList userEmailList = userListMap.get(organisation_id);
        if (userEmailList == null) {
            userEmailList = new UserEmailList();
        }
        userEmailList.setOrganisation_id(organisation_id);
        return userEmailList;
    }

    public void updateUserList(UUID organisation_id, UserEmailList userEmailList) {
        userListMap.put(organisation_id, copyUserList(userEmailList));
    }

    ///////////////////////////////////////////////////////////////////////////

    private String getGroupPK(Group group) {
        if (group.getOrganisation_id() == null || group.getName() == null) {
            throw new InvalidParameterException("invalid group primary key");
        }
        return getGroupPK(group.getOrganisation_id(), group.getName());
    }

    private String getGroupPK(UUID org, String name) {
        return org.toString() + ":" + name;
    }

    private Group copyGroup(Group group) {
        if (group != null) {
            try {
            String json = new ObjectMapper().writeValueAsString(group);
            return new ObjectMapper().readValue(json, Group.class);
            } catch (IOException ex){
                logger.error("copyGroup: json", ex);
            }
        }
        return null;
    }

    public List<Group> readAllGroups(UUID organisation_id) {
        List<Group> groupList = new ArrayList<>();
        for ( String key : groupMap.keySet() ) {
            Group group = groupMap.get(key);
            if ( group.getOrganisation_id().equals(organisation_id) ) {
                groupList.add(group);
            }
        }
        return groupList;
    }

    public Group createGroup(UUID organisation_id, Group group) {
        group.setOrganisation_id(organisation_id);
        groupMap.put(getGroupPK(group), copyGroup(group));
        return group;
    }

    public Group readGroup(UUID organisation_id, String name) {
        return groupMap.get(getGroupPK(organisation_id, name));
    }

    public void updateGroup(UUID organisation_id, Group group) {
        groupMap.put(getGroupPK(group), copyGroup(group));
    }

    public void deleteGroup(UUID organisation_id, String name) {
        groupMap.remove(getGroupPK(organisation_id, name));
    }

    ///////////////////////////////////////////////////////////////////////////
    // document images

    @Override
    public void saveDocumentImage(UUID organisation_id, String url, byte[] png_data) {
        if ( organisation_id != null && url != null && png_data != null && png_data.length > 0 ) {
            throw new InvalidParameterException("todo: implement");
        }
    }

    @Override
    public void removeDocumentImage(UUID organisation_id, String url) {
        if ( organisation_id != null && url != null ) {
            throw new InvalidParameterException("todo: implement");
        }
    }

    @Override
    public byte[] getDocumentImage(UUID organisation_id, String url) {
        if ( organisation_id != null && url != null ) {
            throw new InvalidParameterException("todo: implement");
        }
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////

    private String getDocumentPK(Document document) {
        if (document.getOrganisation_id() == null || document.getUrl() == null) {
            throw new InvalidParameterException("invalid Document primary key");
        }
        return getDocumentPK(document.getOrganisation_id(), document.getUrl());
    }

    private String getDocumentPK(UUID org, String url) {
        return org.toString() + ":" + url;
    }

    private Document copyDocument(Document document) throws IOException {
        if (document != null) {
            String json = new ObjectMapper().writeValueAsString(document);
            return new ObjectMapper().readValue(json, Document.class);
        }
        return null;
    }

    public Document createDocument(UUID organisation_id, Document document) throws IOException {
        document.setOrganisation_id(organisation_id);
        documentMap.put(getDocumentPK(document), copyDocument(document));
        return document;
    }

    public Document readDocument(UUID organisation_id, String url) {
        return documentMap.get(getDocumentPK(organisation_id, url));
    }

    public void updateDocument(UUID organisation_id, Document document) throws IOException {
        documentMap.put(getDocumentPK(document), copyDocument(document));
    }

    public void deleteDocument(UUID organisation_id, String url) {
        String pk = getDocumentPK(organisation_id, url);
        documentMap.remove(pk);
        if (documentAuthorMap.containsKey(pk))
            documentAuthorMap.remove(pk);
        deleteTuplesByUrl( organisation_id, url );
    }

    /**
     * select a paginated sub-set of documents
     * @param organisation_id uuid of the organisation
     * @param prevUrl previous (optional) token url
     * @param pageSize the size of the set to return
     * @return a list of documents
     */
    public List<Document> getDocumentList( UUID organisation_id, String prevUrl, int pageSize ) {
        List<Document> documentList = new ArrayList<>();
        String keyPrefix = organisation_id.toString();

        boolean started = (prevUrl == null);
        for ( String key : documentMap.keySet() ) {
            if ( !started ) {
                if ( key.equals( keyPrefix + prevUrl ) ) {
                    started = true;
                }
            }
            if ( key.startsWith(keyPrefix) ) {
                if ( started ) {
                    documentList.add( documentMap.get(key) );
                    pageSize = pageSize - 1;
                }
            }

            // all items fetched?
            if ( pageSize <= 0 ) {
                break;
            }
        }
        return documentList;
    }

    /**
     * get a list of the urls in the document registry - just the urls
     * @param organisation_id the organsiation
     * @param prevUrl pagination, the previous url
     * @param pageSize page size
     * @return list of urls
     */
    public List<String> getDocumentUrlList( UUID organisation_id, String prevUrl, int pageSize ) {
        List<String> documentUrlList = new ArrayList<>();
        String keyPrefix = organisation_id.toString();

        boolean started = (prevUrl == null);
        for ( String key : documentMap.keySet() ) {
            if ( !started ) {
                if ( key.equals( keyPrefix + prevUrl ) ) {
                    started = true;
                }
            }
            if ( key.startsWith(keyPrefix) ) {
                if ( started ) {
                    documentUrlList.add( documentMap.get(key).getUrl() );
                    pageSize = pageSize - 1;
                }
            }

            // all items fetched?
            if ( pageSize <= 0 ) {
                break;
            }
        }
        return documentUrlList;
    }

    public void uploadDocumentBinary(UUID organisation_id, String url, byte[] data) {
        byte[] newData = new byte[data.length];
        System.arraycopy(data, 0, newData, 0, data.length);
        documentBinaryContentMap.put(getDocumentPK(organisation_id, url), newData);
    }

    public byte[] getDocumentBinary(UUID organisation_id, String url) {
        return documentBinaryContentMap.get(getDocumentPK(organisation_id, url));
    }

    public Map<String, byte[]> getDocumentParseTreeMap(UUID organisation_id, String url) {
        return documentParseTreeMap.get(organisation_id.toString() + ":" + url);
    }

    public void saveDocumentParseTreeMap(UUID organisation_id, String url, Map<String, byte[]> map) {
        documentParseTreeMap.put(organisation_id.toString() + ":" + url, map);
        // save document author if they exist
        if (map.containsKey(Document.META_AUTHOR)) {
            byte[] value = map.get(Document.META_AUTHOR);
            documentAuthorMap.put(organisation_id.toString() + ":" + url, value);
        }
    }

    // return a map of url -> author parsed data
    public Map<String, byte[]> getAuthorsForUrlList(UUID organisation_id, List<String> urlList ) {
        Map<String, byte[]> returnData = new HashMap<>();
        if ( urlList != null && organisation_id != null ) {
            for (String url : urlList) {
                byte[] data = documentAuthorMap.get(organisation_id.toString() + ":" + url);
                if ( data != null ) {
                    returnData.put(url, data);
                }
            }
        }
        return returnData;
    }

    public void saveDocumentHistogram(UUID organisation_id, String url, CompressedVector compressedVector) throws IOException {
        String vectorStr = new ObjectMapper().writeValueAsString(compressedVector);
        documentHistogramMap.put(organisation_id.toString() + ":" + url, vectorStr);
    }

    public CompressedVector loadDocumentHistogram(UUID organisation_id, String url) throws IOException {
        String jsonStr = documentHistogramMap.get(organisation_id.toString() + ":" + url);
        if ( jsonStr != null ) {
            return new ObjectMapper().readValue(jsonStr, CompressedVector.class);
        }
        return null;
    }

    public void saveDocumentSummarizationWordSet(UUID organisation_id, String url, SummarisationSet fragmentSet) throws IOException {
        String fragmentStr = new ObjectMapper().writeValueAsString(fragmentSet);
        documentSummarisationMap.put(organisation_id.toString() + ":" + url, fragmentStr);
    }

    public void saveDocumentSummarizationSentenceSet(UUID organisation_id, String url, Sentence sentence ) throws IOException {
        if ( organisation_id != null && url != null && sentence != null ) {
            ObjectMapper mapper = new ObjectMapper();
            String sentenceStr = mapper.writeValueAsString(sentence);
            documentSummarisationSentenceMap.put(organisation_id.toString() + ":" + url, sentenceStr);
        }
    }

    public SummarisationSet loadDocumentSummarizationWordSet(UUID organisation_id, String url) throws IOException {
        if ( url != null && organisation_id != null ) {
            String fragmentStr = documentSummarisationMap.get(organisation_id.toString() + ":" + url);
            if ( fragmentStr != null ) {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(fragmentStr, SummarisationSet.class);
            }
        }
        return null;
    }

    public Sentence loadDocumentSummarizationSentenceSet(UUID organisation_id, String url) throws IOException {
        if ( url != null && organisation_id != null ) {
            String sentenceStr = documentSummarisationSentenceMap.get(organisation_id.toString() + ":" + url);
            if ( sentenceStr != null ) {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(sentenceStr, Sentence.class);
            }
        }
        return null;
    }

    public void saveDocumentSimilarityMap( UUID organisation_id, List<SimilarDocument> similarDocumentList ) {
        if ( organisation_id != null && similarDocumentList != null ) {
            for ( int i = 0; i < similarDocumentList.size(); i++ ) {
                SimilarDocument doc1 = similarDocumentList.get(i);
                saveSimilarity(organisation_id, doc1);
                for ( int j = 0; j < similarDocumentList.size(); j++ ) {
                    if ( i != j ) {
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

    private void saveSimilarity( UUID organisation_id, SimilarDocument document ) {
        String url1 = document.getUrl1();
        String url2 = document.getUrl2();
        double similarity = document.getSimilarity();
        saveSimilarity( organisation_id, url1, url2, similarity );
    }

    private void saveSimilarity( UUID organisation_id, String url1, String url2, double similarity ) {
        documentSimilarityMap.put( organisation_id.toString() + ":" + url1 + ":" + url2, similarity );
        documentSimilarityMap.put( organisation_id.toString() + ":" + url2 + ":" + url1, similarity );
    }

    public List<SimilarDocument> loadSimilarDocuments( UUID organisation_id, String url ) {
        if ( organisation_id != null && url != null ) {
            List<SimilarDocument> similarDocumentList = new ArrayList<>();

            String prefix = organisation_id.toString() + ":" + url;
            documentSimilarityMap.keySet().stream().filter(key -> key.startsWith(prefix)).forEach(key -> {
                String[] items = key.split(":");
                similarDocumentList.add(new SimilarDocument(items[1], items[2], documentSimilarityMap.get(key)));
            });

            if ( similarDocumentList.size() > 0 ) {
                return similarDocumentList;
            }

        } // if valid parameters

        return null;
    }

    public List<SimilarDocumentSet> loadSimilarDocuments(UUID organisation_id ) {
        if ( organisation_id != null ) {

            List<SimilarDocumentSet> similarDocumentList = new ArrayList<>();

            Map<String, SimilarDocumentSet> map = new HashMap<>();

            String prefix = organisation_id.toString() + ":";
            documentSimilarityMap.keySet().stream().filter(key -> key.startsWith(prefix)).forEach(key -> {
                String[] items = key.split(":");
                addSimilar(items[1], items[2], documentSimilarityMap.get(key), map);
            });

            if ( map.size() > 0 ) {
                similarDocumentList.addAll(map.values());
                return similarDocumentList;
            }

        } // if valid parameters

        return null;
    }

    // helper for loadSimilarDocuments() above
    private void addSimilar(String url1, String url2, double value, Map<String, SimilarDocumentSet> map) {
        SimilarDocumentSet similarDocument = map.get(url1);
        if ( similarDocument == null ) {
            similarDocument = new SimilarDocumentSet();
            similarDocument.setUrl(url1);
            map.put( url1, similarDocument );
        }
        similarDocument.getSimilarDocumentList().add( new SimilarDocument(url1, url2, value) );
    }

    ///////////////////////////////////////////////////////////////////////////
    // tuples

    // retrieve a list of tuples by UUID list
    public List<Tuple> readTuples(UUID organisation_id, List<UUID> idArray ) throws IOException {
        if ( organisation_id != null && idArray != null && idArray.size() > 0 ) {
            List<Tuple> tupleList = new ArrayList<>();
            for ( UUID id : idArray ) {
                String pk = organisation_id.toString() + ":" + id.toString();
                String jsonStr = caseTupleStore.get(pk);
                if ( jsonStr != null ) {
                    tupleList.add(new ObjectMapper().readValue(jsonStr, Tuple.class));
                }
            }
            return tupleList;
        }
        return null;
    }

    // write a set of tuples to the database
    public void writeTuple(UUID organisation_id, Tuple tuple) throws IOException {
        if ( organisation_id != null && tuple != null ) {

            String url = tuple.getUrl();
            UUID tupleID = tuple.getId();
            if (tupleID == null) {
                tupleID = UUID.randomUUID();
            }

            String jsonStr = new ObjectMapper().writeValueAsString(tuple);
            String pk = organisation_id.toString() + ":" + tupleID.toString();
            caseTupleStore.put(pk, jsonStr);

            // add them to the url to tuple lookup
            List<UUID> tupleIdList = caseTupleStoreByUrl.get(organisation_id.toString() + ":" + url);
            if (tupleIdList == null) {
                tupleIdList = new ArrayList<>();
                caseTupleStoreByUrl.put(organisation_id.toString() + ":" + url, tupleIdList);
            }
            tupleIdList.add(tupleID);
        }
    }

    // return a set of tuples for a given document
    public List<Tuple> readTuplesForDocument(UUID organisation_id, String url) throws IOException {
        if ( organisation_id != null && url != null ) {
            List<UUID> tupleIdList = caseTupleStoreByUrl.get(organisation_id.toString() + ":" + url);
            if ( tupleIdList != null && tupleIdList.size() > 0 ) {
                List<Tuple> tupleList = readTuples(organisation_id, tupleIdList);
                if ( tupleList.size() > 0 ) {
                    return tupleList;
                }
            }
        }
        return null;
    }

    // remove all tuples for a given document
    public void deleteTuplesByUrl(UUID organisation_id, String url) {
        if ( organisation_id != null && url != null ) {
            List<UUID> tupleIdList = caseTupleStoreByUrl.get(organisation_id.toString() + ":" + url);
            if ( tupleIdList != null ) {
                for ( UUID id : tupleIdList ) {
                    caseTupleStore.remove( organisation_id.toString() + ":" + id.toString() );
                }
            }
            caseTupleStoreByUrl.remove( organisation_id.toString() + ":" + url);
        }
    }

    ///////////////////////////////////////////////////////////////////////////


    private String getOrganisationPK(Organisation organisation) {
        if (organisation.getName() == null) {
            throw new InvalidParameterException("invalid Organisation primary key");
        }
        return getOrganisationPK(organisation.getName());
    }

    private String getOrganisationPK(String name) {
        return name;
    }

    private Organisation copyOrganisation(Organisation organisation) {
        if (organisation != null) {
            try {
                String json = new ObjectMapper().writeValueAsString(organisation);
                return new ObjectMapper().readValue(json, Organisation.class);
            } catch (IOException ex){
                logger.error("copyOrganisation: json", ex);
            }
        }
        return null;
    }

    public Organisation createOrganisation(Organisation organisation) {
        organisation.setId(UUID.randomUUID());
        organisationMap.put(getOrganisationPK(organisation), copyOrganisation(organisation));
        organisationNameMap.put(organisation.getId(), organisation.getName());
        return organisation;
    }

    public Organisation readOrganisation(String name) {
        return organisationMap.get(getOrganisationPK(name));
    }

    public void updateOrganisation(Organisation organisation) {
        organisationMap.put(getOrganisationPK(organisation), copyOrganisation(organisation));
    }

    public String getOrganisationName(UUID id) {
        return organisationNameMap.get(id);
    }

    /**
     * @return list of all organisations
     */
    public List<Organisation> getOrganisationList() {
        return organisationMap.keySet().stream().map(name -> organisationMap.get(name)).collect(Collectors.toList());
    }

    ///////////////////////////////////////////////////////////////////////////

    private Session copySession(Session session) {
        if (session != null) {
            try {
                String json = new ObjectMapper().writeValueAsString(session);
                return new ObjectMapper().readValue(json, Session.class);
            } catch (IOException ex){
                logger.error("copySession: json", ex);
            }
        }
        return null;
    }

    @Override
    public Session createSession(String userEmail, String ipAddress) {
        UUID sessionID = UUID.randomUUID();
        Session session = new Session();
        session.setId(sessionID);
        session.setIp_address(ipAddress);
        session.setEmail(userEmail);
        session.setLast_access(System.currentTimeMillis());
        sessionMap.put(sessionID, session);
        sessionEmailMap.put(userEmail, session);
        return session;
    }

    @Override
    public void clearSession(UUID sessionID) {
        Session session = getSession(sessionID);
        if ( session != null ) {
            sessionMap.remove(sessionID);
            sessionEmailMap.remove(session.getEmail());
        }
    }

    @Override
    public List<Session> getActiveSessions(UUID organisation_id) {
        List<Session> sessionList = new ArrayList<>();
        if ( organisation_id != null ) {
            activeSessionMap.keySet().stream().filter(key -> key.startsWith(organisation_id.toString())).forEach(key -> {
                Session session = activeSessionMap.get(key);
                if (session.getLast_access() < (System.currentTimeMillis() - Session.SESSION_TIMEOUT_IN_MS)) {
                    removeActiveSession(organisation_id, session.getId());
                } else {
                    sessionList.add(session);
                }
            });
        }
        return sessionList;
    }

    /**
     * create an active session by organisation id
     * @param organisation_id the organisation's id
     * @param session the full session to store
     */
    @Override
    public void createActiveSession(UUID organisation_id, Session session) {
        if ( organisation_id != null && session != null && session.getId() != null ) {
            activeSessionMap.put(organisation_id.toString() + ":" + session.getId().toString(), session);
        }
    }

    /**
     * create an active session by organisation id
     * @param organisation_id the organisation's id
     * @param session_id the id of the session to remove
     */
    @Override
    public void removeActiveSession(UUID organisation_id, UUID session_id) {
        if ( organisation_id != null && session_id != null ) {
            activeSessionMap.remove( organisation_id.toString() + ":" + session_id.toString() );
        }
    }

    @Override
    public Session getSession(UUID sessionID) {
        Session session = sessionMap.get(sessionID);
        if (session.getLast_access() < (System.currentTimeMillis() - Session.SESSION_TIMEOUT_IN_MS)) {
            logger.info("session timed out");
            sessionMap.remove(sessionID);
            sessionEmailMap.remove(session.getEmail());
            return null;
        }
        session.setLast_access(System.currentTimeMillis());
        Session sessionCopy = copySession(session);
        sessionMap.put(session.getId(), sessionCopy);
        sessionEmailMap.put( session.getEmail(), sessionCopy);
        return session;
    }

    @Override
    public Session getExistingSessionByEmail(String email) {
        Session session = sessionEmailMap.get(email);
        if ( session != null ) {
            if (session.getLast_access() < (System.currentTimeMillis() - Session.SESSION_TIMEOUT_IN_MS)) {
                logger.info("session timed out");
                sessionMap.remove(session.getId());
                sessionEmailMap.remove(session.getEmail());
                return null;
            }
            session.setLast_access(System.currentTimeMillis());
            sessionMap.put(session.getId(), copySession(session));
        }
        return session;
    }

    @Override
    public UUID createAccountActivation(String email) throws ApplicationException {
        User user = readUser(email);
        if (user == null) {
            throw new ApplicationException("no such user: " + email);
        }
        UUID activationID = UUID.randomUUID();
        accountActivationMap.put(email, activationID);
        return activationID;
    }

    @Override
    public UUID getAccountActivation(String email) {
        return accountActivationMap.get(email);
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

    @Override
    public UUID createPasswordResetRequest(String email) {
        UUID resetID = UUID.randomUUID();
        passwordResetMap.put(email, resetID);
        return resetID;
    }

    @Override
    public UUID getPasswordResetRequest(String email) {
        return passwordResetMap.get(email);
    }

    @Override
    public void resetPassword(String email, String newPassword)
            throws ApplicationException, NoSuchAlgorithmException, UnsupportedEncodingException {
        User user = this.readUser(email);
        if (user != null) {
            this.updateUser(user, newPassword);

            // remove the reset request so it can't be used again
            passwordResetMap.remove(email);

        } else {
            throw new ApplicationException("user does not exist: " + email);
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    private String getIndexPK(UUID organisation_id, Index index) {
        if (organisation_id == null || index.getWord() == null || index.getMeta_data() == null) {
            throw new InvalidParameterException("invalid index primary key");
        }
        return getIndexPK(organisation_id, index.getWord().toLowerCase(), index.getShard(), index.getMeta_data());
    }

    private String getIndexPK(UUID org, String word, int shard, String metadata) {
        return org.toString() + ":" + word.toLowerCase() + ":" + shard + ":" + metadata;
    }

    private String getIndexSecondary(Index index) {
        return index.getUrl() + ":" + index.getWord_origin() + ":" + index.getMeta_data() + ":" + index.getOffset();
    }

    private Index copyIndex(Index index) {
        if (index != null) {
            if (index.getWord_origin() == null) {
                index.setWord_origin("");
            }
            try {
                String json = new ObjectMapper().writeValueAsString(index);
                return new ObjectMapper().readValue(json, Index.class);
            } catch (IOException ex){
                logger.error("copyIndex: json", ex);
            }
        }
        return null;
    }

    @Override
    public void addIndex(UUID organisation_id, Index index) {
        Map<String, Index> secondary = indexMap.get(getIndexPK(organisation_id, index));
        if (secondary == null) {
            secondary = new HashMap<>();
            indexMap.put(getIndexPK(organisation_id, index), secondary);
        }
        secondary.put(getIndexSecondary(index), copyIndex(index));

        // setup unindex
        String key = organisation_id.toString() + ":" + index.getUrl() + ":" + index.getMeta_data();
        List<String> unindexList = unIndexMap.get(key);
        if (unindexList == null) {
            unindexList = new ArrayList<>();
            unIndexMap.put(key, unindexList);
        }
        unindexList.add(index.getWord() + ":" + index.getShard() + ":" + index.getMeta_data());
    }

    @Override
    public void removeIndex(UUID organisation_id, String url, String metadata) {
        if (organisation_id != null && url != null && url.length() > 0) {

            String key = organisation_id.toString() + ":" + url + ":" + metadata;
            List<String> wordList = unIndexMap.get(key);
            if (wordList != null) {
                for (String word : wordList) {
                    String[] parts = word.split(":");
                    Map<String, Index> secondaryMap = indexMap.get(getIndexPK(organisation_id, parts[0], Integer.parseInt(parts[1]), parts[2]));
                    if (secondaryMap != null) {
                        List<String> keysToRemove = secondaryMap.keySet().stream().filter(str -> str.startsWith(url)).collect(Collectors.toList());
                        keysToRemove.forEach(secondaryMap::remove);
                    }
                }
            }
        } // if valid parameters
    }

    @Override
    public List<Index> readIndex(UUID organisation_id, String word, int shard, String metadata) {
        Map<String, Index> secondaryMap = indexMap.get(getIndexPK(organisation_id, word, shard, metadata));
        if ( secondaryMap != null ) {
            List<Index> indexList = secondaryMap.keySet().stream().map(secondaryMap::get).collect(Collectors.toList());
            if (indexList.size() > 0) {
                return indexList;
            }
        }
        return null;
    }

    // no need to flush in the in-memory system
    @Override
    public void flushIndexes() {
    }

    @Override
    public void indexEmotion(UUID organisation_id, String url, int sentence_id, double value, int acl_hash) {
        if ( organisation_id != null && url != null ) {
            String pk = organisation_id.toString() + ":" + url;
            if ( indexEmotionalSet.containsKey(pk) ) {
                indexEmotionalSet.get(pk).add( new EmotionalItem(sentence_id, value, acl_hash) );
            } else {
                List<EmotionalItem> list = new ArrayList<>();
                list.add( new EmotionalItem(sentence_id, value, acl_hash) );
                indexEmotionalSet.put( pk, list );
            }
        }
    }

    public EmotionalSet getEmotionSet(UUID organisation_id, String url) {
        if ( organisation_id != null && url != null ) {
            String pk = organisation_id.toString() + ":" + url;
            EmotionalSet set = new EmotionalSet();
            List<EmotionalItem> list = indexEmotionalSet.get(pk);
            if ( list != null ) {
                set.setEmotional_list(list);
            }
            return set;
        }
        return null;
    }

    /**
     * index a set of times
     * @param organisation_id the organisation
     * @param dateTimeList a complete set of indexes
     */
    @Override
    public void addTimeIndexes(UUID organisation_id, List<TimeIndex> dateTimeList) {

        if ( dateTimeList != null && organisation_id != null ) {

            ObjectMapper mapper = new ObjectMapper();

            // (organisation_id, year, month), day, hour, minute, meta_data, url, offset
            for (TimeIndex timeIndex : dateTimeList) {

                DateTime dateTime = new DateTime(timeIndex.getDate_time());

                String pk = organisation_id + ":" + dateTime.getYear() + ":" + dateTime.getMonthOfYear() +
                        ":" + dateTime.getDayOfMonth() + ":" + dateTime.getHourOfDay();
                try {
                    String jsonStr = mapper.writeValueAsString(timeIndex);
                    TimeIndex timeIndexCopy = mapper.readValue(jsonStr, TimeIndex.class);
                    List<TimeIndex> existingList = timeIndexMap.get(pk);
                    if ( existingList == null ) {
                        existingList = new ArrayList<>();
                        timeIndexMap.put( pk, existingList);
                    }
                    existingList.add( timeIndexCopy );

                } catch ( IOException ex ){
                    logger.error("addTimeIndexes", ex);
                }

            } // for each index

        } // if parameters valid
    }

    @Override
    public List<TimeIndex> readTimeIndexes(UUID organisation_id, int year, int month) {
        String pk = organisation_id + ":" + year + ":" + month;
        return getTimeIndexListForKey(pk);
    }

    @Override
    public List<TimeIndex> readTimeIndexes(UUID organisation_id, int year, int month, int day) {
        String pk = organisation_id + ":" + year + ":" + month + ":" + day;
        return getTimeIndexListForKey(pk);
    }

    @Override
    public List<TimeIndex> readTimeIndexes(UUID organisation_id, int year, int month, int day, int hour) {
        String pk = organisation_id + ":" + year + ":" + month + ":" + day + ":" + hour;
        return getTimeIndexListForKey(pk);
    }

    @Override
    public List<TimeIndex> readTimeIndexes(UUID organisation_id, int year, int month, int day, int hour, int minute) {
        String pk = organisation_id + ":" + year + ":" + month + ":" + day + ":" + hour;
        return getTimeIndexListForKey(pk);
    }

    private List<TimeIndex> getTimeIndexListForKey( String pk ) {
        List<TimeIndex> resultList = new ArrayList<>();
        timeIndexMap.keySet().stream().filter(key -> key.startsWith(pk)).forEach(key -> resultList.addAll(timeIndexMap.get(key)));
        if ( resultList.size() > 0 ) {
            return resultList;
        }
        return null;
    }


    /////////////////////////////////////////////////////////////////////////////////////////
    // hazelcast storage

    // there is no point in implementing an in memory save of an in memory store

    public Long hazelcastWordMapLoad(String key) {
        return null;
    }

    public List<String> hazelcastWordMapLoadAllKeys() {
        return null;
    }

    public Map<String, Long> hazelcastWordMapLoadAll(Collection<String> keys) {
        return null;
    }

    public void hazelcastWordMapStore(String key, Long value) {
    }

    public void hazelcastWordMapStoreAll(Map<String, Long> map) {
    }

    public void hazelcastWordMapDelete(String key) {
    }

    public void hazelcastWordMapDeleteAll(Collection<String> keys) {
    }


    // there is no point in implementing an in memory save of an in memory store

    public List<String> hazelcastAclMapLoad(Integer key) {
        return null;
    }

    public IntList hazelcastAclMapLoadAllKeys() {
        return null;
    }

    public Map<Integer, List<String>> hazelcastAclMapLoadAll(Collection<Integer> keys) {
        return null;
    }

    public void hazelcastAclMapStore(Integer key, List<String> value) {
    }

    public void hazelcastAclMapStoreAll(Map<Integer, List<String>> map) {
    }

    public void hazelcastAclMapDelete(Integer key) {
    }

    public void hazelcastAclMapDeleteAll(Collection<Integer> keys) {
    }

    ///////////////////////////////////////////////////////////////////////////////////////////

    // return a document's index count for maintaining statistics
    public long getDocumentIndexCount(UUID organisation_id, String url) {
        if ( organisation_id != null && url != null ) {
            String key = organisation_id.toString() + ":" + url;
            Long value = documentIndexCount.get(key);
            if ( value != null ) {
                return value;
            }
        }
        return 0;
    }

    // return a document's index count for maintaining statistics
    public void setDocumentIndexCount(UUID organisation_id, String url, long index_count) {
        if ( organisation_id != null && url != null ) {
            String key = organisation_id.toString() + ":" + url;
            documentIndexCount.put(key, index_count);
        }
    }

    // return a document's index count for maintaining statistics
    public void deleteDocumentIndexCount(UUID organisation_id, String url) {
        if ( organisation_id != null && url != null ) {
            String key = organisation_id.toString() + ":" + url;
            documentIndexCount.remove(key);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // cluster

    public void saveCluster( UUID organisation_id, int id, kMeansCluster clusterData ) throws IOException {
        if ( organisation_id != null && id >= 0 ) {
            String clusterStr = new ObjectMapper().writeValueAsString(clusterData);
            clusterDataMap.put( organisation_id.toString() + ":" + id, clusterStr);
        }
    }

    public kMeansCluster loadFullClusterItem(UUID organisation_id, int id) throws IOException {
        if ( organisation_id != null && id >= 0 ) {
            String json = clusterDataMap.get(organisation_id.toString() + ":" + id);
            if ( json != null ) {
                return new ObjectMapper().readValue(json, kMeansCluster.class);
            }
        }
        return null;
    }

    public kMeansCluster loadSummaryClusterItem(UUID organisation_id, int id) throws IOException {
        if ( organisation_id != null && id >= 0 ) {
            String json = clusterDataMap.get(organisation_id.toString() + ":" + id);
            if ( json != null ) {
                return new ObjectMapper().readValue(json, kMeansCluster.class);
            }
        }
        return null;
    }

    // when was the system last taken through a full cluster cycle?
    public long getClusterLastClustered( UUID organisation_id ) {
        if ( organisation_id != null ) {
            Long value = lastClustered.get(organisation_id);
            if ( value != null ) {
                return value;
            }
        }
        return 0L;
    }

    public void setClusterLastClustered( UUID organisation_id, long dateTime ) {
        if ( organisation_id != null ) {
            lastClustered.put( organisation_id, dateTime );
        }
    }

    // when did the system last have a change that required re-clustering of the data?
    public long getClusterLastChange( UUID organisation_id ) {
        if ( organisation_id != null ) {
            Long value = lastChanged.get(organisation_id);
            if ( value != null ) {
                return value;
            }
        }
        return 0L;
    }

    public void setClusterLastChange( UUID organisation_id, long dateTime ) {
        if ( organisation_id != null ) {
            lastChanged.put( organisation_id, dateTime );
        }
    }

    public long getCosineLastChange( UUID organisation_id ) {
        if ( organisation_id != null ) {
            Long value = lastCosined.get(organisation_id);
            if ( value != null ) {
                return value;
            }
        }
        return 0L;
    }

    public void setCosineLastChange( UUID organisation_id, long dateTime ) {
        if ( organisation_id != null ) {
            lastCosined.put( organisation_id, dateTime );
        }
    }

    // save a document's emotion status if it has exceeded a certain threshold
    public void setDocumentEmotion( UUID organisation_id, String url, int positive_sentence_id, double positive, int negative_sentence_id, double negative ) {
        if ( organisation_id != null && url != null ) {
            List<UrlValue> list = emotionSet.get(organisation_id);
            if ( list == null ) {
                list = new ArrayList<>();
                emotionSet.put(organisation_id, list);
            }
            if ( positive != 0.0 ) {
                list.add(new UrlValue(url, positive, positive_sentence_id));
            }
            if ( negative != 0.0 ) {
                list.add(new UrlValue(url, negative, negative_sentence_id));
            }
        }
    }

    public synchronized List<UrlValue> getDocumentEmotion( UUID organisation_id, boolean positive, int offset, int pageSize ) {
        List<UrlValue> resultList = new ArrayList<>();
        if ( organisation_id != null ) {
            List<UrlValue> urlValueList = emotionSet.get(organisation_id);

            Collections.sort( urlValueList , (s1, s2) -> {
                if ( positive ) {
                    if ( s1.getValue() < s2.getValue() ) return 1;
                    if ( s1.getValue() > s2.getValue() ) return -1;
                    return 0;
                } else {
                    if ( s1.getValue() < s2.getValue() ) return -1;
                    if ( s1.getValue() > s2.getValue() ) return 1;
                    return 0;
                }
            });

            int index = 0;
            for (Iterator<UrlValue> iterator = urlValueList.iterator(); iterator.hasNext() && resultList.size() < pageSize; ) {
                UrlValue item = iterator.next();
                if (offset <= index && index < (offset + pageSize)) {
                    resultList.add(item);
                }
                index++;
            }
        }
        return resultList;
    }

    // anomalies
    public List<String> getDocumentAnomaliesPaginated( UUID organisation_id, String prevUrl, int pageSize ) {
        if ( organisation_id != null && anomalySet.containsKey(organisation_id) ) {
            List<String> urlList = new ArrayList<>();
            boolean started = (prevUrl == null);
            List<String> list = anomalySet.get(organisation_id);
            for ( String value : list ) {
                if (!started) {
                    if ( value.equals(prevUrl) ) {
                        started = true;
                    }
                }
                if (started) {
                    urlList.add(value);
                    pageSize = pageSize - 1;
                }

                // all items fetched?
                if (pageSize <= 0) {
                    break;
                }
            }
            return urlList;
        }
        return null;
    }

    public void saveDocumentAnomalies( UUID organisation_id, List<String> urlList ) {
        if ( organisation_id != null ) {
            List<String> list = anomalySet.get(organisation_id);
            if (list == null) {
                list = new ArrayList<>();
                anomalySet.put(organisation_id, list);
                list.addAll( urlList );
            } else {
                for ( String url : urlList ) {
                    if ( !list.contains(url) ) {
                        list.add(url);
                    }
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // queue persistence

    // there is no point in implementing an in memory save of an in memory store

    @Override
    public DocumentAction hazelcastDocumentActionLoad(Long key) {
        return null;
    }

    @Override
    public Set<Long> hazelcastDocumentActionLoadAllKeys() {
        return null;
    }

    @Override
    public Map<Long, DocumentAction> hazelcastDocumentActionLoadAll(Collection<Long> keys) {
        return null;
    }

    @Override
    public void hazelcastDocumentActionStore(Long key, DocumentAction value) {
    }

    @Override
    public void hazelcastDocumentActionStoreAll(Map<Long, DocumentAction> map) {
    }

    @Override
    public void hazelcastDocumentActionDelete(Long key) {
    }

    @Override
    public void hazelcastDocumentActionDeleteAll(Collection<Long> keys) {
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // rules

    public void saveRule( UUID organisation_id, String rule_name, RuleItem item ) throws IOException {
        if ( organisation_id != null && rule_name != null && item != null ) {
            String ruleJson = new ObjectMapper().writeValueAsString(item);
            ruleMap.put( organisation_id.toString() + ":" + rule_name, ruleJson );
        }
    }

    public RuleItem loadRuleByName( UUID organisation_id, String rule_name ) throws IOException {
        if ( organisation_id != null && rule_name != null ) {
            String json = ruleMap.get( organisation_id.toString() + ":" + rule_name );
            if ( json != null ) {
                return new ObjectMapper().readValue( json, RuleItem.class );
            }
        }
        return null;
    }

    public List<RuleItem> getRuleList(UUID organisation_id, String prevRule, int pageSize) throws IOException {
        List<RuleItem> ruleList = new ArrayList<>();
        if ( organisation_id != null && pageSize > 0 ) {
            String prefix = organisation_id.toString() + ":";
            boolean started = false;
            for (String key : ruleMap.keySet()) {
                if ( key.startsWith(prefix) ) {
                    String json = ruleMap.get( key );
                    if ( prevRule == null || started ) {
                        ruleList.add( new ObjectMapper().readValue( json, RuleItem.class ) );
                    } else {
                        if ( key.equals(prefix + prevRule) ) {
                            started = true;
                        }
                    }
                }
                if ( ruleList.size() >= pageSize ) {
                    break;
                }
            } // for each key
        }
        return ruleList;
    }

    public void deleteRule( UUID organisation_id, String rule_name ) {
        if ( organisation_id != null && rule_name != null ) {
            String key = organisation_id.toString() + ":" + rule_name;
            ruleMap.remove(key);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // reports

    public Report createReport(UUID organisation_id, Report report) {
        if ( organisation_id != null && report != null && report.getReport_name() != null ) {
            report.setOrganisation_id(organisation_id);
            try {
                String reportJson = new ObjectMapper().writeValueAsString(report);
                reportMap.put(organisation_id.toString() + ":" + report.getReport_name(), reportJson);
            } catch (IOException ex) {
                logger.error("createReport", ex);
            }
        }
        return report;
    }

    public Report readReport(UUID organisation_id, String report_name) {
        if ( organisation_id != null && report_name != null ) {
            String key = organisation_id.toString() + ":" + report_name;
            String json = reportMap.get(key);
            if ( json != null ) {
                try {
                    return new ObjectMapper().readValue(json, Report.class);
                } catch (IOException ex) {
                    logger.error("readReport", ex);
                }
            }
        }
        return null;
    }

    public List<Report> readReportList(UUID organisation_id) {
        if ( organisation_id != null ) {
            List<Report> reportList = new ArrayList<>();
            String prefix = organisation_id.toString() + ":";
            reportMap.keySet().stream().filter(key -> key.startsWith(prefix)).forEach(key -> {
                String json = reportMap.get(key);
                if (json != null) {
                    try {
                        reportList.add(new ObjectMapper().readValue(json, Report.class));
                    } catch (IOException ex) {
                        logger.error("readReportList", ex);
                    }
                }
            });
            return reportList;
        }
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // neural networks

    public void addNNetTrainingSample( UUID organisation_id, String word, int synset_id,
                                       NNetTrainingSample training_sample ) throws IOException {

        if ( organisation_id != null && word != null && synset_id >= 0 && training_sample != null &&
                training_sample.getId() != null && training_sample.getTraining_data() != null ) {

            String jsonStr = new ObjectMapper().writeValueAsString(training_sample);
            String key = organisation_id.toString() + ":" + word + ":" + synset_id + ":" +training_sample.getId().toString();
            nnetTrainingSet.put( key, jsonStr );
        }
    }


    public List<NNetTrainingSample> getNNetTrainingSamples( UUID organisation_id, String word, int synset_id,
                                                            UUID prev_id, int pageSize ) throws IOException {

        if ( organisation_id != null && word != null && synset_id >= 0 && pageSize > 0 ) {

            List<NNetTrainingSample> trainingSetList = new ArrayList<>();
            String prefix = organisation_id.toString() + ":" + word + ":" + synset_id + ":";
            boolean started = false;
            for (String key : nnetTrainingSet.keySet()) {
                if ( key.startsWith(prefix) ) {
                    String json = nnetTrainingSet.get( key );
                    if ( prev_id == null || started ) {
                        trainingSetList.add( new ObjectMapper().readValue( json, NNetTrainingSample.class ) );
                    } else {
                        if ( key.equals(prefix + prev_id.toString()) ) {
                            started = true;
                        }
                    }
                }
                if ( trainingSetList.size() >= pageSize ) {
                    break;
                }
            } // for each key
            return trainingSetList;
        }
        return null;
    }

    public void saveModel( UUID organisation_id, String word, NNetModelData model ) throws IOException {
        if ( organisation_id != null && word != null && model != null ) {
            String jsonStr = new ObjectMapper().writeValueAsString(model);
            nnetStore.put( organisation_id.toString() + ":" + word, jsonStr);
        }
    }

    public long getModelLastUpdated( UUID organisation_id, String word ) throws IOException {
        if ( organisation_id != null && word != null ) {
            NNetModelData model = loadModel(organisation_id, word);
            if ( model != null ) {
                return model.getLast_updated();
            }
        }
        return 0;
    }

    public NNetModelData loadModel( UUID organisation_id, String word ) throws IOException {
        if ( organisation_id != null && word != null ) {
            String jsonStr = nnetStore.get( organisation_id.toString() + ":" + word);
            if (jsonStr != null) {
                return new ObjectMapper().readValue( jsonStr, NNetModelData.class );
            }
        }
        return null;
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    // address book database

    // insert or update an entry
    public void saveKBEntry(KBEntry kbEntry) {
        if ( kbEntry != null && kbEntry.getType() != null && kbEntry.getOrganisation_id() != null ) {
            if (kbEntry.getId() == null) {
                kbEntry.setId(UUID.randomUUID());
            }
            String key = kbEntry.getOrganisation_id().toString() + ":" + kbEntry.getType() + ":" + kbEntry.getId().toString();
            kbSet.put(key, copyKBEntry(kbEntry));
        }
    }

    private KBEntry copyKBEntry(KBEntry kbEntry) {
        if (kbEntry != null) {
            try {
                String json = new ObjectMapper().writeValueAsString(kbEntry);
                return new ObjectMapper().readValue(json, KBEntry.class);
            } catch (IOException ex){
                logger.error("copyKBEntry: json", ex);
            }
        }
        return null;
    }

    // get a list of entries matching name
    public KBEntry getKBEntry(UUID organisation_id, String type, UUID id) {
        List<KBEntry> list = new ArrayList<>();
        if ( id != null && organisation_id != null && type != null ) {
            String key = organisation_id.toString() + ":" + type + ":" + id.toString();
            return kbSet.get(key);
        }
        return null;
    }

    // remove an entry using its keys
    public void deleteKBEntry(UUID organisation_id, String type, UUID id) {
        if ( id != null && organisation_id != null && type != null ) {
            String key = organisation_id.toString() + ":" + type + ":" + id.toString();
            kbSet.remove(key);
        }
    }

    /**
     * paginated access to an entity
     *
     * @param organisation_id the organisation of the entity
     * @param type the type of the object to fetch
     * @param prevEntity    the pagination indicator (or null)
     * @param numEntitiesToRead   size of the page
     * @return a list of entity items
     */
    public List<KBEntry> getEntityList(UUID organisation_id, String type, UUID prevEntity, int numEntitiesToRead) {

        List<KBEntry> allEntities = new ArrayList<>();
        allEntities.addAll( kbSet.values() );

        int readTime = -1;
        List<KBEntry> resultList = new ArrayList<>();
        for ( KBEntry entity : allEntities ) {
            if ( prevEntity == null && numEntitiesToRead > 0 ) {
                resultList.add( copyKBEntry(entity) );
                numEntitiesToRead = numEntitiesToRead - 1;
            } else if ( prevEntity != null && prevEntity.equals(entity.getId()) ) {
                if ( readTime == -1 ) {
                    readTime = -2;
                } else if ( readTime == -2 ) {
                    readTime = 0;
                    resultList.add( copyKBEntry(entity) );
                    numEntitiesToRead = numEntitiesToRead - 1;
                } else if ( numEntitiesToRead > 0 ) {
                    readTime = numEntitiesToRead - 1;
                    resultList.add( copyKBEntry(entity) );
                }
            }
        }
        return resultList;
    }


}

