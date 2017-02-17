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

import industries.vocht.viki.model.user.User;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

/**
 * Created by peter on 17/10/15.
 *
 * service for managing documents
 *
 */
@Component
@Path("/viki/echo")
@Api(tags = "/viki/echo")
public class EchoServiceLayer {

    @Autowired
    private IDao dao;

    final Logger logger = LoggerFactory.getLogger(EchoServiceLayer.class);

    public EchoServiceLayer() {
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("test/{sessionID}")
    public Response test(@Context HttpServletRequest request,
                         @PathParam("sessionID") String sessionID) {
        try {
            // check session
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("echo/test invalid session (" + sessionID + ")");
                return Response.status(500).entity("invalid session").build();
            } else {
                return Response.status(200).entity(new JsonMessage("echo",null)).build();
            }
        } catch (Exception ex) {
            logger.error("echo/test", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


}
