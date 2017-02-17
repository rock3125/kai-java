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

import industries.vocht.viki.converter.DocumentConverter;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.jersey.JsonMessage;
import industries.vocht.viki.k_means.Vectorizer;
import industries.vocht.viki.lexicon.ValidPennTypes;
import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.model.Vector;
import industries.vocht.viki.model.k_means.kMeansValue;
import industries.vocht.viki.model.k_means.kMeansValueList;
import industries.vocht.viki.model.user.User;
import industries.vocht.viki.parser.NLParser;
import io.swagger.annotations.Api;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
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
import java.io.InputStream;
import java.util.*;

/**
 * Created by peter on 13/06/16.
 *
 *
 */
@Component
@Path("/viki/document-compare")
@Api(tags = "/viki/document-compare")
public class DocumentCompareServiceLayer {

    final Logger logger = LoggerFactory.getLogger(DocumentCompareServiceLayer.class);

    private final static String base64Marker = "data:text/plain;base64,";

    // message to the user if this service layer isn't active
    private static final String sl_inactive_message = "document comparison service layer not active on this node";

    @Value("${sl.document.comparison.activate:true}")
    private boolean slDocumentComparisonActive;

    @Autowired
    private UserService userService;

    @Autowired
    private DocumentConverter documentConverter;

    @Autowired
    private NLParser parser;

    @Autowired
    private Vectorizer vectorizer;

    @Value("${kmeans.cluster.size:20}")
    private int kClusterSize;

    // valid penn type detector
    private static ValidPennTypes validPennTypes = new ValidPennTypes();

    /**
     * massive document compare - go through all analysis steps for a document to compare it to
     * other existing documents in the system right now and return an ordered list of document urls
     * of the set that is closest to this one
     *
     * @param request the http request object
     * @param fileInputStream the stream to the object being uploaded
     * @param sessionID the session identifier / security for this user
     * @param url the url of the object
     * @param threshold the cosine's threshold [1.0 ... 0.0], the smaller, the closer to identical documents, 1.0 is all documents
     * @param numResults, 0 to return all results
     * @return a proper 200 response on success
     */
    @POST
    @Path("compare/{sessionID}/{url}/{threshold}/{numResults}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response compare(@Context HttpServletRequest request,
                            @FormDataParam("file") final InputStream fileInputStream,
                            @PathParam("sessionID") String sessionID,
                            @PathParam("url") String url,
                            @PathParam("threshold") double threshold,
                            @PathParam("numResults") int numResults ) {

        if ( !slDocumentComparisonActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        if ( sessionID == null || fileInputStream == null ) {
            return Response.status(500).entity(new JsonMessage("invalid parameters")).build();
        }
        try {
            // check session
            User user = userService.getUser(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("document-compare/compare invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else {
                try {

                    byte[] documentContent = IOUtils.toByteArray(fileInputStream);
                    if ( documentContent != null && documentContent.length > 0 ) {

                        // base64?
                        String base64Str = new String(documentContent);
                        if ( base64Str.startsWith(base64Marker)) {
                            base64Str = base64Str.substring(base64Marker.length());
                            documentContent = Base64.decodeBase64(base64Str);
                        }

                        // 1. convert binary to text
                        Map<String, String> documentTextMap = documentConverter.getText(url, documentContent);

                        // 2. convert text to parsed text
                        List<Sentence> sentenceList = parser.parseText( documentTextMap.get(Document.META_BODY) );

                        // 3. vectorize the document
                        Vector vector = vectorizer.getFullVector(sentenceList);

                        // 4. find the closest document(s)
                        List<kMeansValue> closestDocumentList = vectorizer.getClosestDocumentThroughCluster(user.getOrganisation_id(), vector );

                        // filter documents by distance
                        if ( threshold > 0.0 ) {
                            List<kMeansValue> filteredDocumentList = new ArrayList<>();
                            for ( kMeansValue value : closestDocumentList ) {
                                double dist = value.getDistance();
                                if (dist >= 0.0 && dist < threshold) {
                                    filteredDocumentList.add(new kMeansValue(value.getUrl(), dist, 0.0, 0.0));
                                }
                            }
                            closestDocumentList = filteredDocumentList;
                        }

                        // sort by distance
                        Collections.sort(closestDocumentList);
                        for ( kMeansValue value : closestDocumentList ) {
                            value.setDistance( 1.0 - value.getDistance() ); // reverse to display percentage
                        }
                        // paginate?
                        if ( numResults > 0 && numResults < closestDocumentList.size() ) {
                            closestDocumentList = closestDocumentList.subList(0, numResults);
                        }

                        // all is ok - return
                        logger.debug("document-compare/compare complete " + sessionID + ", " + url);
                        return Response.status(200).entity( new kMeansValueList(closestDocumentList) ).build();

                    } else {
                        return Response.status(404).entity(new JsonMessage("IO Exception: document empty / 0 bytes")).build();
                    }


                } catch (IOException ex) {
                    return Response.status(500).entity(new JsonMessage("IO Exception:" + ex.getMessage())).build();
                }
            }
        } catch (Exception ex) {
            logger.error("document-compare/compare", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }






}

