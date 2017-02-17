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

import industries.vocht.viki.*;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.document.DocumentList;
import industries.vocht.viki.document_orchestrator.DocumentOrchestrator;
import industries.vocht.viki.hazelcast.DocumentAction;
import industries.vocht.viki.hazelcast.Hazelcast;
import industries.vocht.viki.indexer.IndexerDocumentFind;
import industries.vocht.viki.jersey.HtmlWrapper;
import industries.vocht.viki.jersey.JsonMessage;

import industries.vocht.viki.model.*;
import industries.vocht.viki.model.user.User;
import industries.vocht.viki.utility.SentenceFromBinary;
import io.swagger.annotations.Api;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by peter on 17/10/15.
 *
 * service for managing documents
 *
 */
@Component
@Path("/viki/document")
@Api(tags = "/viki/document")
public class DocumentServiceLayer {

    private final Logger logger = LoggerFactory.getLogger(DocumentServiceLayer.class);

    // message to the user if this service layer isn't active
    private static final String sl_inactive_message = "document service layer not active on this node";

    @Value("${sl.document.activate:true}")
    private boolean slDocumentActive;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private IDao dao;

    @Autowired
    private UserService userService;

    @Autowired
    private IndexerDocumentFind documentIndexer; // ui

    @Autowired
    private DocumentOrchestrator documentOrchestrator;

    /**
     * location for uploading files to - spring injected
     */
    @Value("${document.upload.directory:/tmp}")
    private String serverUploadLocation;

    // image not found resource
    @Value("${doc.not.found.image.resource:/data/wordcloud_backgrounds/kai_640_480_not_found.png}")
    private String imageNotFoundResourcePath;

    /**
     * max size of a file allowed in MB (or infinite if <= 0) - spring injected
     */
    @Value("${document.max.upload.size.in.mb:100}")
    private int maxUploadSizeInMB;

    public DocumentServiceLayer() {
    }

    /**
     * view a document that belongs to this seeker by title as a binary
     * return the byte[] that is the document's original content
     *
     * @param request  the context of the request for ip purposes
     * @param url the url of the document to be viewed
     * @return a byte[] stream of the object
     */
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("document/{sessionID}/{url}")
    public Response getBinary(@Context HttpServletRequest request,
                        @PathParam("sessionID") String sessionID,
                        @PathParam("url") String url) {
        if ( !slDocumentActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            // check session
            User user = userService.getUser(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("document/get-binary invalid session (" + sessionID + ")");
                return Response.status(500).entity("invalid session").build();
            } else {
                byte[] data = documentService.getDocumentBinary(UUID.fromString(sessionID), url, request.getRemoteAddr());
                logger.debug("document/get-binary " + url);
                return Response.status(200).entity(data).build();
            }
        } catch (Exception ex) {
            logger.error("document/get-binary", ex);
            return Response.status(500).entity(ex.getMessage()).build();
        }
    }


    /**
     * return an image for the given document
     * @param request the http request
     * @param sessionID the user's session id
     * @param url the url of the document
     * @return null, or a valid PNG image for this document
     */
    @GET
    @Produces("image/png")
    @Path("document/image/{sessionID}/{url}")
    public Response documentImage(@Context HttpServletRequest request,
                                  @PathParam("sessionID") String sessionID,
                                  @PathParam("url") String url) {
        if ( !slDocumentActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            User user = userService.getUser(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("document/image/ invalid session (" + sessionID + ")");
                return Response.status(500).entity("invalid session").build();
            } else {
                logger.debug("document/image " + url);
                byte[] data = dao.getDocumentDao().getDocumentImage(user.getOrganisation_id(), url);
                if ( data != null && data.length > 100 ) {
                    return Response.status(200).entity(data).build();
                } else {
                    data = loadBinaryFromResource(imageNotFoundResourcePath);
                    return Response.status(200).entity(data).build();
                }
            }
        } catch (Exception ex) {
            logger.error("document/image", ex);
            return Response.status(500).entity(new HtmlWrapper().wrapH1(ex.getMessage())).build();
        }
    }


    /**
     * get all meta-data fields from a document in our system as text in a map
     * does not return the original text of the document itself
     *
     * @param request  the context of the request for ip purposes
     * @param sessionID the user's session
     * @param url the file name of the document whose meta-data is to be viewed
     * @return a MetadataTextSet object with name / value pairs if they were found
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("metadata/{sessionID}/{url}")
    public Response metadata(@Context HttpServletRequest request,
                           @PathParam("sessionID") String sessionID,
                           @PathParam("url") String url) {
        if ( !slDocumentActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            // check session
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("document/metadata @GET invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else {
                logger.debug("document/metadata @GET (" + url + ")");
                Map<String, byte[]> documentMap = dao.getDocumentDao().getDocumentParseTreeMap(user.getOrganisation_id(), url);
                logger.info("text " + url);
                if (documentMap == null) {
                    return Response.status(404).entity(new JsonMessage("meta-data map for " + url + " not found, perhaps not yet processed.  please try again later.")).build();
                } else {
                    // turn the map into a set of name / values
                    SentenceFromBinary sentenceFromBinary = new SentenceFromBinary();
                    MetadataTextSet set = new MetadataTextSet();
                    for ( String key : documentMap.keySet() ) {
                        // skip main document content key
                        if ( key.equals(Document.META_BODY ) ) {
                            continue;
                        }

                        // convert data back to text
                        byte[] data = documentMap.get(key);
                        if ( data != null && data.length > 4 ) {
                            List<Sentence> sentenceList = sentenceFromBinary.convert(data);
                            if ( sentenceList != null && sentenceList.size() > 0 ) {
                                StringBuilder sb = new StringBuilder();
                                for ( Sentence sentence : sentenceList ) {
                                    sb.append(sentence.toString()).append("  ");
                                }
                                set.addValue( Document.prettyName(key), sb.toString() );
                            }
                        }

                    } // for each item in the meta-data map

                    return Response.status(200).entity(set).build();
                }
            }
        } catch (Exception ex) {
            logger.error("document/metadata @GET", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }

    /**
     * view a document sentence
     *
     * @param request  the context of the request for ip purposes
     * @param url the url of the document to be viewed
     * @param sentenceIndex the index of the sentence to return
     * @return a string that is the sentence
     */
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("sentence/{sessionID}/{url}/{sentenceIndex}")
    public Response getSentence(@Context HttpServletRequest request,
                                @PathParam("sessionID") String sessionID,
                                @PathParam("url") String url,
                                @PathParam("sentenceIndex") int sentenceIndex ) {
        if ( !slDocumentActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            // check session
            User user = userService.getUser(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("document/sentence invalid session (" + sessionID + ")");
                return Response.status(500).entity("invalid session").build();
            } else {
                logger.debug("document/sentence " + sentenceIndex + " @ " + url);
                SentenceFromBinary converter = new SentenceFromBinary();
                Map<String, byte[]> documentMap = dao.getDocumentDao().getDocumentParseTreeMap(user.getOrganisation_id(), url);
                List<Sentence> sentenceList = converter.convert(documentMap.get(Document.META_BODY));
                if ( sentenceList != null && sentenceIndex >= 0 && sentenceIndex < sentenceList.size() ) {
                    Sentence sentence = sentenceList.get(sentenceIndex);
                    return Response.status(200).entity(sentence.toString()).build();
                } else {
                    return Response.status(404).entity("document/sentence not found").build();
                }
            }
        } catch (Exception ex) {
            logger.error("document/sentence", ex);
            return Response.status(500).entity(ex.getMessage()).build();
        }
    }

    /**
     * create the initial document before an upload
     * @param request the http request
     * @param sessionID the session's id
     * @param origin the document's origin
     * @param url the url of the document
     * @param document the document meta-data and ACLs
     * @return 200 on success with a message
     */
    @POST
    @Path("metadata/{sessionID}/{origin}/{url}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response metadata(@Context HttpServletRequest request,
                             @PathParam("sessionID") String sessionID,
                             @PathParam("origin") String origin,
                             @PathParam("url") String url,
                             Document document) {

        if ( !slDocumentActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        if ( sessionID == null || document == null ) {
            return Response.status(500).entity(new JsonMessage("invalid parameters")).build();
        }
        if ( document.getAcl_set() == null || document.getAcl_set().size() == 0 ) {
            return Response.status(500).entity(new JsonMessage("invalid parameters, document must have acls")).build();
        }
        if ( document.getProcessingPipeline() == 0L ) {
            return Response.status(500).entity(new JsonMessage("invalid parameters, document pipeline flag not set")).build();
        }
        try {
            // check session
            User user = userService.getUser(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("document/metadata invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else {
                try {
                    document.setUrl(url);
                    document.setOrigin(origin);
                    document.getName_value_set().put( Document.META_ORIGIN, origin );
                    document.setOrganisation_id(user.getOrganisation_id());
                    documentService.saveDocument( UUID.fromString(sessionID), document, request.getRemoteAddr() );

                    // index a document for the ui
                    documentIndexer.index(user.getOrganisation_id(), document);

                    // all is ok - return
                    logger.debug("document/metadata complete " + sessionID + ", " + url);
                    return Response.status(200).entity(new JsonMessage("metadata successfully uploaded",null)).build();

                } catch (ApplicationException ex) {
                    return Response.status(500).entity(new JsonMessage("IO Exception:" + ex.getMessage())).build();
                }
            }
        } catch (Exception ex) {
            logger.error("document/metadata", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }

    /**
     * upload a document to the system for a given owner.  The owner can be an email (user) or a guid (job id)
     * to test:
     * curl -v -X POST --form file=@dol.png --form payload=junk https://localhost:8443/1.0/pg/seeker/upload -k
     *
     * @param fileInputStream          the stream to the object being uploaded
     * @param sessionID the session identifier / security for this user
     * @return a proper 200 response on success
     */
    @POST
    @Path("document/{sessionID}/{url}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response upload(@Context HttpServletRequest request,
                           @FormDataParam("file") final InputStream fileInputStream,
                           @PathParam("sessionID") String sessionID,
                           @PathParam("url") String url) {

        if ( !slDocumentActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        if ( sessionID == null ) {
            return Response.status(500).entity(new JsonMessage("invalid parameters")).build();
        }
        try {
            // check session
            User user = userService.getUser(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("document/upload invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else {
                try {
                    // put the document into Cassandra
                    storeDocument(fileInputStream, UUID.fromString(sessionID), url, request.getRemoteAddr());

                    // and start its processing
                    // get the document
                    Document document = documentService.getDocument(UUID.fromString(sessionID), url, request.getRemoteAddr());
                    if ( document != null ) {
                        // get the document they're talking about
                        logger.debug("document/upload starting upload");
                        documentOrchestrator.offer(Hazelcast.QueueType.Convert, new DocumentAction(user.getOrganisation_id(), document.getUrl()));
                    }

                    // all is ok - return
                    logger.debug("document/upload complete " + sessionID + ", " + url);
                    return Response.status(200).entity(new JsonMessage("document successfully uploaded",null)).build();

                } catch (IOException ex) {
                    return Response.status(500).entity(new JsonMessage("IO Exception:" + ex.getMessage())).build();
                }
            }
        } catch (Exception ex) {
            logger.error("document/upload", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


    /**
     * teach the system a new bit of information
     * @param request the http request
     * @param sessionIDStr the session's id
     * @param text the text to teach
     * @return ok if it all went well
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("teach/{sessionID}/{text}")
    public Response teach( @Context HttpServletRequest request,
                           @PathParam("sessionID") String sessionIDStr,
                           @PathParam("text") String text ) {

        if ( !slDocumentActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            // check session
            UUID sessionID = UUID.fromString(sessionIDStr);
            User user = userService.getUser(sessionID, request.getRemoteAddr());
            if (user == null) {
                logger.debug("document/teach invalid session (" + sessionIDStr + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else {

                // create teach metadata
                String email = user.getEmail();
                String url = email + "://" + UUID.randomUUID() + ".txt";
                Document document = new Document();
                document.setUrl(url);
                document.setOrigin(email);
                document.getName_value_set().put( Document.META_ORIGIN, email );
                String dateTimeStr = DateTime.now().toString("yyyy-MM-dd hh:mm:ss");
                document.getName_value_set().put( Document.META_CREATED_DATE_TIME, dateTimeStr );
                document.getName_value_set().put( Document.META_AUTHOR, user.getFullname() );
                document.setOrganisation_id(user.getOrganisation_id());
                document.getAcl_set().add( new Acl(email, true));
                documentService.saveDocument( UUID.fromString(sessionIDStr), document, request.getRemoteAddr() );

                // index a document for the ui
                documentIndexer.index(user.getOrganisation_id(), document);

                // then start the process of the document conversion etc.
                // put the document into Cassandra
                documentService.uploadDocument(sessionID, url, text.getBytes(), request.getRemoteAddr());

                // and start its processing
                // get the document
                // get the document they're talking about
                logger.debug("document/teach starting processing");
                documentOrchestrator.offer(Hazelcast.QueueType.Convert, new DocumentAction(user.getOrganisation_id(), document.getUrl()));

                return Response.status(200).entity(new JsonMessage("ok", null)).build();
            }

        } catch ( Exception ex ) {
            logger.error("PUT: document/teach: ",ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


    /**
     * start processing a set of documents
     * @param request the http request
     * @param sessionID the session id of the user
     * @param url the url of the document
     * @return success or failure
     */
    @POST
    @Path("start/{sessionID}/{url}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response start(@Context HttpServletRequest request,
                          @PathParam("sessionID") String sessionID,
                          @PathParam("url") String url) {

        if ( !slDocumentActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        if ( sessionID == null ) {
            return Response.status(500).entity(new JsonMessage("invalid parameters")).build();
        }
        try {
            // check session
            User user = userService.getUser(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("document/start invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else {
                // get the document
                Document document = documentService.getDocument(UUID.fromString(sessionID), url, request.getRemoteAddr());
                if ( document != null ) {
                    // get the document they're talking about
                    documentOrchestrator.offer(Hazelcast.QueueType.Convert, new DocumentAction(user.getOrganisation_id(), document.getUrl()));
                    // all is ok - return
                    logger.debug("document/start complete " + sessionID + ", " + url);
                    return Response.status(200).entity(new JsonMessage("document started processing",null)).build();
                } else {
                    // all is ok - return
                    logger.debug("document/start not found " + sessionID + ", " + url);
                    return Response.status(400).entity(new JsonMessage("document not found")).build();
                }
            }
        } catch (Exception ex) {
            logger.error("document/start", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


    /**
     * file upload helper.  Read the file from the input stream, and using uuid / filename store it in Cassandra
     * @param fileInputStream the input stream, the file being uploaded
     * @param sessionID the user's session
     * @param url filename for this file
     * @param ipAddress the ip address of the request
     * @throws IOException anything going wrong with the io
     */
    private void storeDocument(InputStream fileInputStream, UUID sessionID, String url, String ipAddress)
            throws VikiException, IOException, ApplicationException {

        String uuid = UUID.randomUUID().toString();
        if (!serverUploadLocation.endsWith("/"))
            serverUploadLocation = serverUploadLocation + "/";
        String filePath = serverUploadLocation + uuid;

        // save the file to the server
        saveFile(fileInputStream, filePath);
        byte[] fileData = Files.readAllBytes(Paths.get(filePath));
        if (fileData == null || fileData.length == 0) {
            throw new VikiException("invalid file, empty");
        }
        // save the file to cassandra
        documentService.uploadDocument(sessionID, url, fileData, ipAddress);
    }

    /**
     * remove a document for the user by file name
     *
     * @param request  the context of the request for ip purposes
     * @param sessionID the user's session for security purposes
     * @param url the name of the document to remove
     * @return a proper 200 response on success
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("document/{sessionID}/{url}")
    public Response deleteDocument(@Context HttpServletRequest request,
                                   @PathParam("sessionID") String sessionID,
                                   @PathParam("url") String url) {
        if ( !slDocumentActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        if ( sessionID == null ) {
            return Response.status(500).entity(new JsonMessage("invalid parameters")).build();
        }
        try {
            // check session
            User user = userService.getUser(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("document/delete invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else {
                if (url != null) {

                    logger.debug("document/delete deleteDocument(" + sessionID + ", " + url + ")");

                    // remove the document for the stats services

                    documentService.deleteDocument(UUID.fromString(sessionID), url, request.getRemoteAddr());
                    documentIndexer.unindexDocument(user.getOrganisation_id(), url);

                    return Response.status(200).entity(new JsonMessage("document deleted successfully",null)).build();

                } else
                    return Response.status(500).entity(new JsonMessage("invalid parameters")).build();
            }
        } catch (Exception ex) {
            logger.error("document/delete", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }

    /**
     * save uploaded file to a defined location on the server
     *
     * @param uploadedInputStream the stream to the object being uploaded
     * @param serverLocation      the location on the server to use
     */
    private void saveFile(InputStream uploadedInputStream, String serverLocation) throws IOException {
        try {
            OutputStream outputStream;
            int read;
            byte[] bytes = new byte[1024];
            long totalSize = 0;
            long maxSize = (long) maxUploadSizeInMB * 1024_000L;
            outputStream = new FileOutputStream(new File(serverLocation));
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
                totalSize = totalSize + read;
                // only check if positive
                if (maxSize > 0 && totalSize > maxSize)
                    throw new IOException("file exceeds maximum allowed size of " + maxUploadSizeInMB + "MB");
            }
            outputStream.flush();
            outputStream.close();

            logger.info("saved " + totalSize + " bytes to " + serverLocation);

        } catch (IOException ex) {
            logger.error("upload.saveFile", ex);
        }
    }

    /**
     * get a paginated list of documents
     * @param request the request object
     * @param sessionIDStr the session of the active user
     * @param prevUrl the previous url
     * @param itemsPerPage number of items per page
     * @param filter a string based filter for search
     * @return a list of documents
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("document-list/{sessionID}/{prevUrl}/{page}/{itemsPerPage}/{filter}")
    public Response documentList( @Context HttpServletRequest request,
                                  @PathParam("sessionID") String sessionIDStr,
                                  @PathParam("prevUrl") String prevUrl,
                                  @PathParam("page") int page,
                                  @PathParam("itemsPerPage") int itemsPerPage,
                                  @PathParam("filter") String filter ) {
        if ( !slDocumentActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( sessionIDStr != null) {
                if ( prevUrl != null && prevUrl.equals("null") ) {
                    prevUrl = null;
                }
                logger.debug("document-list: " + sessionIDStr);
                UUID sessionID = UUID.fromString(sessionIDStr);
                User user = userService.getUser(UUID.fromString(sessionIDStr), request.getRemoteAddr());
                if (user == null) {
                    logger.debug("document-list: invalid session (" + sessionID + ")");
                    return Response.status(500).entity(new JsonMessage("invalid session")).build();
                } else {
                    if (filter == null || filter.compareToIgnoreCase("null") == 0) {
                        DocumentList documentList = documentService.getPaginatedDocumentList(sessionID, prevUrl, itemsPerPage, request.getRemoteAddr());
                        return Response.status(200).entity(documentList).build();
                    } else {
                        List<String> urList = documentIndexer.findDocument( user.getOrganisation_id(), filter );
                        DocumentList documentList = documentService.getPaginatedDocumentList( user.getOrganisation_id(), urList, page, itemsPerPage );
                        return Response.status(200).entity(documentList).build();
                    }
                }
            } else {
                logger.debug("document-list: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex){
            logger.debug("document-list " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("document-list", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


    // helper - load binary from resource
    private byte[] loadBinaryFromResource(String resourcePath) throws IOException {
        InputStream in = getClass().getResourceAsStream(resourcePath);
        if (in != null ) {
            return IOUtils.toByteArray(in);
        }
        return null;
    }


}
