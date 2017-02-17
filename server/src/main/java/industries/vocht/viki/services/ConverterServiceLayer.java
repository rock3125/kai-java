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
import industries.vocht.viki.IDao;
import industries.vocht.viki.IHazelcast;
import industries.vocht.viki.VikiException;
import industries.vocht.viki.converter.DocumentConverter;
import industries.vocht.viki.document_orchestrator.DocumentOrchestrator;
import industries.vocht.viki.hazelcast.DocumentAction;
import industries.vocht.viki.system_stats.DocumentWordCount;
import industries.vocht.viki.jersey.HtmlWrapper;
import industries.vocht.viki.jersey.JsonMessage;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.model.user.User;
import io.swagger.annotations.Api;
import org.joda.time.DateTime;
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
import java.util.Map;
import java.util.UUID;

/**
 * Created by peter on 12/03/16.
 *
 * service for managing documents
 *
 */
@Component
@Path("/viki/convert")
@Api(tags = "/viki/convert")
public class ConverterServiceLayer {

    final Logger logger = LoggerFactory.getLogger(ConverterServiceLayer.class);

    // message to the user if this service layer isn't active
    private static final String sl_inactive_message = "converter service layer not active on this node";

    @Value("${sl.converter.activate:true}")
    private boolean slConverterActive;

    @Autowired
    private DocumentOrchestrator documentOrchestrator;

    @Autowired
    private IDao dao;

    @Autowired
    private DocumentConverter documentConverter;

    @Autowired
    private IHazelcast hazelcast;


    public ConverterServiceLayer() {
    }

    /**
     * view the text of a previously processed document
     *
     * @param request  the context of the request for ip purposes
     * @param url the file name of the document to be viewed
     * @return returns the text of the document if available
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("text/html")
    @Path("text/{sessionID}/{url}")
    public Response text(@Context HttpServletRequest request,
                         @PathParam("sessionID") String sessionID,
                         @PathParam("url") String url) {
        if ( !slConverterActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            // check session
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("convert/text invalid session (" + sessionID + ")");
                return Response.status(500).entity(new HtmlWrapper().wrapH1("invalid session")).build();
            } else {
                logger.debug("convert/text (" + url + ")");
                Document document = dao.getDocumentDao().read(user.getOrganisation_id(), url);
                Map<String, String> documentMap = document.getName_value_set();
                if (documentMap == null) {
                    return Response.status(404).entity(new HtmlWrapper().wrap("map for " + url + " not found, perhaps not yet processed.  please try again later.")).build();
                } else {
                    return Response.status(200).entity(new HtmlWrapper().wrap(mapToHtml(documentMap))).build();
                }
            }
        } catch (Exception ex) {
            logger.error("document/text", ex);
            return Response.status(500).entity(new HtmlWrapper().wrapH1(ex.getMessage())).build();
        }
    }


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
    public Response convert(@Context HttpServletRequest request,
                            @PathParam("sessionID") String sessionID,
                            @PathParam("url") String url) {
        if ( !slConverterActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            // check session
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("convert/start invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid parameters")).build();
            } else {

                if ( convertDocument( user.getOrganisation_id(), url ) ) {
                    // add this document as an entity that can now be parsed
                    documentOrchestrator.offer(IHazelcast.QueueType.Parse, new DocumentAction(user.getOrganisation_id(), url));
                    // return success
                    return Response.status(200).entity(new JsonMessage("ok", null)).build();
                } else {
                    return Response.status(500).entity(new JsonMessage("document binary not found")).build();
                }
            }
        } catch (Exception ex) {
            logger.error("convert/start", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }

    /**
     * convert a document from binary to text
     * @param owner the owner of the document
     * @param url the url / file id of the object
     * @return true if success, false otherwise
     * @throws IOException, SemCoreException
     */
    private boolean convertDocument( UUID owner, String url ) throws IOException, VikiException {

        logger.debug("convert/start (" + owner + "," + url + ")");
        Format format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // read the document binary and document data from cassandra
        logger.info("reading document " + url);
        Document document = dao.getDocumentDao().read(owner, url);
        byte[] data = dao.getDocumentDao().getDocumentBinary(owner, url);

        // convert this document to text
        if (data != null && document != null) {
            logger.info("converting text of " + url);

            Map<String, String> documentTextMap = documentConverter.getText(url, data);

            // always put document features into the map
            document.getName_value_set().put(Document.META_ACLS, document.aclsToPrettyString());
            if ( document.getDate_time_uploaded() == 0L ) {
                document.setDate_time_uploaded(System.currentTimeMillis());
            }
            document.getName_value_set().put(Document.META_UPLOAD_DATE_TIME, format.format(new DateTime(document.getDate_time_uploaded()).toDate()));

            // set the origin
            document.getName_value_set().put(Document.META_ORIGIN, document.getOrigin());

            // add the document's parsed meta-data items
            for ( String key : documentTextMap.keySet() ) {
                document.getName_value_set().put( key, documentTextMap.get(key) );
            }

            // save this document's text
            logger.info("saving text of " + url);
            document.setTs_converted(System.currentTimeMillis()); // update time-stamp
            dao.getDocumentDao().update(owner, document);

            return true;
        } else {
            return false;
        }
    }


    /**
     * pretty print a hashmap to html
     * @param map the hashmap to pretty print
     * @return an html string of the map
     */
    private String mapToHtml(Map<String, String> map) {
        if ( map != null && map.size() > 0 ) {
            StringBuilder sb = new StringBuilder();
            for (String key : map.keySet()) {
                String value = map.get(key);
                sb.append("<h3>").append(key).append("<h3>");
                sb.append("<p>").append(value).append("</p>");
                sb.append("<br/><br/>");
            }
            return sb.toString();
        }
        return "<h2>map empty or null</h2>";
    }


}
