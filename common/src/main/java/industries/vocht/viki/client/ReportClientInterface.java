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
import industries.vocht.viki.model.reports.Report;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
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
 * client interface for reports
 *
 *
 */
public class ReportClientInterface extends ClientInterfaceCommon {

    // document system is default port 14080
    public ReportClientInterface(String host, int port ) {
        super(host, port);
    }

    /**
     * create a report
     * @param sessionID the session
     * @param report the report to create
     * @return the created user object
     * @throws IOException
     */
    public Report createReport(UUID sessionID, Report report) throws IOException {
        HttpPut put = new HttpPut( serverAddress + "/viki/reports/report/" + URLEncoder.encode(sessionID.toString(),"UTF-8"));

        String jsonRequest = new ObjectMapper().writeValueAsString(report);

        // set the entity and its mime-type
        StringEntity se = new StringEntity(jsonRequest);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        put.setEntity(se);

        HttpResponse httpResponse = send(put);
        if (httpResponse.getStatusLine().getStatusCode() == 200) { // check 200
            return retrieveResourceFromResponse(httpResponse, Report.class);
        } else {
            JsonMessage message = retrieveResourceFromResponse(httpResponse, JsonMessage.class);
            throw new IOException(message.toString());
        }
    }


}

