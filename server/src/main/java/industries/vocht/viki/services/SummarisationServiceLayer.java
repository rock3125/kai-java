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
import industries.vocht.viki.indexer.SummaryIndexer;
import industries.vocht.viki.jersey.JsonMessage;
import industries.vocht.viki.model.*;
import industries.vocht.viki.model.similar.SimilarDocument;
import industries.vocht.viki.model.similar.SimilarDocumentSet;
import industries.vocht.viki.model.similar.SimilarDocumentSetList;
import industries.vocht.viki.model.summary.SummarisationFragment;
import industries.vocht.viki.model.summary.SummarisationItem;
import industries.vocht.viki.model.summary.SummarisationItemList;
import industries.vocht.viki.model.summary.SummarisationSet;
import industries.vocht.viki.model.user.User;
import industries.vocht.viki.summarize.Summarize;
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
import java.util.*;

/**
 * Created by peter on 29/03/16.
 *
 * summarising of text
 *
 */
@Component
@Path("/viki/summarization")
@Api(tags = "/viki/summarization")
public class SummarisationServiceLayer extends ServiceLayerCommon {

    final Logger logger = LoggerFactory.getLogger(SummarisationServiceLayer.class);

    // message to the user if this service layer isn't active
    private static final String sl_inactive_message = "summarisation service layer not active on this node";

    @Autowired
    private SummaryIndexer summaryIndexer;

    @Value("${sl.summarisation.activate:true}")
    private boolean slSummarisationActive;

    @Value("${summary.number.of.sentences:10}")
    private int sentenceSummaryTopX;

    // the number of words in a summarisation set for sentences
    @Value("${summary.number.of.words.cutoff:100}")
    private int sentenceSummaryWordLimit;

    @Value("${summary.number.of.words.to.return:10}")
    private int maxSummaryReturnSize;

    public SummarisationServiceLayer() {
    }

    /**
     * perform sentence level text summarisation
     * @param request the http request object
     * @param sessionID the user's session
     * @param url the url of the document to be sentence and textrank summarized
     * @return ok if all goes well
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("summarize/{sessionID}/{url}")
    public Response summarizeSentences(@Context HttpServletRequest request,
                                       @PathParam("sessionID") String sessionID,
                                       @PathParam("url") String url) {
        if ( !slSummarisationActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            // check session
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("summzarize/start invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else {

                Document document = dao.getDocumentDao().read(user.getOrganisation_id(), url);
                if ( document != null ) {
                    List<Sentence> sentenceList = getSentenceList(user.getOrganisation_id(), url);
                    if ( sentenceList != null ) {
                        summarize(user.getOrganisation_id(), url, document, sentenceList);
                        return Response.status(200).entity(new JsonMessage("ok", null)).build();
                    } else {
                        return Response.status(400).entity(new JsonMessage("document parse-tree not found")).build();
                    }
                } else {
                    return Response.status(400).entity(new JsonMessage("document not found")).build();
                }
            }
        } catch (Exception ex) {
            logger.error("summzarize/start", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


    /**
     * retrieve a set of summarization items
     * @param request the http request object
     * @param sessionID the user's session
     * @param urlList a list of urls to return summarisation items for
     * @return 200 if all ok with the result set
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("retrieve/{sessionID}")
    public Response get(@Context HttpServletRequest request,
                        @PathParam("sessionID") String sessionID,
                        StringList urlList ) {
        if ( !slSummarisationActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( urlList == null || urlList.getString_list() == null ) {
                logger.debug("summarization/retrieve @POST invalid string_list (null)");
                return Response.status(500).entity(new JsonMessage("invalid string_list")).build();
            }
            // check session
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null ) {
                logger.debug("summarization/retrieve @POST invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else {
                logger.debug("summarization/retrieve @POST (" + urlList.getString_list().size() + " summary sets)");

                SummarisationItemList summarisationItemList = new SummarisationItemList();
                for ( String url : urlList.getString_list() ) {

                    SummarisationItem summarisationItem = new SummarisationItem();

                    // get similar documents to this one and return them too
                    List<SimilarDocument> similarDocumentList = dao.getDocumentDao().loadSimilarDocumentList(user.getOrganisation_id(), url);
                    if ( similarDocumentList != null && similarDocumentList.size() > 0 ) {
                        summarisationItem.setSimilarDocumentList(similarDocumentList);
                    }

                    Sentence sentence = dao.getDocumentDao().loadDocumentSummarizationSentenceSet(user.getOrganisation_id(), url);
                    if ( sentence != null ) {
                        summarisationItem.setSentence(sentence.toString());
                        summarisationItem.setUrl( url );
                        summarisationItemList.getSummarisationItemList().add(summarisationItem);
                    }

                }
                return Response.status(200).entity(summarisationItemList).build();
            }
        } catch (Exception ex) {
            logger.error("summarization/retrieve @POST", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }




    /**
     * retrieve all similar document i tems
     * @param request the http request object
     * @param sessionID the user's session
     * @return 200 if all ok with the result set
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("retrieve-similar/{sessionID}/{page}/{pageSize}")
    public Response getSimilar( @Context HttpServletRequest request,
                                @PathParam("sessionID") String sessionID,
                                @PathParam("page") int page,
                                @PathParam("pageSize") int pageSize ) {
        if ( !slSummarisationActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( sessionID == null || sessionID.length() == 0 ) {
                logger.debug("summarization/retrieve-similar @GET invalid session-id");
                return Response.status(500).entity(new JsonMessage("invalid session-id")).build();
            }
            // check session
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null ) {
                logger.debug("summarization/retrieve-similar @GET invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else {
                logger.debug("summarization/retrieve-similar @GET similarity sets)");

                List<SimilarDocumentSet> similarDocumentSetList = dao.getDocumentDao().loadSimilarDocumentList(user.getOrganisation_id());
                Collections.sort(similarDocumentSetList);

                // paginate it
                List<SimilarDocumentSet> paginated = paginate( similarDocumentSetList, page, pageSize );

                // get extra details for the paginated set (the documents)
                Map<String, Document> documentMap = new HashMap<>();
                for ( SimilarDocumentSet item : paginated ) {
                    item.setDocument( getDocument( user.getOrganisation_id(), item.getUrl(), documentMap) );
                    for ( SimilarDocument item2 : item.getSimilarDocumentList() ) {
                        item2.setDocumentForUrl2( getDocument( user.getOrganisation_id(), item2.getUrl2(), documentMap) );
                    }
                }

                return Response.status(200).entity(new SimilarDocumentSetList(paginated) ).build();
            }
        } catch (Exception ex) {
            logger.error("summarization/retrieve-similar @POST", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }





    ////////////////////////////////////////////////////////////////////////////////////////
    // helpers

    /**
     * get a paginated set of list
     * @param list the list to paginate
     * @param page the page into the list
     * @param pageSize the size of each page
     * @return a paginated list
     */
    private List<SimilarDocumentSet> paginate( List<SimilarDocumentSet> list, int page, int pageSize ) {
        int startOffset = page * pageSize;
        int endOffset = startOffset + pageSize;
        if ( startOffset < list.size() ) {
            endOffset = Math.min( list.size() - 1, endOffset );
            return list.subList( startOffset, endOffset );
        }
        return new ArrayList<>();
    }

    /**
     * get document with cache
     * @param url the url of the document to get
     * @param cache the cache for the documents
     * @return the document, either from cache or get
     */
    private Document getDocument( UUID organisation_id, String url, Map<String, Document> cache ) throws IOException {
        if ( cache.containsKey(url) ) {
            return cache.get(url);
        } else {
            Document document = dao.getDocumentDao().read(organisation_id, url);
            cache.put( url, document );
            return document;
        }
    }


    /**
     * perform summarization at both text-rank and word sentence level
     * @param organisation_id the organisation
     * @param url the url of the document
     * @param document the documen structure
     * @param sentenceList a list of sentences parsed of the document
     */
    private void summarize(UUID organisation_id, String url, Document document, List<Sentence> sentenceList) throws Exception {

        // setup the summarizer without thematic use
        Summarize summarize = new Summarize( sentenceList, null );
        List<Sentence> topX = summarize.calculateTopX(sentenceSummaryTopX);
        if ( topX != null && topX.size() > 0 ) {

            // collect the top "sentenceSummaryWordLimit" words
            Sentence sentence = new Sentence();

            List<Token> tokenList = new ArrayList<>();
            for ( Sentence sentence1 : topX ) {
                tokenList.addAll(sentence1.getTokenList());
            }

            // cut off at sentence-summary-word limit to form one large block of summarisation text
            if ( tokenList.size() > sentenceSummaryWordLimit ) {
                sentence.getTokenList().addAll( tokenList.subList(0, sentenceSummaryWordLimit) );
            } else {
                sentence.getTokenList().addAll( tokenList );
            }

            dao.getDocumentDao().saveDocumentSummarizationSentenceSet(organisation_id, url, sentence );

            // index these top words for search
            summaryIndexer.summaryIndexDocument(organisation_id, url, document.getAclHash(), sentence);

            // update date of processing
            document.setTs_summarised(System.currentTimeMillis());
            dao.getDocumentDao().update(organisation_id, document);
        }

    }


}

