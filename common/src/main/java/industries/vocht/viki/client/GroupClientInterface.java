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
import industries.vocht.viki.model.group.Group;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.UUID;

/**
 * Created by peter on 2/04/16.
 *
 */
public class GroupClientInterface extends ClientInterfaceCommon {

    // document system is default port 14080
    public GroupClientInterface(String host, int port ) {
        super(host, port);
    }

    /**
     * create a new group
     * @param sessionID the session
     * @param group the group to create
     * @return the created user object
     * @throws IOException
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


}

