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
import industries.vocht.viki.model.Organisation;
import industries.vocht.viki.model.user.UserWithExtras;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.UUID;

/**
 * Created by peter on 27/03/16.
 *
 * a client interface into the admin services system
 *
 */
public class SecurityClientInterface extends ClientInterfaceCommon {

    // admin system is default port 10080
    public SecurityClientInterface(String host, int port ) {
        super(host, port);
    }

    /**
     * retrieve an organisation by name if it exists
     * @param organisationName the name of the organisation
     * @return the organisation or null if dne
     * @throws IOException transport exception
     */
    public Organisation getOrganisationByName( String organisationName ) throws IOException {
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
     * activate a user's account
     * @param email the email of the user to activate
     * @param activationID the activation uuid for this user
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



}
