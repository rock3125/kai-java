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
import industries.vocht.viki.document.Document;
import industries.vocht.viki.jersey.JsonMessage;
import industries.vocht.viki.model.indexes.*;
import industries.vocht.viki.model.user.User;
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
 * Created by peter on 20/04/16.
 *
 */
@Component
@Path("/viki/time")
@Api(tags = "/viki/time")
public class TimeServiceLayer {

    private final Logger logger = LoggerFactory.getLogger(TimeServiceLayer.class);

    // message to the user if this service layer isn't active
    private static final String sl_inactive_message = "time sservice layer not active on this node";

    @Value("${sl.time.activate:true}")
    private boolean slTimeActive;

    @Autowired
    private IDao dao;

    public TimeServiceLayer() {
    }

    /**
     * return a time index set for the given date-time range
     * @param request the http request
     * @param sessionID the user security session id
     * @param year1 the start year (required)
     * @param month1 the start month (required)
     * @param day1 the start day (optional, -1 if not required)
     * @param hour1 the hour of day (optional, -1 if not required)
     * @param year2 the end year (required)
     * @param month2 the end month (required)
     * @param day2 the end day (optional, -1 if not required)
     * @param hour2 the hour of day (optional, -1 if not required)
     * @param page the page index
     * @param pageSize the size of each page
     * @return a set of time indexes sorted by time
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("look-at/{sessionID}/{year1}/{month1}/{day1}/{hour1}/{min1}/{year2}/{month2}/{day2}/{hour2}/{min2}/{page}/{pageSize}")
    public Response getTime(@Context HttpServletRequest request,
                            @PathParam("sessionID") String sessionID,
                            @PathParam("year1") int year1,
                            @PathParam("month1") int month1,
                            @PathParam("day1") int day1,
                            @PathParam("hour1") int hour1,
                            @PathParam("min1") int min1,
                            @PathParam("year2") int year2,
                            @PathParam("month2") int month2,
                            @PathParam("day2") int day2,
                            @PathParam("hour2") int hour2,
                            @PathParam("min2") int min2,
                            @PathParam("page") int page,
                            @PathParam("pageSize") int pageSize ) {
        if ( !slTimeActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            // check session
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("time/get invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else {

                TimeGroupingEnum grouping;
                if ( day1 == -1 && hour1 == -1 ) {
                    grouping = TimeGroupingEnum.byDay;
                    logger.debug("time/get (" + year1 + "-" + month1 + " to " + year2 + "-" + month2 + "), page " + page);
                } else if ( hour1 == -1 ) {
                    grouping = TimeGroupingEnum.byHour;
                    logger.debug("time/get (" + year1 + "-" + month1 + "-" + day1 + " to " + year2 + "-" + month2 + "-" + day2 + "), page " + page);
                } else {
                    grouping = TimeGroupingEnum.byMonth;
                    logger.debug("time/get (" + year1 + "-" + month1 + "-" + day1 + " " + hour1 + " to " +
                                                year2 + "-" + month2 + "-" + day2 + " " + hour2 + "), page " + page);
                }

                TimeSelectorSetWithBoundaries set = dao.getIndexDao().getTimeSelectorsForRange(year1, month1, day1, hour1, min1, year2, month2, day2, hour2, min2 );

                List<TimeIndex> indexList = dao.getIndexDao().getIndexListForRange(user.getOrganisation_id(), set);
                TimeUrlSet resultSet = new TimeUrlSet(user.getOrganisation_id());
                if ( indexList != null ) {
                    Map<String, Document> documentHashMap = new HashMap<>();
                    for ( TimeIndex index : indexList ) {
                        String url = index.getUrl();
                        Document document = documentHashMap.get(url);
                        if ( document == null ) {
                            document = dao.getDocumentDao().read(user.getOrganisation_id(), url);
                            documentHashMap.put( url, document );
                        }
                        if ( document != null && document.getAuthor() != null ) {
                            resultSet.add(document.getAuthor(), index);
                        }
                    }
                }
                resultSet.consolidateUrls(grouping); // many to one (take all offsets into one url / time slot)
                resultSet.sort(); // sort by time
                resultSet.paginate(page, pageSize); // paginate

                return Response.status(200).entity(resultSet).build();

            }
        } catch (Exception ex) {
            logger.error("time/get", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


}


