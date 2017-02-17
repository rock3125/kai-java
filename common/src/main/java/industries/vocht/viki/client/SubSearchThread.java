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

import industries.vocht.viki.document.Document;
import industries.vocht.viki.jersey.JsonMessage;
import industries.vocht.viki.model.indexes.DocumentIndexSet;
import industries.vocht.viki.model.indexes.IndexList;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.util.Map;

import static industries.vocht.viki.client.ClientInterfaceCommon.retrieveResourceFromResponse;

/**
 * Created by peter on 6/06/16.
 *
 * run a client sub-search in a thread
 *
 */
public class SubSearchThread implements Runnable {

    private HttpPut put;
    private String exceptionString;
    private final Map<String, DocumentIndexSet> resultSet;
    private Thread thread; // the owner thread when running

    public SubSearchThread(HttpPut put, Map<String, DocumentIndexSet> resultSet) {
        this.put = put;
        this.exceptionString = null;
        this.resultSet = resultSet;
    }

    @Override
    public void run() {
        try {
            HttpResponse httpResponse = HttpClientBuilder.create().build().execute(put);
            if (httpResponse.getStatusLine().getStatusCode() == 200) { // check 200
                IndexList result = retrieveResourceFromResponse(httpResponse, IndexList.class);
                if ( result != null && result.getIndexSet() != null ) {

                    synchronized (resultSet) {
                        Map<String, DocumentIndexSet> indexSet = result.getIndexSet();
                        for ( String key : indexSet.keySet() ) {
                            DocumentIndexSet existing = resultSet.get(key);
                            if ( existing == null ) {
                                resultSet.put(key, indexSet.get(key));
                            } else {
                                existing.combine(indexSet.get(key));
                            }
                        }
                    }

                }
            } else {
                JsonMessage message = retrieveResourceFromResponse(httpResponse, JsonMessage.class);
                exceptionString = message.toString();
            }
        } catch (IOException ex) {
            exceptionString = ex.getMessage();
        }
    }
    public String getExceptionString() {
        return exceptionString;
    }

    public Thread getThread() {
        return thread;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }
}
