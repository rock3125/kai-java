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

package industries.vocht.viki.services;

import industries.vocht.viki.ApplicationException;
import industries.vocht.viki.IDao;
import industries.vocht.viki.IHazelcast;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.document.DocumentList;
import industries.vocht.viki.hazelcast_messages.HMsgSecurityUpdate;
import industries.vocht.viki.hazelcast_messages.IHazelcastMessageProcessor;
import industries.vocht.viki.model.*;
import industries.vocht.viki.model.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by peter on 4/03/16.
 *
 */
@Component
public class DocumentService {

    private final static Logger logger = LoggerFactory.getLogger(DocumentService.class);
    private final static String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @Autowired
    private IDao dao; // dao access

    @Autowired
    private IHazelcast hazelcast;

    @Autowired
    private IHazelcastMessageProcessor messageProcessor;

    private DateFormat dateFormat;

    public DocumentService() {
        this.dateFormat = new SimpleDateFormat(DATE_FORMAT);
    }

    /**
     * get a document record by url
     * @param sessionID the security context session
     * @param url the url of the document to get
     * @param ipAddress the security ip context
     * @return the document
     * @throws ApplicationException
     */
    public Document getDocument(UUID sessionID, String url, String ipAddress) throws ApplicationException {
        if ( sessionID == null || ipAddress == null || url == null ) {
            throw new ApplicationException("getDocument: invalid parameter");
        }
        User sessionUser = dao.getUserDao().getUserForSession(sessionID, ipAddress);
        if ( sessionUser == null ) {
            throw new ApplicationException("getDocument: invalid session");
        }
        try {
            return dao.getDocumentDao().read(sessionUser.getOrganisation_id(), url);
        } catch (IOException ex) {
            throw new ApplicationException(ex);
        }
    }

    /**
     * remove a document by url
     * @param sessionID the security context session
     * @param url the url of the document to get
     * @param ipAddress the security ip context
     * @throws ApplicationException
     */
    public void deleteDocument(UUID sessionID, String url, String ipAddress ) throws ApplicationException {
        if ( sessionID == null || ipAddress == null || url == null ) {
            throw new ApplicationException("deleteDocument: invalid parameter");
        }
        User sessionUser = dao.getUserDao().getUserForSession(sessionID, ipAddress);
        if ( sessionUser == null ) {
            throw new ApplicationException("deleteDocument: invalid session");
        }
        // make sure the document exists
        Document existingDocument;
        try {
            existingDocument = dao.getDocumentDao().read(sessionUser.getOrganisation_id(), url);
        } catch (IOException ex) {
            throw new ApplicationException(ex);
        }
        if ( existingDocument == null ) {
            throw new ApplicationException("deleteDocument: a document with that url does not exist");
        }

        dao.getDocumentDao().delete(sessionUser.getOrganisation_id(), url);
    }

    /**
     * save a document (create new or update existing)
     * @param sessionID the security context session
     * @param document the document to create (its information)
     * @param ipAddress the security ip context
     * @return the updated document structure
     * @throws ApplicationException
     */
    public Document saveDocument(UUID sessionID, Document document, String ipAddress ) throws ApplicationException {
        if ( sessionID == null || ipAddress == null || document == null || document.getUrl() == null ||
                document.getAcl_set() == null || document.getAcl_set().size() == 0 ) {
            throw new ApplicationException("saveDocument: invalid parameter");
        }

        User sessionUser = dao.getUserDao().getUserForSession(sessionID, ipAddress);
        if ( sessionUser == null ) {
            throw new ApplicationException("createDocument: invalid session");
        }
        // make sure the document doesn't exist
        try {
            Document existingDocument = dao.getDocumentDao().read(sessionUser.getOrganisation_id(), document.getUrl());
            // update global hash system for security system
            updateAclList(sessionUser.getOrganisation_id(), document);
            if (existingDocument != null) {
                updateDocument(existingDocument, document);
                return document;
            } else {
                return createDocument(sessionUser.getOrganisation_id(), document);
            }
        } catch (IOException ex) {
            throw new ApplicationException(ex);
        }
    }

    /**
     * store the document's acl hash and set this hash value into the document for saving
     * @param organisation_id the organisation in question
     * @param document the document to be stored / hashed for security
     */
    private void updateAclList( UUID organisation_id, Document document ) throws IOException {
        if ( organisation_id != null && document != null && document.getAcl_set() != null ) {
            // update ACLs?
            List<String> aclList = document.getAcl_set().stream().map(Acl::toString).collect(Collectors.toList());
            Collections.sort(aclList);
            String aclStr = "";
            for (String acl : aclList) {
                aclStr = aclStr + acl + ",";
            }
            int hashValue = aclStr.hashCode();

            // update the acl maps?
            if ( !hazelcast.getHashAclMap(organisation_id).containsKey(hashValue) ) {
                hazelcast.getHashAclMap(organisation_id).put( hashValue, aclList );
                // send update message to all nodes that the security model has been updated
                messageProcessor.publish( new HMsgSecurityUpdate(organisation_id) );
            }
            document.setAclHash(hashValue);
        }
    }

    /**
     * create a new document
     * @param organisation_id the organisation
     * @param document the document to create (its information)
     * @return the updated document structure
     * @throws ApplicationException
     */
    private Document createDocument(UUID organisation_id, Document document) throws ApplicationException {
        // add extra meta-data
        document.getName_value_set().put(Document.META_URL, document.getUrl());
        document.getName_value_set().put(Document.META_ACLS, document.aclsToPrettyString());
        document.getName_value_set().put(Document.META_ORIGIN, document.getOrigin());
        document.getName_value_set().put(Document.META_UPLOAD_DATE_TIME, dateFormat.format(new Date(System.currentTimeMillis())));

        try {
            return dao.getDocumentDao().create(organisation_id, document);
        } catch (IOException ex) {
            throw new ApplicationException(ex);
        }
    }

    /**
     * update an existing document
     * @param existingDocument the existing document to update
     * @param document the document to create (its information)
     * @throws ApplicationException
     */
    private void updateDocument(Document existingDocument, Document document) throws ApplicationException {
        // make sure the document doesn't exist
        document.mergeMetadata(existingDocument);
        document.setAcl_set(existingDocument.getAcl_set());
        try {
            dao.getDocumentDao().update(existingDocument.getOrganisation_id(), document);
        } catch (IOException ex) {
            throw new ApplicationException(ex);
        }
    }

    /**
     * upload / create the binary for a document
     * @param sessionID the security context session
     * @param url the url of the document
     * @param data the binary content of the document
     * @param ipAddress the security ip context
     * @throws ApplicationException
     */
    public void uploadDocument(UUID sessionID, String url, byte[] data, String ipAddress ) throws ApplicationException {
        if ( sessionID == null || ipAddress == null || url == null || data == null || data.length == 0 ) {
            throw new ApplicationException("uploadDocument: invalid parameter");
        }
        User sessionUser = dao.getUserDao().getUserForSession(sessionID, ipAddress);
        if ( sessionUser == null ) {
            throw new ApplicationException("uploadDocument: invalid session");
        }
        // make sure the document exists
        Document existingDocument;
        try {
            existingDocument = dao.getDocumentDao().read(sessionUser.getOrganisation_id(), url);
        } catch (IOException ex) {
            throw new ApplicationException(ex);
        }
        if ( existingDocument == null ) {
            throw new ApplicationException("uploadDocument: a document with that url does not exist");
        }
        dao.getDocumentDao().updateDocumentBinary(sessionUser.getOrganisation_id(), url, data);
        // update the last uploaded flag
        existingDocument.setDate_time_uploaded(System.currentTimeMillis());
        try {
            dao.getDocumentDao().update(sessionUser.getOrganisation_id(), existingDocument);
        } catch (IOException ex) {
            throw new ApplicationException(ex);
        }
    }

    /**
     * paginate a set of documents for this user
     * @param sessionID the security context session
     * @param prevUrl the url of the previous page (or null) for pagination
     * @param numItemsPerPage the number of items to return at any one time
     * @param ipAddress the security ip context
     * @return a document list set
     * @throws ApplicationException
     */
    public DocumentList getPaginatedDocumentList(UUID sessionID, String prevUrl, int numItemsPerPage,
                                                 String ipAddress) throws ApplicationException {
        if ( sessionID == null || ipAddress == null ) {
            throw new ApplicationException("getPaginatedDocumentList: invalid parameter");
        }
        User sessionUser = dao.getUserDao().getUserForSession(sessionID, ipAddress);
        if ( sessionUser == null ) {
            throw new ApplicationException("getPaginatedDocumentList: invalid session");
        }
        try {
            List<Document> documentList = dao.getDocumentDao().getDocumentList(sessionUser.getOrganisation_id(), prevUrl, numItemsPerPage);
            DocumentList list = new DocumentList();
            list.setDocument_list(documentList);
            list.setOrganisation_id(sessionUser.getOrganisation_id());
            list.setPrevUrl(prevUrl);
            list.setItems_per_page(numItemsPerPage);
            list.setTotal_document_count(0);
            return list;
        } catch (IOException ex) {
            throw new ApplicationException(ex);
        }
    }

    /**
     * convert a set of urls to a document list
     * @param list a list of urls
     * @param page page offset into the list
     * @param numItemsPerPage num of items per page
     * @return a list of documents (or empty object)
     */
    public DocumentList getPaginatedDocumentList( UUID organisation_id, List<String> list, int page, int numItemsPerPage ) throws ApplicationException {
        if ( list != null && list.size() > 0 ) {

            DocumentList documentList = new DocumentList();
            documentList.setItems_per_page(numItemsPerPage);
            documentList.setTotal_document_count(list.size());

            int offset = page * numItemsPerPage;
            int endOffset = offset + numItemsPerPage;
            for (int i = offset; i < endOffset; i++) {
                if (i < list.size()) {
                    try {
                        Document document = dao.getDocumentDao().read(organisation_id, list.get(i));
                        if (document != null) {
                            documentList.getDocument_list().add(document);
                        }
                    } catch (IOException ex) {
                        throw new ApplicationException(ex);
                    }
                }
            }
            return documentList;
        } else {
            return new DocumentList();
        }
    }

    /**
     * access a previously stored document binary
     * @param sessionID the security context session
     * @param url the url of the document
     * @param ipAddress the security ip context
     * @return the data of the document or null if dne
     * @throws ApplicationException
     */
    public byte[] getDocumentBinary(UUID sessionID, String url, String ipAddress) throws ApplicationException {
        if ( sessionID == null || ipAddress == null || url == null ) {
            throw new ApplicationException("getDocumentBinary: invalid parameter");
        }
        User sessionUser = dao.getUserDao().getUserForSession(sessionID, ipAddress);
        if ( sessionUser == null ) {
            throw new ApplicationException("getDocumentBinary: invalid session");
        }
        // make sure the document exists
        Document existingDocument;
        try {
            existingDocument = dao.getDocumentDao().read(sessionUser.getOrganisation_id(), url);
        } catch (IOException ex) {
            throw new ApplicationException(ex);
        }
        if ( existingDocument == null ) {
            throw new ApplicationException("getDocumentBinary: a document with that url does not exist");
        }
        return dao.getDocumentDao().getDocumentBinary(sessionUser.getOrganisation_id(), url);
    }

}

