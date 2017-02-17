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
import industries.vocht.viki.document.DocumentList;
import industries.vocht.viki.jersey.JsonMessage;
import industries.vocht.viki.model.*;
import industries.vocht.viki.model.k_means.kMeansCluster;
import industries.vocht.viki.model.k_means.kMeansClusterSet;
import industries.vocht.viki.model.user.User;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;


/**
 * Created by peter on 04/05/16.
 *
 * summarising of text
 *
 */
@Component
@Path("/viki/cluster")
@Api(tags = "/viki/cluster")
public class ClusterServiceLayer extends ServiceLayerCommon {

    final Logger logger = LoggerFactory.getLogger(ClusterServiceLayer.class);

    // message to the user if this service layer isn't active
    private static final String sl_inactive_message = "cluster service layer not active on this node";

    @Value("${anomaly.detection.mean.divider:10}")
    private int meanDivider;

    @Value("${kmeans.cluster.size:20}")
    private int kClusterSize;

    @Value("${sl.cluster.activate:true}")
    private boolean slClusterActive;

    public ClusterServiceLayer() {
    }


    /**
     * retrieve the k-means data for the cluster
     *
     * @param request the http request object
     * @param sessionID the session id of the user
     * @return an ok message or a failure message
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("k-means/{sessionID}")
    public Response getKMeans(@Context HttpServletRequest request,
                              @PathParam("sessionID") String sessionID ) {
        if ( !slClusterActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            // check session
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("cluster/k-means GET: invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else {

                // get all the clusters and return them
                kMeansClusterSet set = getKMeansClusterSet( user.getOrganisation_id() );

                // success
                return Response.status(200).entity(set).build();
            }
        } catch (Exception ex) {
            logger.error("cluster/k-means GET: ", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }



    /**
     * retrieve the k-means data for the cluster and sort it for anomalies
     * return null if there are no anomalies
     *
     * @param request the http request object
     * @param sessionID the session id of the user
     * @return an ok message or a failure message
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("k-means-anomalies/{sessionID}/{prevUrl}/{pageSize}")
    public Response getKMeansDocumentAnomalies( @Context HttpServletRequest request,
                                                @PathParam("sessionID") String sessionID,
                                                @PathParam("prevUrl") String prevUrl,
                                                @PathParam("pageSize") int pageSize ) {
        if ( !slClusterActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            // check session
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if ( user == null ) {
                logger.debug("cluster/k-means GET: invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else {

                // set it to null if need be (page 0)
                if ( prevUrl.equals("null") ) {
                    prevUrl = null;
                }

                List<String> anomalyList = dao.getStatisticsDao().getDocumentAnomaliesPaginated( user.getOrganisation_id(), prevUrl, pageSize );

                // get the documents for this set
                List<Document> documentList = new ArrayList<>();
                if ( anomalyList != null ) {
                    for (String url : anomalyList) {
                        Document document = dao.getDocumentDao().read(user.getOrganisation_id(), url);
                        if (document != null) {
                            documentList.add(document);
                        }
                    }
                }

                // success
                return Response.status(200).entity(new DocumentList(documentList)).build();
            }
        } catch (Exception ex) {
            logger.error("cluster/k-means GET: ", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }



    /**
     * retrieve the k-means data for a specific cluster by cluster_id
     *
     * @param request the http request object
     * @param sessionID the session id of the user
     * @return an ok message or a failure message
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("k-means/{sessionID}/{cluster_id}")
    public Response getKMeansById(@Context HttpServletRequest request,
                                  @PathParam("sessionID") String sessionID,
                                  @PathParam("cluster_id") int cluster_id ) {
        if ( !slClusterActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            // check session
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("cluster/k-means GET by id: invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else {

                // get all the clusters and return them
                kMeansCluster cluster = dao.getClusterDao().loadFullClusterItem(user.getOrganisation_id(), cluster_id);
                if ( cluster != null ) {
                    cluster.setCentroid(null); // wipe centroid
                    // success
                    return Response.status(200).entity(cluster).build();
                } else {
                    return Response.status(404).entity(new JsonMessage("cluster with id " + cluster_id + " not found")).build();
                }

            }
        } catch (Exception ex) {
            logger.error("cluster/k-means GET: ", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////
    // helpers

    /**
     * get the k-means cluster set
     * @param organisation_id the organisation to get the set for
     * @return the set of clusters for this organisation sorted by frequency desc.
     * @throws IOException
     */
    private kMeansClusterSet getKMeansClusterSet( UUID organisation_id ) throws IOException {
        // get all the clusters and return them
        kMeansClusterSet set = new kMeansClusterSet();
        for ( int i = 1; i <= kClusterSize; i++ ) {
            kMeansCluster cluster = dao.getClusterDao().loadSummaryClusterItem(organisation_id, i);
            if ( cluster != null ) {
                // setup the cluster for min returns
                cluster.setCentroid(null); // wipe centroid
                cluster.setClusterContents(null); // wipe url contents - should not be there anyways
                set.getkMeansClusterList().add(cluster);
            }
        }

        set.sort(); // sort by member size
        return set;
    }


    /**
     * get the k-means cluster set
     * @param organisation_id the organisation to get the set for
     * @return the set of clusters for this organisation sorted by frequency desc.
     * @throws IOException
     */
    private kMeansClusterSet getKMeansFullClusterSet( UUID organisation_id ) throws IOException {
        // get all the clusters and return them
        kMeansClusterSet set = new kMeansClusterSet();
        for ( int i = 1; i <= kClusterSize; i++ ) {
            kMeansCluster cluster = dao.getClusterDao().loadFullClusterItem(organisation_id, i);
            if ( cluster != null ) {
                // setup the cluster for min returns
                cluster.setCentroid(null); // wipe centroid
                set.getkMeansClusterList().add(cluster);
            }
        }

        set.sort(); // sort by member size
        return set;
    }


    /**
     * get a paginated set of list
     * @param list the list to paginate
     * @param page the page into the list
     * @param pageSize the size of each page
     * @return a paginated list
     */
    private List<UrlValue> paginate( List<UrlValue> list, int page, int pageSize ) {
        int startOffset = page * pageSize;
        int endOffset = startOffset + pageSize;
        if ( startOffset < list.size() ) {
            endOffset = Math.min( list.size() - 1, endOffset );
            return list.subList( startOffset, endOffset );
        }
        return new ArrayList<>();
    }

}


