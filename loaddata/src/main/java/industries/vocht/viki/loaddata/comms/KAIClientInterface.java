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

package industries.vocht.viki.loaddata.comms;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.jersey.JsonMessage;
import industries.vocht.viki.model.Organisation;
import industries.vocht.viki.model.group.Group;
import industries.vocht.viki.model.knowledge_base.KBEntry;
import industries.vocht.viki.model.knowledge_base.KBEntryList;
import industries.vocht.viki.model.user.UserWithExtras;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 29/07/16.
 *
 * communicate with KAI
 *
 */
public class KAIClientInterface {

    private final static Logger logger = LoggerFactory.getLogger(KAIClientInterface.class);

    private final static int SLEEP_TIME_IN_MS = 25_000;

    private final String serverAddress;

    /**
     * setup ssl based on the port last three digits - if they're 443 the use ssl
     * @param port the port to check
     * @return true if we are to use ssl
     */
    private boolean useSSL( int port ) {
        return (port == 443) || ((port % 1_000) == 443) || ((port % 10_000) == 443);
    }

    // admin system is default port 10080
    public KAIClientInterface(String host, int port ) {
        if ( useSSL(port) ) {
            serverAddress = "https://" + host + ":" + port + "/";
        } else {
            serverAddress = "http://" + host + ":" + port + "/";
        }
    }

    /**
     * retrieve an organisation by name if it exists
     * @param organisationName the name of the organisation
     * @return the organisation or null if dne
     * @throws IOException transport exception
     */
    public Organisation getOrganisationByName(String organisationName ) throws IOException {
        HttpGet get = new HttpGet( serverAddress + "/viki/security/organisation/" + organisationName.replace(" ", "%20"));
        try {
            HttpResponse httpResponse = send(get);
            if (httpResponse.getStatusLine().getStatusCode() == 200) { // check 200
                return retrieveResourceFromResponse(httpResponse, Organisation.class);
            }
        } catch (IOException ex) {
            if ( ex.getMessage() != null && !ex.getMessage().contains("organisation not found")) {
                throw ex;
            }
        }
        return null;
    }


    /**
     * create an organisation with its first user
     * @param organisationName the name of the organisation
     * @param user user object with details
     * @return the organisation object created
     * @throws IOException wrong
     */
    public Organisation createUserOrganisation(String organisationName, UserWithExtras user)
            throws IOException {
        HttpPost post = new HttpPost( serverAddress + "/viki/security/user-organisation/" + organisationName.replace(" ", "%20"));

        // convert user with extras to json
        String jsonRequest = new ObjectMapper().writeValueAsString(user);

        // set the entity and its mime-type
        StringEntity se = new StringEntity(jsonRequest);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        post.setEntity(se);

        // When
        HttpResponse httpResponse = send(post);

        // Then
        if (httpResponse.getStatusLine().getStatusCode() == 200) { // check 200
            return retrieveResourceFromResponse(httpResponse, Organisation.class);
        } else {
            JsonMessage message = retrieveResourceFromResponse(httpResponse, JsonMessage.class);
            throw new IOException(message.toString());
        }
    }

    /**
     * get a user's activation ID
     * @param email the email of the user to activate
     * @throws IOException wrong
     */
    public UUID getUserActivationID( String email ) throws IOException {
        HttpGet hget = new HttpGet( serverAddress + "/viki/security/user-aa/" + URLEncoder.encode(email,"UTF-8") );
        HttpResponse httpResponse = send(hget);
        if (httpResponse.getStatusLine().getStatusCode() != 200) { // check 200
            JsonMessage message = retrieveResourceFromResponse(httpResponse, JsonMessage.class);
            throw new IOException(message.toString());
        } else {
            JsonMessage message = retrieveResourceFromResponse(httpResponse, JsonMessage.class);
            return UUID.fromString(message.getMessage());
        }
    }

    /**
     * activate a user's account
     * @param email the email of the user to activate
     * @param activationID the activation uuid for this user
     * @throws IOException wrong
     */
    public void activateUser( String email, UUID activationID ) throws IOException {
        HttpPost post = new HttpPost( serverAddress + "/viki/security/activate/" + URLEncoder.encode(email,"UTF-8") + "/" +
                URLEncoder.encode(activationID.toString(),"UTF-8"));
        HttpResponse httpResponse = send(post);
        if (httpResponse.getStatusLine().getStatusCode() != 200) { // check 200
            JsonMessage message = retrieveResourceFromResponse(httpResponse, JsonMessage.class);
            throw new IOException(message.toString());
        }
    }

    /**
     * send post with re-try on queue full
     * @param post post
     * @return the response
     * @throws IOException wrong
     */
    private HttpResponse send(HttpRequestBase post) throws IOException {
        boolean error = true;
        HttpResponse httpResponse;
        do {
            httpResponse = HttpClientBuilder.create().build().execute(post);
            if (httpResponse.getStatusLine().getStatusCode() == 200) { // check 200
                error = false;
            } else {
                JsonMessage message = retrieveResourceFromResponse(httpResponse, JsonMessage.class);
                // try again later
                if (message != null && message.getError() != null && message.getError().contains("queue full")) {
                    logger.info("queue full, sleeping " + (SLEEP_TIME_IN_MS / 1000) + " seconds");
                    error = true;
                    try {
                        Thread.sleep(SLEEP_TIME_IN_MS);
                    } catch (InterruptedException ex2) {
                    }
                } else if ( message != null ) {
                    throw new IOException(message.getError());
                }
            }
        } while (error);
        return httpResponse;
    }

    /**
     * login an activated user
     * @param email the email of the user
     * @param password the password of the user
     * @return the user object with session etc.
     * @throws IOException wrong
     */
    public UserWithExtras login(String email, String password ) throws IOException {
        HttpGet get = new HttpGet( serverAddress + "/viki/security/token/" + URLEncoder.encode(email,"UTF-8") + "/" +
                URLEncoder.encode(password,"UTF-8"));
        HttpResponse httpResponse = send(get);
        if (httpResponse.getStatusLine().getStatusCode() == 200) { // check 200
            return retrieveResourceFromResponse(httpResponse, UserWithExtras.class);
        } else {
            JsonMessage message = retrieveResourceFromResponse(httpResponse, JsonMessage.class);
            throw new IOException(message.toString());
        }
    }


    /**
     * create a new group
     * @param sessionID the session
     * @param group the group to create
     * @return the created user object
     * @throws IOException error
     */
    public Group createGroup(UUID sessionID, Group group) throws IOException {
        HttpPost post = new HttpPost( serverAddress + "/viki/group/group/" + URLEncoder.encode(sessionID.toString(),"UTF-8"));

        String jsonRequest = new ObjectMapper().writeValueAsString(group);

        // set the entity and its mime-type
        StringEntity se = new StringEntity(jsonRequest);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        post.setEntity(se);

        HttpResponse httpResponse = send(post);
        if (httpResponse.getStatusLine().getStatusCode() == 200) { // check 200
            return retrieveResourceFromResponse(httpResponse, Group.class);
        } else {
            JsonMessage message = retrieveResourceFromResponse(httpResponse, JsonMessage.class);
            throw new IOException(message.toString());
        }
    }

    /**
     * create a list of entities in the system
     * @param sessionID the session for the user
     * @param entityList the list of entities to create
     * @return the created entity
     * @throws IOException error
     */
    public boolean createEntityList(UUID sessionID, List<KBEntry> entityList) throws IOException {
        // post an entity-list to the service
        HttpPost post = new HttpPost( serverAddress + "/viki/kb/entry-list/" + URLEncoder.encode(sessionID.toString(),"UTF-8"));

        // create an entity and convert it to json
        KBEntryList entityList1 = new KBEntryList(entityList);
        String jsonRequest = new ObjectMapper().writeValueAsString(entityList1);

        // set the entity and its mime-type
        StringEntity se = new StringEntity(jsonRequest);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        post.setEntity(se);

        // When
        HttpResponse httpResponse = send(post);

        // Then
        return (httpResponse.getStatusLine().getStatusCode() == 200); // check 200
    }

    /**
     * post the document object and its meta-data to the system
     * @param sessionID a valid user session
     * @param origin the origin / source of the document
     * @param url the url of the document
     * @param document the document object in question
     * @return true if the upload was successful
     * @throws IOException error
     */
    public boolean metadata(String sessionID, String origin, String url, Document document ) throws IOException {
        HttpPost post = new HttpPost( serverAddress + "/viki/document/metadata/" + URLEncoder.encode(sessionID,"UTF-8") + "/" +
                URLEncoder.encode(origin,"UTF-8") + "/" + URLEncoder.encode(url,"UTF-8"));

        // convert user with extras to json
        String jsonRequest = new ObjectMapper().writeValueAsString(document);

        // set the entity and its mime-type
        StringEntity se = new StringEntity(jsonRequest);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        post.setEntity(se);

        HttpResponse httpResponse = send(post);
        return (httpResponse.getStatusLine().getStatusCode() == 200); // check 200
    }

    /**
     * upload a file (the binary) to the service layer
     * @param sessionID a valid user session
     * @param url the url of the document
     * @param data the data to post (the binary)
     * @return true if the upload was successful
     * @throws IOException error
     */
    public boolean upload(String sessionID, String url, byte[] data ) throws IOException {
        HttpPost post = new HttpPost( serverAddress + "/viki/document/document/" + URLEncoder.encode(sessionID,"UTF-8") + "/" + URLEncoder.encode(url,"UTF-8"));

        // set the entity and its mime-type
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addPart("file", new ByteArrayBody(data, ContentType.APPLICATION_OCTET_STREAM, url));
        post.setEntity(builder.build());

        HttpResponse httpResponse = send(post);
        return (httpResponse.getStatusLine().getStatusCode() == 200); // check 200
    }

    /**
     * tell the system to upload any missing files it has in the standard folder /opt/kai/data/nnet/*
     * to the database - helper for setup
     * @param sessionID the session to use for auth
     * @return true when its ok
     * @throws IOException error
     */
    public boolean startUploadFromFiles(String sessionID) throws IOException {
        HttpPut put = new HttpPut( serverAddress + "/viki/nnet/upload/" + URLEncoder.encode(sessionID,"UTF-8"));
        HttpResponse httpResponse = send(put);
        return (httpResponse.getStatusLine().getStatusCode() == 200); // check 200
    }

    /**
     * helper utility to get a json payload from a response object
     * @param response the response object
     * @param clazz the class of the payload
     * @return the de-serialised object of the payload of type clazz
     * @throws IOException wrong object
     */
    private static <T> T retrieveResourceFromResponse(HttpResponse response, Class<T> clazz) throws IOException {
        String jsonFromResponse = EntityUtils.toString(response.getEntity());
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.readValue(jsonFromResponse, clazz);
    }

}

