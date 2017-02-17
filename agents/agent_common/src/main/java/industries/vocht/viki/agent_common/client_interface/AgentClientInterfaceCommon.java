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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;

/**
 * Created by peter on 28/03/16.
 *
 * common components of the client interface
 *
 */
public class AgentClientInterfaceCommon {

    private final static Logger logger = LoggerFactory.getLogger(AgentClientInterfaceCommon.class);

    protected final static int SLEEP_TIME_IN_MS = 25_000;

    protected final String documentServerAddress;
    protected final String loginServerAddress;

    public AgentClientInterfaceCommon(String loginHost, int loginPort, String documentHost, int documentPort) {
        if ( useSSL(documentPort) ) {
            documentServerAddress = "https://" + documentHost + ":" + documentPort + "/";
        } else {
            documentServerAddress = "http://" + documentHost + ":" + documentPort + "/";
        }
        if ( useSSL(loginPort) ) {
            loginServerAddress = "https://" + loginHost + ":" + loginPort + "/";
        } else {
            loginServerAddress = "http://" + loginHost + ":" + loginPort + "/";
        }
    }

    /**
     * send post with re-try on queue full
     * @param post post
     * @return the response
     * @throws IOException
     */
    protected HttpResponse send(HttpRequestBase post) throws IOException {
        boolean error;
        HttpResponse httpResponse;
        do {
            httpResponse = HttpClientBuilder.create().build().execute(post);
            if (httpResponse.getStatusLine().getStatusCode() == 200) { // check 200
                error = false;
            } else {
                AgentJsonMessage message = retrieveResourceFromResponse(httpResponse, AgentJsonMessage.class);
                // try again later
                if (message != null && message.getError() != null && message.getError().contains("queue full")) {
                    logger.info("queue full, sleeping " + (SLEEP_TIME_IN_MS / 1000) + " seconds");
                    error = true;
                    try {
                        Thread.sleep(SLEEP_TIME_IN_MS);
                    } catch (InterruptedException ex2) {
                    }
                } else {
                    throw new IOException(message.getError());
                }
            }
        } while (error);
        return httpResponse;
    }

    /**
     * setup ssl based on the port last three digits - if they're 443 the use ssl
     * @param port the port to check
     * @return true if we are to use ssl
     */
    protected boolean useSSL( int port ) {
        return (port == 443) || ((port % 1_000) == 443) || ((port % 10_000) == 443);
    }

    /**
     * login an activated user
     * @param email the email of the user
     * @param password the password of the user
     * @return the user object with session etc.
     * @throws IOException
     */
    public AgentUser login(String email, String password ) throws IOException {
        HttpGet get = new HttpGet( loginServerAddress + "/viki/security/token/" + URLEncoder.encode(email,"UTF-8") + "/" +
                URLEncoder.encode(password,"UTF-8"));
        HttpResponse httpResponse = send(get);
        if (httpResponse.getStatusLine().getStatusCode() == 200) { // check 200
            return retrieveResourceFromResponse(httpResponse, AgentUser.class);
        } else {
            AgentJsonMessage message = retrieveResourceFromResponse(httpResponse, AgentJsonMessage.class);
            throw new IOException(message.toString());
        }
    }


    /**
     * helper utility to get a json payload from a response object
     * @param response the response object
     * @param clazz the class of the payload
     * @return the de-serialised object of the payload of type clazz
     * @throws IOException
     */
    public static <T> T retrieveResourceFromResponse(HttpResponse response, Class<T> clazz) throws IOException {
        String jsonFromResponse = EntityUtils.toString(response.getEntity());
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.readValue(jsonFromResponse, clazz);
    }

}
