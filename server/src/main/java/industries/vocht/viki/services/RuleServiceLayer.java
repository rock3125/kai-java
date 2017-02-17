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
import industries.vocht.viki.model.rules.RuleItem;
import industries.vocht.viki.model.rules.RuleList;
import industries.vocht.viki.model.user.User;
import industries.vocht.viki.rules_engine.RuleOrchestrator;
import industries.vocht.viki.rules_engine.model.OrganisationRuleName;
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

/**
 * Created by peter on 8/04/16.
 *
 */
@Component
@Path("/viki/rules")
@Api(tags = "/viki/rules")
public class RuleServiceLayer extends ServiceLayerCommon {

    private final Logger logger = LoggerFactory.getLogger(RuleServiceLayer.class);

    // message to the user if this service layer isn't active
    private static final String sl_inactive_message = "rule service layer not active on this node";

    @Value("${sl.rule.activate:true}")
    private boolean slRuleActive;

    @Autowired
    private UserService userService;

    @Autowired
    private RuleOrchestrator ruleOrchestrator;

    @Autowired
    private IDao dao;

    public RuleServiceLayer() {
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("rule/{sessionID}")
    public Response create(@Context HttpServletRequest request,
                           @PathParam("sessionID") String sessionID,
                           RuleItem ruleItem ) {
        if ( !slRuleActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( ruleItem != null ) {
                User user = checkSession( userService, sessionID, request );
                // must exist
                RuleItem existingRule = dao.getRuleDao().loadRuleByName(user.getOrganisation_id(), ruleItem.getRule_name());
                if ( existingRule != null ) {
                    logger.debug("PUT: rules/rule: a rule with that name already exists, can't create");
                    return Response.status(510).entity(new JsonMessage("a rule with that name already exists, can't create")).build();
                } else {
                    dao.getRuleDao().saveRule(user.getOrganisation_id(), ruleItem.getRule_name(), ruleItem);
                    return Response.status(200).entity(ruleItem).build();
                }
            } else {
                logger.debug("PUT: rules/rule: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex) {
            logger.debug("PUT: rules/rule: " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("PUT: rules/rule: ",ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("rule/{sessionID}")
    public Response update(@Context HttpServletRequest request,
                           @PathParam("sessionID") String sessionID,
                           RuleItem ruleItem ) {
        if ( !slRuleActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( ruleItem != null ) {
                User user = checkSession( userService, sessionID, request );

                // must exist
                RuleItem existingRule = dao.getRuleDao().loadRuleByName(user.getOrganisation_id(), ruleItem.getRule_name());
                if ( existingRule == null ) {
                    logger.debug("POST: rules/rule: no such rule, can't update");
                    return Response.status(404).entity(new JsonMessage("no such rule, can't update")).build();
                } else {
                    dao.getRuleDao().saveRule(user.getOrganisation_id(), ruleItem.getRule_name(), ruleItem);
                    return Response.status(200).entity(ruleItem).build();
                }
            } else {
                logger.debug("POST: rules/rule: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex) {
            logger.debug("POST: rules/rule: " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("POST: rules/rule: ",ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("rule/{sessionID}/{rule_name}")
    public Response delete(@Context HttpServletRequest request,
                           @PathParam("sessionID") String sessionID,
                           @PathParam("rule_name") String rule_name ) {
        if ( !slRuleActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( rule_name != null ) {
                User user = checkSession( userService, sessionID, request );

                // must exist
                RuleItem existingRule = dao.getRuleDao().loadRuleByName(user.getOrganisation_id(), rule_name);
                if ( existingRule == null ) {
                    logger.debug("DELETE: rules/rule: no such rule, can't delete");
                    return Response.status(404).entity(new JsonMessage("no such rule, can't delete")).build();
                } else {
                    dao.getRuleDao().deleteRule(user.getOrganisation_id(), rule_name);
                    return Response.status(200).entity(new JsonMessage("done", null)).build();
                }
            } else {
                logger.debug("DELETE: rules/rule: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex) {
            logger.debug("DELETE: rules/rule: " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("DELETE: rules/rule: ",ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }

    /**
     * return a paginated set of rules
     * @param request the http request object
     * @param sessionIDStr the session id
     * @param prevRuleName pagination previous if present ("null" string otherwise)
     * @param itemsPerPage number of items to return per page
     * @return a paginated list of rule objects
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("rule-list/{sessionID}/{prevRuleName}/{itemsPerPage}")
    public Response ruleList( @Context HttpServletRequest request,
                              @PathParam("sessionID") String sessionIDStr,
                              @PathParam("prevRuleName") String prevRuleName,
                              @PathParam("itemsPerPage") int itemsPerPage ) {
        if ( !slRuleActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( sessionIDStr != null) {
                logger.debug("rule-list: " + sessionIDStr);
                if ( prevRuleName == null || prevRuleName.equals("null") ) {
                    prevRuleName = null;
                }
                User user = checkSession( userService, sessionIDStr, request );
                RuleList list = new RuleList(dao.getRuleDao().getRuleList(user.getOrganisation_id(), prevRuleName, itemsPerPage));
                list.setItems_per_page(itemsPerPage);
                list.setOrganisation_id(user.getOrganisation_id());
                return Response.status(200).entity(list).build();
            } else {
                logger.debug("rule-list: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex){
            logger.debug("rule-list " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("rule-list", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


    /**
     * execute a rule by name
     * @param request the http request object
     * @param sessionIDStr the session id
     * @param rule_name the name of the rule
     * @return ok if all goes well - offers the message to the processor
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Path("exec/{sessionID}/{rule_name}")
    public Response exec( @Context HttpServletRequest request,
                          @PathParam("sessionID") String sessionIDStr,
                          @PathParam("rule_name") String rule_name ) {
        if ( !slRuleActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( sessionIDStr != null && rule_name != null ) {
                logger.debug("exec: " + sessionIDStr);
                User user = checkSession( userService, sessionIDStr, request );
                RuleItem rule = dao.getRuleDao().loadRuleByName(user.getOrganisation_id(), rule_name);
                if ( rule != null ) {
                    ruleOrchestrator.offer(new OrganisationRuleName(user.getOrganisation_id(), rule_name, rule.getCreator()));
                    return Response.status(200).entity(new JsonMessage("ok", null)).build();
                } else {
                    return Response.status(404).entity(new JsonMessage("rule not found")).build();
                }
            } else {
                logger.debug("exec: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex){
            logger.debug("exec " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("exec", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


    /**
     * execute the document arrival
     * @param request the http request
     * @param sessionIDStr the session id
     * @param rule_name the name of the rule to execute
     * @param url the url to execute the rule against
     * @return success if done
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Path("exec-arrival/{sessionID}/{rule_name}/{url}")
    public Response exec_arrival( @Context HttpServletRequest request,
                                  @PathParam("sessionID") String sessionIDStr,
                                  @PathParam("rule_name") String rule_name,
                                  @PathParam("url") String url ) {
        if ( !slRuleActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( sessionIDStr != null && rule_name != null && url != null ) {
                logger.debug("exec-arrival: " + sessionIDStr);
                User user = checkSession( userService, sessionIDStr, request );

                RuleItem rule = dao.getRuleDao().loadRuleByName(user.getOrganisation_id(), rule_name);
                if ( rule != null ) {
                    ruleOrchestrator.offer(new OrganisationRuleName(user.getOrganisation_id(), rule_name, url, rule.getCreator()));
                    return Response.status(200).entity(new JsonMessage("ok", null)).build();
                } else {
                    return Response.status(404).entity(new JsonMessage("rule not found")).build();
                }

            } else {
                logger.debug("exec-arrival: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex){
            logger.debug("exec-arrival " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("exec-arrival", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }




}

