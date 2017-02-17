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

package industries.vocht.viki.agent_common.client_interface;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.net.URLEncoder;

/**
 * Created by peter on 28/03/16.
 *
 * test the document via the client system
 *
 */
public class AgentClientInterface extends AgentClientInterfaceCommon {

    // document system is default document port: 14080, default login port: 10080
    public AgentClientInterface(String loginHost, int loginPort, String documentHost, int documentPort) {
        super(loginHost, loginPort, documentHost, documentPort);
    }

    /**
     * post the document object and its meta-data to the system
     * @param sessionID a valid user session
     * @param origin the origin / source of the document
     * @param url the url of the document
     * @param document the document object in question
     * @return true if the upload was successful
     * @throws IOException
     */
    public boolean metadata(String sessionID, String origin, String url, AgentDocument document ) throws IOException {
        HttpPost post = new HttpPost( documentServerAddress + "/viki/document/metadata/" + URLEncoder.encode(sessionID,"UTF-8") + "/" +
                URLEncoder.encode(origin,"UTF-8") + "/" + URLEncoder.encode(url,"UTF-8"));

        // convert user with extras to json
        String jsonRequest = new ObjectMapper().writeValueAsString(document);

        // set the entity and its mime-type
        StringEntity se = new StringEntity(jsonRequest);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        post.setEntity(se);

        HttpResponse httpResponse = send(post);
        if (httpResponse.getStatusLine().getStatusCode() == 200) { // check 200
            return true;
        } else {
            AgentJsonMessage message = retrieveResourceFromResponse(httpResponse, AgentJsonMessage.class);
            throw new IOException(message.toString());
        }
    }

    /**
     * upload a file (the binary) to the service layer
     * @param sessionID a valid user session
     * @param url the url of the document
     * @param data the data to post (the binary)
     * @return true if the upload was successful
     * @throws IOException
     */
    public boolean upload(String sessionID, String url, byte[] data ) throws IOException {
        HttpPost post = new HttpPost( documentServerAddress + "/viki/document/document/" + URLEncoder.encode(sessionID,"UTF-8") + "/" + URLEncoder.encode(url,"UTF-8"));

        // set the entity and its mime-type
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addPart("file", new ByteArrayBody(data, ContentType.APPLICATION_OCTET_STREAM, url));
        post.setEntity(builder.build());

        HttpResponse httpResponse = send(post);
        if (httpResponse.getStatusLine().getStatusCode() == 200) { // check 200
            return true;
        } else {
            AgentJsonMessage message = retrieveResourceFromResponse(httpResponse, AgentJsonMessage.class);
            throw new IOException(message.toString());
        }
    }

    /**
     * signal the system to start processing a document as supplied
     * @param sessionID a valid user session
     * @param url the url of the document
     * @return true if the start was successful
     * @throws IOException
     */
    public boolean start(String sessionID, String url) throws IOException {
        HttpPost post = new HttpPost( documentServerAddress + "/viki/document/start/" + URLEncoder.encode(sessionID,"UTF-8") + "/" + URLEncoder.encode(url,"UTF-8"));
        HttpResponse httpResponse = send(post);
        if (httpResponse.getStatusLine().getStatusCode() == 200) { // check 200
            return true;
        } else {
            AgentJsonMessage message = retrieveResourceFromResponse(httpResponse, AgentJsonMessage.class);
            throw new IOException(message.toString());
        }
    }

    /**
     * remove a document from the system
     * @param sessionID a valid user session
     * @param url the url of the document
     * @return true if the delete was successful
     * @throws IOException
     */
    public boolean delete(String sessionID, String url) throws IOException {
        HttpDelete delete = new HttpDelete( documentServerAddress + "/viki/document/document/" + URLEncoder.encode(sessionID,"UTF-8") + "/" + URLEncoder.encode(url,"UTF-8"));
        HttpResponse httpResponse = send(delete);
        if (httpResponse.getStatusLine().getStatusCode() == 200) { // check 200
            return true;
        } else {
            AgentJsonMessage message = retrieveResourceFromResponse(httpResponse, AgentJsonMessage.class);
            throw new IOException(message.toString());
        }
    }


}

