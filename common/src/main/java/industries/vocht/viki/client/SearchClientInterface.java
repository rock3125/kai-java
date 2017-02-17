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
import industries.vocht.viki.model.indexes.DocumentIndexSet;
import industries.vocht.viki.model.indexes.IIndex;
import industries.vocht.viki.model.indexes.IndexList;
import industries.vocht.viki.model.super_search.ISSearchItem;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

/**
 * Created by peter on 6/06/16.
 *
 */
public class SearchClientInterface extends ClientInterfaceCommon {

    // document system is default port 14080
    public SearchClientInterface(String host, int port ) {
        super(host, port);
    }

    /**
     * search for a sub-set of a super search for the shards
     * @param sessionID the session's id
     * @param searchItem the item expression for the search
     * @return a list of indexes
     * @throws IOException
     */
    public IndexList subSearch( String sessionID, ISSearchItem searchItem ) throws IOException {
        HttpPut put = new HttpPut( serverAddress + "/viki/search/sub-search/" + URLEncoder.encode(sessionID,"UTF-8"));

        // create an entity and convert it to json
        String jsonRequest = new ObjectMapper().writeValueAsString(searchItem);

        // set the entity and its mime-type
        StringEntity se = new StringEntity(jsonRequest);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        put.setEntity(se);

        HttpResponse httpResponse = send(put);
        if (httpResponse.getStatusLine().getStatusCode() == 200) { // check 200
            return retrieveResourceFromResponse(httpResponse, IndexList.class);
        } else {
            JsonMessage message = retrieveResourceFromResponse(httpResponse, JsonMessage.class);
            throw new IOException(message.toString());
        }
    }


    /**
     * search for a sub-set of a super search for the shards
     * @param sessionID the session's id
     * @param searchItem the item expression for the search
     * @return a list of indexes
     * @throws IOException
     */
    public SubSearchThread subSearchParallel( String sessionID, ISSearchItem searchItem, Map<String, DocumentIndexSet> resultSet ) throws IOException {
        HttpPut put = new HttpPut( serverAddress + "/viki/search/sub-search/" + URLEncoder.encode(sessionID,"UTF-8"));

        // create an entity and convert it to json
        String jsonRequest = new ObjectMapper().writeValueAsString(searchItem);

        // set the entity and its mime-type
        StringEntity se = new StringEntity(jsonRequest);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        put.setEntity(se);

        SubSearchThread searchAndWait = new SubSearchThread(put, resultSet);
        searchAndWait.setThread(new Thread(searchAndWait));
        searchAndWait.getThread().start();
        return searchAndWait;
    }

}

