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
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 2/04/16.
 *
 * client interface for reports
 *
 *
 */
public class RuleClientInterface extends ClientInterfaceCommon {

    // rule system is default port 10080
    public RuleClientInterface (String host, int port ) {
        super(host, port);
    }

    /**
     * execute a rule
     * @param sessionID the session
     * @param rule_name the rule to execute
     * @throws IOException
     */
    public Report executeRuleByName( UUID sessionID, String rule_name ) throws IOException {
        HttpPut put = new HttpPut( serverAddress + "/viki/rules/exec/" + URLEncoder.encode(sessionID.toString(),"UTF-8") + "/" +
                                   URLEncoder.encode(rule_name,"UTF-8") );
        HttpResponse httpResponse = send(put);
        if (httpResponse.getStatusLine().getStatusCode() == 200) { // check 200
            return retrieveResourceFromResponse(httpResponse, Report.class);
        } else {
            JsonMessage message = retrieveResourceFromResponse(httpResponse, JsonMessage.class);
            throw new IOException(message.toString());
        }
    }

    /**
     * execute a "new arrival" rule for a specified url
     * @param sessionID the session to use
     * @param rule_name the name of the rule
     * @param url the url of the document
     * @throws IOException
     */
    public Report executeRuleByName( UUID sessionID, String rule_name, String url ) throws IOException {
        HttpPut put = new HttpPut( serverAddress + "/viki/rules/exec-arrival/" + URLEncoder.encode(sessionID.toString(),"UTF-8") + "/" +
                URLEncoder.encode(rule_name,"UTF-8") + "/" + URLEncoder.encode(url,"UTF-8") );
        HttpResponse httpResponse = send(put);
        if (httpResponse.getStatusLine().getStatusCode() == 200) { // check 200
            return retrieveResourceFromResponse(httpResponse, Report.class);
        } else {
            JsonMessage message = retrieveResourceFromResponse(httpResponse, JsonMessage.class);
            throw new IOException(message.toString());
        }
    }

    /**
     * turn a csv string list of host1:port1,host2:port2 into an actual set of client interfaces of this type
     * @param ruleEngineCsvList the string to process
     * @return a list of these client interfaces
     */
    public static List<RuleClientInterface> ruleEngineCsvListToClientInterfaceList( String ruleEngineCsvList ) {
        List<RuleClientInterface> ruleEngineClientInterfaceList = new ArrayList<>();
        for ( String ruleEnginePair : ruleEngineCsvList.split(",") ) { // config defines end-points
            String[] parts = ruleEnginePair.split(":");
            if ( parts.length == 2 ) {
                ruleEngineClientInterfaceList.add( new RuleClientInterface(parts[0], Integer.parseInt(parts[1])));
            }
        }
        return ruleEngineClientInterfaceList;
    }


}

