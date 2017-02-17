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

package industries.vocht.viki.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import industries.vocht.viki.jersey.JsonMessage;
import industries.vocht.viki.model.knowledge_base.KBEntry;
import industries.vocht.viki.model.knowledge_base.KBEntryList;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 27/03/16.
 *
 * a client interface into the admin services system
 *
 */
public class KBClientInterface extends ClientInterfaceCommon {

    // admin system is default port 10080
    public KBClientInterface(String host, int port ) {
        super(host, port);
    }

    /**
     * create an entity in the system
     * @param sessionID the session for the user
     * @param entity the entity to create
     * @return the created entity
     */
    public KBEntry createEntity(UUID sessionID, KBEntry entity) throws IOException {
        // post an entity to the service
        HttpPost post = new HttpPost( serverAddress + "/viki/kb/entry/" + URLEncoder.encode(sessionID.toString(),"UTF-8"));

        // create an entity and convert it to json
        String jsonRequest = new ObjectMapper().writeValueAsString(entity);

        // set the entity and its mime-type
        StringEntity se = new StringEntity(jsonRequest);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        post.setEntity(se);

        // When
        HttpResponse httpResponse = send(post);

        // Then
        if (httpResponse.getStatusLine().getStatusCode() == 200) { // check 200
            return retrieveResourceFromResponse(httpResponse, KBEntry.class);
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
     * create an entity in the system
     * @param sessionID the session for the user
     * @param entity the entity to create
     */
    public void updateEntity(UUID sessionID, KBEntry entity) throws IOException {
        HttpPut put = new HttpPut( serverAddress + "/viki/kb/entry/" + URLEncoder.encode(sessionID.toString(),"UTF-8") + "/" +
                URLEncoder.encode(entity.getId().toString(),"UTF-8"));

        String jsonRequest = new ObjectMapper().writeValueAsString(entity);

        // set the entity and its mime-type
        StringEntity se = new StringEntity(jsonRequest);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        put.setEntity(se);

        // When
        HttpResponse httpResponse = send(put);

        // Then
        if (httpResponse.getStatusLine().getStatusCode() != 200) { // check 200
            try {
                JsonMessage message = retrieveResourceFromResponse(httpResponse, JsonMessage.class);
                throw new IOException(message.toString());
            } catch (Exception ex) {
                throw new IOException(httpResponse.getEntity().toString());
            }
        }
    }



}

