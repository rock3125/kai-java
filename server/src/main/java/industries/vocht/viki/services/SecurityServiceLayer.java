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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import industries.vocht.viki.ApplicationException;
import industries.vocht.viki.indexer.IndexerUserFind;
import industries.vocht.viki.infrastructure.ClusterInfrastructure;
import industries.vocht.viki.jersey.JsonMessage;

import industries.vocht.viki.model.Organisation;
import industries.vocht.viki.model.Session;
import industries.vocht.viki.model.knowledge_base.KBEntry;
import industries.vocht.viki.model.user.*;
import industries.vocht.viki.utility.Mailer;
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
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by peter on 17/10/15.
 *
 * service for managing documents
 *
 */
@Component
@Path("/viki/security")
@Api(tags = "/viki/security")
public class SecurityServiceLayer extends ServiceLayerCommon {

    private final Logger logger = LoggerFactory.getLogger(SecurityServiceLayer.class);

    // message to the user if this service layer isn't active
    private static final String sl_inactive_message = "security service layer not active on this node";

    @Value("${sl.security.activate:true}")
    private boolean slSecurityActive;

    // password minimum required size
    public static final int MIN_PASSWORD_LENGTH = 8;

    @Autowired
    private Mailer mailer;

    @Autowired
    private UserService userService;

    @Autowired
    private OrganisationService organisationService;

    @Autowired
    private IndexerUserFind userIndexer;

    @Autowired
    private ClusterInfrastructure clusterInfrastructure;

    @Value("${smtp.activationUrl:https://vocht.industries/#/activate}")
    private String activationUrl;

    @Value("${smtp.resetPasswordUrl:https://vocht.industries/#/resetpassword}")
    private String resetPasswordUrl;

    // overwrite for test systems
    @Value("${send.emails:true}")
    private boolean sendEmails;

    public SecurityServiceLayer() {
    }

    /**
     * login a user and return the user object for the given email (pk) / password
     * @param request the http request object
     * @param email the email of the user (login)
     * @param password the password for this user
     * @return a user object on successful login with the sessionID set
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("token/{email}/{password}")
    public Response token(@Context HttpServletRequest request,
                          @PathParam("email") String email,
                          @PathParam("password") String password) {
        if ( !slSecurityActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if (email != null && password != null && password.trim().length() > 0) {
                logger.debug("security/token " + email);
                UUID sessionID = userService.login(email.trim(), password.trim(), request.getRemoteAddr());
                User user = userService.getUser(sessionID, request.getRemoteAddr());
                if ( user != null ) {
                    UserWithExtras userWithExtras = new UserWithExtras(user); // the response includes the infrastructure
                    userWithExtras.setSessionID(sessionID);
                    userWithExtras.setCluster_address_list(clusterInfrastructure.getAllExternalNodes());
                    userWithExtras.setUser_tab_list(getUserTabList(user.getOrganisation_id()));
                    return Response.status(200).entity(userWithExtras).build();
                } else {
                    return Response.status(500).entity(new JsonMessage("user object not found")).build();
                }
            } else {
                logger.error("security/token: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex) {
            logger.error("security/token" + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch (Exception ex) {
            logger.error("security/token", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }

    /**
     * get an organisation by name
     * @param request the request
     * @param name the name of the organisation
     * @return the organisation object
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("organisation/{name}")
    public Response organisation(@Context HttpServletRequest request,
                                 @PathParam("name") String name) {
        if ( !slSecurityActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if (name != null && name.trim().length() > 0) {
                logger.debug("security/organisation " + name);
                Organisation organisation = organisationService.getOrganisationByName(name);
                if ( organisation != null ) {
                    return Response.status(200).entity(organisation).build();
                } else {
                    return Response.status(500).entity(new JsonMessage("organisation not found")).build();
                }
            } else {
                logger.error("security/organisation: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex) {
            logger.error("security/organisation" + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch (Exception ex) {
            logger.error("security/organisation", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }

    /**
     * logout an existing user that is logged in
     * @param request the http request object
     * @param sessionID the session to logout
     * @return a json message ack
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Path("logout/{sessionID}")
    public Response logout(@Context HttpServletRequest request,
                           @PathParam("sessionID") String sessionID) {
        if ( !slSecurityActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if (sessionID != null) {
                User existingUser = userService.getUser(UUID.fromString(sessionID), request.getRemoteAddr());
                logger.debug("security/logout " + sessionID);
                userService.logout(existingUser.getOrganisation_id(), UUID.fromString(sessionID));
                return Response.status(200).entity(new JsonMessage("ok",null)).build();
            } else {
                logger.debug("security/logout invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (Exception ex) {
            logger.error("security/logout", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }

    /**
     * return the user object for a given session
     * @param request http request object
     * @param sessionID session of the given user
     * @return a User object
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("user/{sessionID}")
    public Response session(@Context HttpServletRequest request,
                            @PathParam("sessionID") String sessionID) {
        if ( !slSecurityActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if (sessionID != null && sessionID.trim().length() > 0) {
                logger.debug("security/session " + sessionID);
                User user = checkSession( userService, sessionID, request );
                UserWithExtras userWithExtras = new UserWithExtras(user);
                userWithExtras.setCluster_address_list( clusterInfrastructure.getAllExternalNodes() );
                userWithExtras.setSessionID(UUID.fromString(sessionID));
                userWithExtras.setUser_tab_list(getUserTabList(user.getOrganisation_id()));
                return Response.status(200).entity(userWithExtras).build();
            } else {
                logger.debug("security/session: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (Exception ex) {
            logger.error("security/session", ex);
            return Response.status(500).entity(new JsonMessage("session: " + ex.getMessage())).build();
        }
    }

    /**
     * return the user's account activation id
     * @param request http request object
     * @param email the user's primary key
     * @return a guid, the user's account activation ID
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("user-aa/{email}")
    public Response getUserAccountActivationID( @Context HttpServletRequest request,
                                                @PathParam("email") String email ) {
        if ( !slSecurityActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if (email != null && email.trim().length() > 0) {
                UUID uuid = dao.getUserDao().getAccountActivation(email);
                if ( uuid != null ) {
                    return Response.status(200).entity(new JsonMessage(uuid.toString(), null)).build();
                } else {
                    return Response.status(404).entity(new JsonMessage("user activation ID not found")).build();
                }
            } else {
                logger.debug("security/session: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (Exception ex) {
            logger.error("security/session", ex);
            return Response.status(500).entity(new JsonMessage("session: " + ex.getMessage())).build();
        }
    }

    /**
     * create the user object and all its properties
     * @param request http request object
     * @param sessionID session id for a valid security context
     * @param user a basic user object with properties to save
     * @return the updated/created user object
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("user/{sessionID}")
    public Response create( @Context HttpServletRequest request,
                            @PathParam("sessionID") String sessionID,
                            UserWithExtras user ) {
        if ( !slSecurityActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( user != null && sessionID != null ) {
                // we don't have a session - we need the minimum details for a create
                if ( user.getEmail() == null || user.getFirst_name() == null || user.getSurname() == null || user.getPassword() == null ) {
                    logger.debug("POST: security/user: invalid parameters");
                    return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
                }
                User existingUser = checkSession( userService, sessionID, request );
                user.setOrganisation_id(existingUser.getOrganisation_id());
                User newUser = userService.createUser(UUID.fromString(sessionID), user, user.getPassword(), request.getRemoteAddr());
                userIndexer.index(newUser);

                // send an email to the user to activate their account if it is not a system user
                if ( sendEmails && !user.isSystem_user() ) {
                    SMTPMessageSender sender = new SMTPMessageSender(userService, mailer);
                    sender.sendActivationMessage(user.getEmail(), activationUrl);
                }

                return Response.status(200).entity(newUser).build();
            } else {
                logger.debug("POST: security/user: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex) {
            logger.debug("POST: security/user: " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("POST: security/user: ",ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }

    /**
     * delete a user object (can't be primary user, or the logged in user)
     * @param request http request object
     * @param sessionID session id for a valid security context
     * @param email the email address of the user to delete
     * @return ok
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("user/{sessionID}/{email}")
    public Response delete( @Context HttpServletRequest request,
                            @PathParam("sessionID") String sessionID,
                            @PathParam("email") String email) {
        if ( !slSecurityActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( email != null ) {
                UUID sid = UUID.fromString(sessionID);
                User user = checkSession( userService, sessionID, request );
                if ( user.getEmail().compareToIgnoreCase(email) == 0 ) {
                    logger.debug("DELETE: security/user: you cannot delete the currently logged in user");
                    return Response.status(500).entity(new JsonMessage("you cannot delete the currently logged in user")).build();
                }
                User userToDelete = userService.getUserByEmail(email, request.getRemoteAddr());
                Organisation organisation = organisationService.getOrganisationById(user.getOrganisation_id());
                if ( organisation.getPrimary_user().equals(userToDelete.getId())) {
                    logger.debug("DELETE: security/user: you cannot delete the primary account user");
                    return Response.status(500).entity(new JsonMessage("you cannot delete the primary account user")).build();
                }
                userService.deleteUser(sid, user.getOrganisation_id(), email, request.getRemoteAddr());
                userIndexer.unindexUser(user.getOrganisation_id(), userToDelete.getId());
                return Response.status(200).entity(new JsonMessage("ok", null)).build();
            } else {
                logger.debug("DELETE: security/user: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex) {
            logger.debug("DELETE: security/user: " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("DELETE: security/user: ",ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }

    /**
     * update the user object and all its properties
     * @param request http request object
     * @param user a basic user object with properties to save
     * @return the updated/created user object
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("user/{sessionID}")
    public Response update( @Context HttpServletRequest request,
                            @PathParam("sessionID") String sessionID,
                            UserWithExtras user ) {
        if ( !slSecurityActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( user != null ) {
                User existingUser = checkSession( userService, sessionID, request );
                user.setOrganisation_id(existingUser.getOrganisation_id());
                userService.updateUser(UUID.fromString(sessionID), user, user.getPassword(), request.getRemoteAddr());
                userIndexer.unindexUser(user.getOrganisation_id(), user.getId());
                userIndexer.index(user);
                return Response.status(200).entity(user).build();
            } else {
                logger.debug("PUT: security/user: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex) {
            logger.debug("PUT: security/user: " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("PUT: security/user: ",ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }

    /**
     * create a user object and an organisation
     * @param request http request object
     * @param organisationName the name of the new organisation to create
     * @param user a basic user object with properties to save
     * @return the updated/created user object
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("user-organisation/{organisationName}")
    public Response createUserOrganisation( @Context HttpServletRequest request,
                                            @PathParam("organisationName") String organisationName,
                                            UserWithExtras user ) {
        if ( !slSecurityActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( user != null ) {
                // we don't have a session - we need the minimum details for a create
                if ( user.getEmail() == null || user.getFirst_name() == null || user.getSurname() == null ||
                     user.getPassword() == null || organisationName == null ) {
                    logger.debug("security/user-organisation: invalid parameters");
                    return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
                }
                Organisation organisation = organisationService.createOrganisation(new Organisation(organisationName), user, user.getPassword() );
                userIndexer.index(user);

                // send an email to the user to activate their account
                if ( sendEmails ) {
                    SMTPMessageSender sender = new SMTPMessageSender(userService, mailer);
                    sender.sendActivationMessage(user.getEmail(), activationUrl);
                }

                // create the main system user account for the services for this organisation
                UserWithExtras system = new UserWithExtras();
                system.setEmail(organisation.getId().toString() + UserService.SYSTEM_USER_EMAIL_POSTFIX);
                system.setFirst_name(UserService.SYSTEM_USER_FIRST_NAME);
                system.setSurname(UserService.SYSTEM_USER_SURNAME);
                system.setOrganisation_id(organisation.getId());
                system.setPassword(UUID.randomUUID().toString());
                system.setSystem_user(true);
                system.setConfirmed(true);
                system.setCluster_address_list(clusterInfrastructure.getAllExternalNodes());
                userService.createSystemUser(system);

                return Response.status(200).entity(organisation).build();

            } else {
                logger.debug("security/user-organisation: invalid parameters (user obj null)");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex) {
            logger.debug("security/user-organisation: " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("security/user-organisation: ",ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


    /**
     * re-generate the activation request for an account not yet activated
     * @param request http request object
     * @param email - the email of the account to regen
     * @return ok
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("activate-request/{email}")
    public Response reactivate( @Context HttpServletRequest request,
                                @PathParam("email") String email ) {
        if ( !slSecurityActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            // send an email to the user to activate their account
            if ( sendEmails ) {
                SMTPMessageSender sender = new SMTPMessageSender(userService, mailer);
                sender.sendActivationMessage(email, activationUrl);
            }

            return Response.status(200).entity(new JsonMessage("message send", null)).build();
        } catch (ApplicationException ex) {
            logger.debug("security/activate " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("security/activate",ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }

    /**
     * perform account activation from link
     * @param email the email address to verify
     * @param activateID the uid sent out with the original registration request
     * @return an HTML formatted response message
     */
    @POST
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML})
    @Path("activate/{email}/{activateID}")
    public Response activateAccount( @PathParam("email") String email,
                                     @PathParam("activateID") String activateID ) {
        if ( !slSecurityActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if (email != null && activateID != null) {
                logger.debug("security/confirm: " + email + ", " +activateID);
                UUID activationID = UUID.fromString(activateID);
                userService.confirmAccount(email, activationID);
                return Response.status(200).entity(new JsonMessage("your account has been successfully activated.", null)).build();
            } else {
                logger.debug("security/confirm: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex){
            logger.debug("security/confirm " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("security/confirm", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }

    /**
     * account password reset request (via email)
     * @param request http request object
     * @param email - the email of the account to setup for password reset
     * @return 200 ok on success
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("password-reset-request/{email}")
    public Response resetPassword( @Context HttpServletRequest request,
                                   @PathParam("email") String email ) {
        if ( !slSecurityActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            // send an email to the user to activate their account
            if ( sendEmails ) {
                SMTPMessageSender sender = new SMTPMessageSender(userService, mailer);
                sender.sendEmailPasswordResetMessage(email, resetPasswordUrl, request.getRemoteAddr());
            }
            return Response.status(200).entity(new JsonMessage("message send", null)).build();
        } catch ( Exception ex ) {
            logger.error("security/password-reset-request",ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }

    /**
     * user confirms their password reset
     * @param email the email of the user
     * @param resetIDStr the id given to the user for a reset
     * @param newPassword the newly chosen password by the user
     * @return a message announcing the reset
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("password-reset/{email}/{resetID}/{newPassword}")
    public Response confirmPasswordReset( @PathParam("email") String email,
                                          @PathParam("resetID") String resetIDStr,
                                          @PathParam("newPassword") String newPassword ) {
        if ( !slSecurityActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if (email != null && resetIDStr != null && newPassword != null && newPassword.length() >= MIN_PASSWORD_LENGTH ) {
                logger.debug("security/password-reset: " + email + ", " + resetIDStr);
                UUID resetID = UUID.fromString(resetIDStr);
                userService.resetPassword(email, resetID, newPassword);
                return Response.status(200).entity(new JsonMessage("password reset.", null)).build();
            } else {
                if ( newPassword != null && newPassword.length() < MIN_PASSWORD_LENGTH ) {
                    logger.debug("security/password-reset: password too short");
                    return Response.status(500).entity(new JsonMessage("password too short (must be a minimum of " + MIN_PASSWORD_LENGTH + " characters)")).build();
                } else {
                    logger.debug("security/password-reset: invalid parameters");
                    return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
                }
            }
        } catch (ApplicationException ex){
            logger.debug("security/password-reset " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("security/password-reset", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }

    /**
     * return the list of paginated users
     * @param request the http request object
     * @param sessionIDStr the session's id
     * @param page the page into the pageset
     * @param itemsPerPage number of items on a page
     * @param filter a string filter for search
     * @return a list of user objects
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("user-list/{sessionID}/{page}/{itemsPerPage}/{filter}")
    public Response userList( @Context HttpServletRequest request,
                              @PathParam("sessionID") String sessionIDStr,
                              @PathParam("page") int page,
                              @PathParam("itemsPerPage") int itemsPerPage,
                              @PathParam("filter") String filter ) {
        if ( !slSecurityActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( sessionIDStr != null) {
                logger.debug("security/user-list: " + sessionIDStr);
                UUID sessionID = UUID.fromString(sessionIDStr);
                if ( filter == null || filter.compareToIgnoreCase("null") == 0 ) {
                    UserList userList = userService.getPaginatedUserList(sessionID, page, itemsPerPage, request.getRemoteAddr());
                    return Response.status(200).entity(userList).build();
                } else {
                    User user = userService.getUser(sessionID, request.getRemoteAddr());
                    List<String> emailList = userIndexer.findUser( user.getOrganisation_id(), filter );
                    return Response.status(200).entity(userService.getListFromEmails(emailList, page, itemsPerPage)).build();
                }
            } else {
                logger.debug("security/user-list: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex){
            logger.debug("security/user-list " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("security/user-list", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


    /**
     * return the list of paginated active sessions
     * @param request the http request object
     * @param sessionIDStr the session's id
     * @param page the page into the pageset
     * @param itemsPerPage number of items on a page
     * @return a list of session objects
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("session-list/{sessionID}/{page}/{itemsPerPage}")
    public Response sessionList( @Context HttpServletRequest request,
                                 @PathParam("sessionID") String sessionIDStr,
                                 @PathParam("page") int page,
                                 @PathParam("itemsPerPage") int itemsPerPage ) {
        if ( !slSecurityActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( sessionIDStr != null) {
                logger.debug("security/session-list: " + sessionIDStr);
                User user = checkSession( userService, sessionIDStr, request );

                Format format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                List<Session> sessionList = dao.getUserDao().getActiveSessions(user.getOrganisation_id());

                // paginate session list
                int startOffset = page * itemsPerPage;
                int endOffset = startOffset + itemsPerPage;
                List<Session> paginated = new ArrayList<>();
                for ( int i = startOffset; i < endOffset; i++ ) {
                    if ( i < sessionList.size() ) {
                        Session session = sessionList.get(i);
                        session.setPrettyDate( format.format(new Date(session.getLast_access())) );
                        paginated.add( session );
                    }
                }

                return Response.status(200).entity(new SessionList(paginated)).build();

            } else {
                logger.debug("security/session-list: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex){
            logger.debug("security/session-list " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("security/session-list", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


    /**
     * get all visible schema tabs
     * @param organisation_id the organisation
     * @return null if not found, otherwise a list of user displayable tabs
     */
    private List<UserTab> getUserTabList(UUID organisation_id) throws IOException {
        UUID prev = null;
        List<UserTab> userTabList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        List<KBEntry> entryList;
        do {
            entryList = dao.getKBDao().getEntityList(organisation_id, "schema", prev, 10);
            if ( entryList != null && entryList.size() > 0 ) {
                for (KBEntry entry : entryList) {
                    Map<String, Object> protoMap = mapper.readValue(entry.getJson_data(), new TypeReference<Map<String, Object>>() {});
                    if (protoMap.containsKey("name") && protoMap.containsKey("tab_visible") && protoMap.containsKey("tab_name") &&
                            protoMap.containsKey("field_list") ) {
                        String name = (String) protoMap.get("name");
                        boolean tab_visible = (Boolean) protoMap.get("tab_visible");
                        String tab_name = (String) protoMap.get("tab_name");
                        List field_list = (List) protoMap.get("field_list");
                        if (tab_visible && tab_name != null && name != null && field_list.size() > 0) {
                            // filter out the visible fields from the field list
                            List<String> actualFieldList = new ArrayList<>();
                            for ( Object item : field_list ) {
                                if (item instanceof Map) {
                                    Map itemMap = (Map)item;
                                    if (itemMap.containsKey("name") && itemMap.containsKey("indexed") ) {
                                        if (itemMap.get("indexed") instanceof Boolean && (Boolean)itemMap.get("indexed") &&
                                                (itemMap.get("name") instanceof String)) {
                                            actualFieldList.add((String)itemMap.get("name"));
                                        }
                                    }
                                }
                            }
                            UserTab ut = new UserTab(name, tab_name, actualFieldList);
                            if (protoMap.containsKey("html_template")) {
                                ut.setHtml_template((String)protoMap.get("html_template"));
                            }
                            userTabList.add(ut);
                        }
                    }
                }
                prev = entryList.get(entryList.size()-1).getId();
            }
        } while(entryList != null && entryList.size() > 0);
        if (userTabList.size() > 0) {
            return userTabList;
        }
        return null;
    }


}
