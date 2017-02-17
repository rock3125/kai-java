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
import industries.vocht.viki.indexer.IndexerGroupFind;
import industries.vocht.viki.jersey.JsonMessage;
import industries.vocht.viki.model.group.Group;
import industries.vocht.viki.model.group.GroupList;
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
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 8/04/16.
 *
 */
@Component
@Path("/viki/group")
@Api(tags = "/viki/group")
public class GroupServiceLayer extends ServiceLayerCommon {

    private final Logger logger = LoggerFactory.getLogger(GroupServiceLayer.class);

    // message to the user if this service layer isn't active
    private static final String sl_inactive_message = "group service layer not active on this node";

    @Value("${sl.group.activate:true}")
    private boolean slGroupActive;

    @Autowired
    private UserService userService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private IndexerGroupFind groupIndexer;

    public GroupServiceLayer() {
    }

    /**
     * create the group object and all its properties
     * @param request http request object
     * @param sessionID session id for a valid security context
     * @param group a group object
     * @return the updated/created user object
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("group/{sessionID}")
    public Response create(@Context HttpServletRequest request,
                           @PathParam("sessionID") String sessionID,
                           Group group ) {
        if ( !slGroupActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( group != null ) {
                // we don't have a session - we need the minimum details for a create
                if ( group.getName() == null || group.getUser_list() == null || group.getUser_list().size() == 0 ) {
                    logger.debug("POST: security/group: invalid parameters");
                    return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
                }
                User user = checkSession( userService, sessionID, request );
                group.setOrganisation_id(user.getOrganisation_id());
                Group newGroup = groupService.createGroup(UUID.fromString(sessionID), group, request.getRemoteAddr());
                groupIndexer.index(newGroup);
                return Response.status(200).entity(newGroup).build();
            } else {
                logger.debug("POST: security/group: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex) {
            logger.debug("POST: security/group: " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("POST: security/group: ",ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


    /**
     * delete a group by name
     * @param request http request object
     * @param sessionID session id for a valid security context
     * @param name the name of the group
     * @return success or fail
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("group/{sessionID}/{name}")
    public Response delete( @Context HttpServletRequest request,
                            @PathParam("sessionID") String sessionID,
                            @PathParam("name") String name) {
        if ( !slGroupActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( name != null ) {
                UUID sid = UUID.fromString(sessionID);
                User user = checkSession( userService, sessionID, request );

                // check the group isn't a pre-defined group name that can't be deleted ever!
                String nameTrim = name.trim();
                if ( nameTrim.toLowerCase().equals("users") || nameTrim.toLowerCase().equals("administrators") ) {
                    logger.debug("DELETE: security/group: cannot delete 'users' nor 'administrators' groups");
                    return Response.status(500).entity(new JsonMessage("cannot delete 'users' nor 'administrators' groups")).build();
                }

                Group groupToDelete = groupService.getGroup(sid, nameTrim, request.getRemoteAddr());
                groupService.deleteGroup(sid, nameTrim, request.getRemoteAddr());
                groupIndexer.unindexGroup(user.getOrganisation_id(), groupToDelete.getName());
                return Response.status(200).entity(new JsonMessage("ok", null)).build();
            } else {
                logger.debug("DELETE: security/group: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex) {
            logger.debug("DELETE: security/group: " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("DELETE: security/group: ",ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


    /**
     * update an existing group object
     * @param request http request object
     * @param sessionID session id for a valid security context
     * @param group the group to update
     * @return success or fail
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("group/{sessionID}")
    public Response update( @Context HttpServletRequest request,
                            @PathParam("sessionID") String sessionID,
                            Group group ) {
        if ( !slGroupActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( group != null ) {
                User user = checkSession( userService, sessionID, request );
                group.setOrganisation_id(user.getOrganisation_id());
                groupService.updateGroup(UUID.fromString(sessionID), group, request.getRemoteAddr());
                groupIndexer.unindexGroup(group.getOrganisation_id(), group.getName());
                groupIndexer.index(group);
                return Response.status(200).entity(group).build();
            } else {
                logger.debug("PUT: security/group: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex) {
            logger.debug("PUT: security/group: " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("PUT: security/group: ",ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


    /**
     * return the list of paginated groups
     * @param request the http request object
     * @param sessionIDStr the session's id
     * @param page the page into the page-set
     * @param itemsPerPage number of items on a page
     * @param filter a string filter for search
     * @return a list of group objects
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("group-list/{sessionID}/{page}/{itemsPerPage}/{filter}")
    public Response userList( @Context HttpServletRequest request,
                              @PathParam("sessionID") String sessionIDStr,
                              @PathParam("page") int page,
                              @PathParam("itemsPerPage") int itemsPerPage,
                              @PathParam("filter") String filter ) {
        if ( !slGroupActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( sessionIDStr != null) {
                logger.debug("security/group-list: " + sessionIDStr);
                UUID sessionID = UUID.fromString(sessionIDStr);
                if ( filter == null || filter.compareToIgnoreCase("null") == 0 ) {
                    GroupList groupList = groupService.getPaginatedGroupList(sessionID, page, itemsPerPage, request.getRemoteAddr());
                    return Response.status(200).entity(groupList).build();
                } else {
                    User user = checkSession( userService, sessionIDStr, request );
                    List<String> groupList = groupIndexer.find( user.getOrganisation_id(), filter );
                    return Response.status(200).entity(groupService.getListFromGroupNames(user.getOrganisation_id(), groupList, page, itemsPerPage)).build();
                }
            } else {
                logger.debug("security/group-list: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex){
            logger.debug("security/group-list " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("security/group-list", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


}

