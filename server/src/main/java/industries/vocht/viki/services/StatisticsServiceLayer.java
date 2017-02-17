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

import com.hazelcast.core.IMap;
import industries.vocht.viki.IHazelcast;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.system_stats.DocumentWordCount;
import industries.vocht.viki.system_stats.GeneralStatistics;
import industries.vocht.viki.jersey.JsonMessage;
import industries.vocht.viki.model.*;
import industries.vocht.viki.model.user.User;
import industries.vocht.viki.system_stats.NameValue;
import industries.vocht.viki.system_stats.NameValueList;
import industries.vocht.viki.utility.SentenceFromBinary;
import io.swagger.annotations.Api;
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
import java.util.*;

/**
 * Created by peter on 8/05/16.
 *
 * service layer for getting statistics from Viki
 *
 */
@Component
@Path("/viki/statistics")
@Api(tags = "/viki/statistics")
public class StatisticsServiceLayer extends ServiceLayerCommon {

    final Logger logger = LoggerFactory.getLogger(StatisticsServiceLayer.class);

    // message to the user if this service layer isn't active
    private static final String sl_inactive_message = "statistics service layer not active on this node";

    @Value("${sl.stats.activate:true}")
    private boolean slStatsActive;

    @Autowired
    private DocumentWordCount documentWordCount;

    @Autowired
    private IHazelcast hazelcast;


    /**
     * get the system's general statistics (counts for most items)
     * @param request the http request
     * @param sessionID the user's session object
     * @return a general statistics object with the data you need!
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("general/{sessionID}")
    public Response generalStats(@Context HttpServletRequest request,
                                 @PathParam("sessionID") String sessionID) {
        if ( !slStatsActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            // check session
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("statistics/general invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else {
                return Response.status(200).entity(documentWordCount.getGeneralStatistics(user.getOrganisation_id())).build();
            }
        } catch (Exception ex) {
            logger.error("statistics/general", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }



    /**
     * get the system's index statistics - top x indexes
     * @param request the http request
     * @param sessionID the user's session object
     * @return an index stats object with the data you need!
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("index/{sessionID}/{count}")
    public Response indexStats(@Context HttpServletRequest request,
                               @PathParam("sessionID") String sessionID,
                               @PathParam("count") int count ) {
        if ( !slStatsActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            // check session
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("statistics/general invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else if ( count > 0 ) {

                IMap<String, Long> map = hazelcast.getWordCountMap(user.getOrganisation_id(), Document.META_BODY);
                List<NameValue> nameValueList = new ArrayList<>();
                if ( map != null ) {
                    for ( String key : map.keySet() ) {
                        if ( key.startsWith("{s:") ) {
                            Long value = map.get(key);
                            nameValueList.add( new NameValue( key.substring(3, key.length() - 1), value ) );
                        }
                    }
                    Collections.sort(nameValueList);
                }
                int size = nameValueList.size();
                if ( count > size ) {
                    count = size;
                }
                NameValueList nvl = new NameValueList();
                if ( count > 0 ) {
                    nvl.setNameValueList( nameValueList.subList(0, count) );
                }
                return Response.status(200).entity(nvl).build();

            } else {
                return Response.status(500).entity(new JsonMessage("invalid count:" + count)).build();
            }
        } catch (Exception ex) {
            logger.error("statistics/general", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }



    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("document/{sessionID}/{url}")
    public Response documentStats(@Context HttpServletRequest request,
                                  @PathParam("sessionID") String sessionID,
                                  @PathParam("url") String url ) {
        if ( !slStatsActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            // check session
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("statistics/document invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else {

                logger.debug("statistics/document " + url);
                Document document = dao.getDocumentDao().read(user.getOrganisation_id(), url);
                if ( document != null ) {
                    // get the binary data of the parsed document
                    SentenceFromBinary converter = new SentenceFromBinary();
                    Map<String, byte[]> documentMap = dao.getDocumentDao().getDocumentParseTreeMap(user.getOrganisation_id(), url);
                    byte[] bodyBytes = documentMap.get( Document.META_BODY );
                    if ( bodyBytes != null ) {
                        List<Sentence> sentenceList = converter.convert(bodyBytes); // convert to sentences
                        if (sentenceList != null) {
                            GeneralStatistics stats = documentWordCount.getDocumentStatistics(sentenceList);
                            return Response.status(200).entity(stats).build();
                        }
                    }
                    GeneralStatistics stats = new GeneralStatistics(); // empty set
                    return Response.status(200).entity(stats).build();
                } else {
                    logger.error("statistics/document 404: " + url + " not found");
                    return Response.status(404).entity(new JsonMessage(url + " not found")).build();
                }

            }
        } catch (Exception ex) {
            logger.error("statistics/document", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }




}




