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

package industries.vocht.viki.services;

import industries.vocht.viki.IDao;
import industries.vocht.viki.jersey.JsonMessage;
import industries.vocht.viki.model.StringList;
import industries.vocht.viki.model.user.User;
import io.swagger.annotations.Api;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 14/06/16.
 *
 * service layer for viewing the log of a node
 * EACH SERVICE node needs one of these!!!
 *
 */
@Component
@Path("/viki/view-log")
@Api(tags = "/viki/view-log")
public class ViewLogServiceLayer {

    final Logger logger = LoggerFactory.getLogger(ViewLogServiceLayer.class);

    @Autowired
    private IDao dao;

    @Value("${log.file.location:/var/log/kai/server.log}")
    private String logFileFilename;


    public ViewLogServiceLayer() {
    }

    /**
     * return the last x lines from a log file
     * @param request the http request
     * @param sessionID the session id string
     * @param lastLineCount the number of "last lines" to return, returned in reverse order (most recent first)
     * @return a string list of log file lines
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("log/{sessionID}/{lastLineCount}")
    public Response getLog( @Context HttpServletRequest request,
                            @PathParam("sessionID") String sessionID,
                            @PathParam("lastLineCount") int lastLineCount ) {
        try {
            // check session
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("GET: view-log/log invalid session (" + sessionID + ")");
                return Response.status(500).entity("invalid session").build();
            } else {

                if ( !new File(logFileFilename).exists() ) {
                    return Response.status(404).entity(new JsonMessage("log-file not found:" + logFileFilename)).build();
                } else {
                    List<String> lines = Files.readAllLines(Paths.get(logFileFilename));
                    if ( lines.size() > lastLineCount ) {
                        int previous = lines.size() - lastLineCount;
                        lines = lines.subList(previous, lines.size());
                    }
                    // safe escape each string
                    List<String> escapedStringList = new ArrayList<>();
                    for ( String line : lines ) {
                        escapedStringList.add(StringEscapeUtils.escapeJavaScript(line) );
                    }
                    return Response.status(200).entity(new StringList(escapedStringList)).build();
                }

            }
        } catch (Exception ex) {
            logger.error("GET: view-log/log", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }

}


