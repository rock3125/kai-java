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

import industries.vocht.viki.ApplicationException;
import industries.vocht.viki.IDao;
import industries.vocht.viki.jersey.JsonMessage;
import industries.vocht.viki.model.user.User;
import industries.vocht.viki.model.reports.Report;
import industries.vocht.viki.model.reports.ReportList;
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
import java.util.List;

/**
 * Created by peter on 14/05/16.
 *
 * create / view reports
 *
 */
@Component
@Path("/viki/reports")
@Api(tags = "/viki/reports")
public class ReportServiceLayer extends ServiceLayerCommon {

    final Logger logger = LoggerFactory.getLogger(ReportServiceLayer.class);

    // message to the user if this service layer isn't active
    private static final String sl_inactive_message = "report service layer not active on this node";

    @Value("${sl.report.activate:true}")
    private boolean slReportActive;

    @Autowired
    private IDao dao;

    @Autowired
    private UserService userService;


    public ReportServiceLayer() {
    }


    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("report/{sessionID}")
    public Response create(@Context HttpServletRequest request,
                           @PathParam("sessionID") String sessionID,
                           Report report ) {
        if ( !slReportActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( report != null ) {
                // we don't have a session - we need the minimum details for a create
                if ( report.getReport_name() == null ) {
                    logger.debug("PUT: reports/report: invalid parameters");
                    return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
                }

                User user = checkSession( userService, sessionID, request );
                dao.getReportDao().createReport(user.getOrganisation_id(), report);
                return Response.status(200).entity(report).build();

            } else {
                logger.debug("PUT: reports/report: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex) {
            logger.debug("PUT: reports/report: " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("PUT: reports/report: ",ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("report-list/{sessionID}")
    public Response readList(@Context HttpServletRequest request,
                         @PathParam("sessionID") String sessionID ) {
        if ( !slReportActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            User user = checkSession( userService, sessionID, request );
            List<Report> reportList = dao.getReportDao().readReportList(user.getOrganisation_id());
            return Response.status(200).entity(new ReportList(reportList)).build();

        } catch (ApplicationException ex) {
            logger.debug("GET: reports/report-list: " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("GET: reports/report-list: ",ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }



}
