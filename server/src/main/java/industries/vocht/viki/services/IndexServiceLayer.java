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
import industries.vocht.viki.client.RuleClientInterface;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.indexer.TupleIndexer;
import industries.vocht.viki.system_stats.DocumentWordCount;
import industries.vocht.viki.indexer.Indexer;
import industries.vocht.viki.model.cluster.KAIActionType;
import industries.vocht.viki.infrastructure.ClusterInfrastructure;
import industries.vocht.viki.jersey.JsonMessage;
import industries.vocht.viki.model.*;
import industries.vocht.viki.model.rules.RuleItem;
import industries.vocht.viki.model.user.User;
import industries.vocht.viki.nnet.NNetAutoTrainer;
import industries.vocht.viki.utility.SentenceFromBinary;
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
 * Created by peter on 17/10/15.
 *
 * service for managing documents
 *
 */
@Component
@Path("/viki/index")
@Api(tags = "/viki/index")
public class IndexServiceLayer {

    private final Logger logger = LoggerFactory.getLogger(IndexServiceLayer.class);

    // message to the user if this service layer isn't active
    private static final String sl_inactive_message = "index service layer not active on this node";

    @Value("${sl.index.activate:true}")
    private boolean slIndexActive;

    @Autowired
    private IDao dao;

    @Autowired
    private ClusterInfrastructure clusterInfrastructure;

    @Autowired
    private Indexer indexer;

    @Autowired
    private TupleIndexer tupleIndexer;

    @Autowired
    private IHazelcast hazelcast;

    @Autowired
    private NNetAutoTrainer nNetAutoTrainer;

    // how long the below cache will exist for, set to 0 for no cache
    @Value("${rule.cache.longivity.in.minutes:5}")
    private long ruleCacheLongivityInMinutes;
    private List<RuleItem> cachedRuleList = null;
    private long lastTimeChecked = 0L;


    public IndexServiceLayer() {
    }

    /**
     * index a document by url
     *
     * @param request  the context of the request for ip purposes
     * @param url the url of the document to be indexed
     * @return true if successful
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("index/{sessionID}/{url}")
    public Response create(@Context HttpServletRequest request,
                           @PathParam("sessionID") String sessionID,
                           @PathParam("url") String url) {
        if ( !slIndexActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            // check session
            SentenceFromBinary converter = new SentenceFromBinary();
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("index/put invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else {
                logger.debug("index/put " + url);
                Document document = dao.getDocumentDao().read(user.getOrganisation_id(), url);
                if ( document != null ) {

                    // get the binary data
                    Map<String, byte[]> documentMap = dao.getDocumentDao().getDocumentParseTreeMap(user.getOrganisation_id(), url);
                    Map<String, List<Sentence>> documentSentenceMap = new HashMap<>();
                    for ( String key : documentMap.keySet() ) {
                        List<Sentence> sentenceList = converter.convert(documentMap.get(key));
                        if (sentenceList != null) {
                            documentSentenceMap.put( key, sentenceList );
                        }
                    }

                    // process / index the document
                    long indexCount = 0;
                    for ( String key : documentSentenceMap.keySet() ) {

                        indexCount = indexCount + indexer.indexDocument(user.getOrganisation_id(), document.getUrl(), key,
                                document.getAclHash(), documentSentenceMap.get(key));

                        updateHazelcastIndexCount(user.getOrganisation_id(), key, indexCount);
                    }
                    // save it for un-indexing
                    dao.getStatisticsDao().setDocumentIndexCount(user.getOrganisation_id(), url, indexCount);

                    // process / index the document's tuples
                    for ( String key : documentSentenceMap.keySet() ) {
                        List<Sentence> sentenceList = documentSentenceMap.get(key);
                        if ( sentenceList != null ) {
                            for ( Sentence sentence : sentenceList ) {
                                if ( sentence != null && sentence.getTuple() != null ) {
                                    tupleIndexer.indexTuple(user.getOrganisation_id(), sentence.getTuple(), document.getAclHash());
                                }
                            }
                        }
                    }
                    // save it for un-indexing
                    //dao.getStatisticsDao().setDocumentIndexCount(user.getOrganisation_id(), url, indexCount);

                    // update timestamp on the index
                    document.setTs_indexed(System.currentTimeMillis());
                    dao.getDocumentDao().update(user.getOrganisation_id(), document);

                    // do we have any rules that need executing / notifying of new document arrival(s)
                    checkRules(sessionID, user.getOrganisation_id(), document.getUrl());

                    // learn anything we can for new neural network generation
                    nNetAutoTrainer.learnFrom( user.getOrganisation_id(), url, documentSentenceMap );

                    // return success
                    return Response.status(200).entity(new JsonMessage("ok", null)).build();
                } else {
                    return Response.status(400).entity(new JsonMessage("document not found " + url)).build();
                }
            }
        } catch (Exception ex) {
            logger.error("index/put", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


    /**
     * un-index a document by url
     *
     * @param request  the context of the request for ip purposes
     * @param url the url of the document to be un-indexed
     * @return true if successful
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("index/{sessionID}/{url}")
    public Response unindex(@Context HttpServletRequest request,
                            @PathParam("sessionID") String sessionID,
                            @PathParam("url") String url) {
        if ( !slIndexActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            // check session
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("index/delete invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else {
                logger.info("index/delete " + url);
                for ( String metadata : Document.METADATA_SET ) {
                    dao.getIndexDao().removeIndex(user.getOrganisation_id(), url, metadata);
                    // subtract from hazelcast stats
                    long indexCount = dao.getStatisticsDao().getDocumentIndexCount(user.getOrganisation_id(), url);
                    updateHazelcastIndexCount(user.getOrganisation_id(), metadata, -indexCount);
                }
                return Response.status(200).entity(new JsonMessage("ok", null)).build();
            }
        } catch (Exception ex) {
            logger.error("index/delete", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


    /**
     * add or subtract an index count from hazelcast's stores to keep track of how many indexes we have
     * @param organisation_id the organisation to do it for
     * @param count the count
     */
    private void updateHazelcastIndexCount(UUID organisation_id, String metadata, long count ) {
        IMap<String, Long> wordCountMap = hazelcast.getWordCountMap(organisation_id, metadata);
        wordCountMap.lock(DocumentWordCount.WC_TOTAL_INDEX_COUNT);
        Long value2 = wordCountMap.get(DocumentWordCount.WC_TOTAL_INDEX_COUNT);
        try {
            if (value2 == null) {
                value2 = count;
            } else {
                value2 = value2 + count;
            }
            wordCountMap.set(DocumentWordCount.WC_TOTAL_INDEX_COUNT, value2);
        } finally {
            wordCountMap.unlock(DocumentWordCount.WC_TOTAL_INDEX_COUNT);
        }
    }


    /**
     * Check all rules for the arrival of new documents
     *
     */
    private void checkRules(String sessionID, UUID organisation_id, String url) {
        if ( organisation_id != null && url != null ) {
            try {

                // setup the rule-cache if so required
                if ( cachedRuleList == null ) { // first time?
                    synchronized (logger) {
                        cachedRuleList = dao.getRuleDao().getAllOnNewDocumentRules(organisation_id); // the right kind of rules
                        lastTimeChecked = System.currentTimeMillis(); // cache expiry
                    }
                } else if ( ruleCacheLongivityInMinutes <= 0 || (lastTimeChecked + ruleCacheLongivityInMinutes * 60_000) >= System.currentTimeMillis() ) {
                    // otherwise - either no cache, or cache has expired - do a re-get of the data
                    synchronized (logger) {
                        cachedRuleList = dao.getRuleDao().getAllOnNewDocumentRules(organisation_id);
                        lastTimeChecked = System.currentTimeMillis();
                    }
                }

                // execute any cached rules for this url
                if (cachedRuleList != null && cachedRuleList.size() > 0) {
                    synchronized (logger) {
                        for ( RuleItem ruleItem : cachedRuleList ) {
                            // send a message for this url to the system distributed for load
                            RuleClientInterface clientInterface = (RuleClientInterface)clusterInfrastructure.getNextClientRoundRobin(KAIActionType.Rule);
                            clientInterface.executeRuleByName(UUID.fromString(sessionID), ruleItem.getRule_name(), url);
                        }
                    }
                } // if cache setup

            } catch (Exception ex) {
                logger.error("checkRules", ex);
            }
        }
    }




}



