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

import industries.vocht.viki.document.Document;
import industries.vocht.viki.jersey.JsonMessage;
import industries.vocht.viki.k_means.Vectorizer;
import industries.vocht.viki.model.*;
import industries.vocht.viki.model.similar.SimilarDocument;
import industries.vocht.viki.model.similar.SimilarDocumentSet;
import industries.vocht.viki.model.similar.SimilarDocumentSetList;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by peter on 2/04/16.
 *
 * vectorize previously parsed documents
 *
 */
@Component
@Path("/viki/vectorizer")
@Api(tags = "/viki/vectorizer")
public class VectorizeServiceLayer extends ServiceLayerCommon {

    final Logger logger = LoggerFactory.getLogger(ParserServiceLayer.class);

    // message to the user if this service layer isn't active
    private static final String sl_inactive_message = "vectorize service layer not active on this node";

    @Value("${sl.vectorize.activate:true}")
    private boolean slVectorizeActive;

    // simple counter for periodically re-calculating all the vectors
    private Map<UUID, Integer> organisationCounterSet = new HashMap<>();

    @Autowired
    private Vectorizer vectorizer;

    /**
     * convert previously processed document to text
     *
     * @param request  the context of the request for ip purposes
     * @param url the file name of the document to be viewed
     * @return the text of hte converted object
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("start/{sessionID}/{url}")
    public Response vectorize(@Context HttpServletRequest request,
                              @PathParam("sessionID") String sessionID,
                              @PathParam("url") String url) {
        if ( !slVectorizeActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            // check session
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("parser/start invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else {

                Document document = dao.getDocumentDao().read(user.getOrganisation_id(), url);
                if ( document != null ) {
                    List<Sentence> sentenceList = getSentenceList(user.getOrganisation_id(), url);
                    if ( sentenceList != null ) {
                        CompressedVector compressedVector = vectorizer.getCompressedVector(sentenceList);
                        if (compressedVector != null) {

                            dao.getDocumentDao().saveDocumentHistogram(user.getOrganisation_id(), url, compressedVector);
                            document.setTs_vectorised(System.currentTimeMillis());
                            dao.getDocumentDao().update(user.getOrganisation_id(), document);

                            // we have a new vector, check if we need to do some action
                            Integer vectorCounter = organisationCounterSet.get(user.getOrganisation_id());
                            if ( vectorCounter == null ) {
                                vectorCounter = 1;
                            } else {
                                vectorCounter = vectorCounter + 1;
                            }
                            organisationCounterSet.put( user.getOrganisation_id(), vectorCounter );

                            return Response.status(200).entity(new JsonMessage("ok", null)).build();
                        } else {
                            return Response.status(500).entity(new JsonMessage("document vector null")).build();
                        }
                    } else {
                        return Response.status(400).entity(new JsonMessage("document parse-tree not found")).build();
                    }
                } else {
                    return Response.status(400).entity(new JsonMessage("document not found")).build();
                }
            }
        } catch (Exception ex) {
            logger.error("parser/start", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }



    /**
     * return a list of similar documents for a set of urls
     *
     * @param request  the context of the request for ip purposes
     * @param urlList a list of urls to get similar documents for
     * @return a set of similar documents for the given urls if applicable
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("similar/{sessionID}")
    public Response similar(@Context HttpServletRequest request,
                            @PathParam("sessionID") String sessionID,
                            StringList urlList ) {
        if ( !slVectorizeActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( urlList == null || urlList.getString_list() == null ) {
                logger.debug("vectorizer/similar @PUT invalid urlList (null)");
                return Response.status(500).entity(new JsonMessage("invalid string_list")).build();
            }
            // check session
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null ) {
                logger.debug("vectorizer/similar @PUT invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else {
                logger.debug("vectorizer/similar @PUT (" + urlList.getString_list().size() + " emotional sets)");

                SimilarDocumentSetList similarDocumentSetList = new SimilarDocumentSetList();
                for ( String url : urlList.getString_list() ) {
                    List<SimilarDocument> similarDocumentList = dao.getDocumentDao().loadSimilarDocumentList(user.getOrganisation_id(), url);
                    if ( similarDocumentList != null && similarDocumentList.size() > 0 ) {
                        similarDocumentSetList.getSimilarDocumentSetList().add( new SimilarDocumentSet(url, similarDocumentList) );
                    }
                }
                return Response.status(200).entity(similarDocumentSetList).build();
            }
        } catch (Exception ex) {
            logger.error("vectorizer/similar @PUT", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }





}


