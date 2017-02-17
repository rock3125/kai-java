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
import industries.vocht.viki.document.Document;
import industries.vocht.viki.jersey.JsonMessage;
import industries.vocht.viki.model.*;
import industries.vocht.viki.model.emotions.EmotionalSet;
import industries.vocht.viki.model.emotions.EmotionalSetList;
import industries.vocht.viki.model.search.SearchResult;
import industries.vocht.viki.model.search.SearchResultList;
import industries.vocht.viki.model.user.User;
import industries.vocht.viki.tokenizer.Tokenizer;
import industries.vocht.viki.utility.SentenceFromBinary;
import industries.vocht.viki.vader.VScore;
import industries.vocht.viki.vader.Vader;
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
 * Created by peter on 3/04/16.
 *
 */
@Component
@Path("/viki/emotional")
@Api(tags = "/viki/emotional")
public class AnalysisServiceLayer {

    private final Logger logger = LoggerFactory.getLogger(AnalysisServiceLayer.class);

    // message to the user if this service layer isn't active
    private static final String sl_inactive_message = "analysis service layer not active on this node";

    @Autowired
    private Vader vader;

    @Autowired
    private IDao dao;

    @Value("${sl.analysis.activate:true}")
    private boolean slAnalysisActive;

    // threshold to start and take notice of this document's own thresholds if present
    @Value("${emotional.analysis.vader.threshold.negative:-0.8}")
    private double negativeThreshold;

    // threshold to start and take notice of this document's own thresholds if present
    @Value("${emotional.analysis.vader.threshold.positive:0.8}")
    private double positiveThreshold;



    public AnalysisServiceLayer() {
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("analysis/{sessionID}/{url}")
    public Response analyse(@Context HttpServletRequest request,
                            @PathParam("sessionID") String sessionID,
                            @PathParam("url") String url) {
        if ( !slAnalysisActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            // check session
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("emotional/analysis invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else {
                logger.debug("emotional/analysis (" + url + ")");

                Document document = dao.getDocumentDao().read(user.getOrganisation_id(), url);
                if ( document != null ) {

                    int negativeSentenceId = 0;
                    double documentNegative = 0.0;
                    int positiveSentenceId = 0;
                    double documentPositive = 0.0;

                    // use the lexicon to process the lexicon system
                    SentenceFromBinary converter = new SentenceFromBinary();
                    Map<String, byte[]> documentMap = dao.getDocumentDao().getDocumentParseTreeMap(user.getOrganisation_id(), url);
                    List<Sentence> sentenceList = converter.convert(documentMap.get(Document.META_BODY));
                    if ( sentenceList != null ) {

                        // calculate the standard deviations etc.
                        int sentence_id = 0;
                        for ( Sentence sentence : sentenceList ) {
                            List<Token> tokenList = sentence.getTokenList();
                            VScore vScore = vader.analyseSentence(tokenList);
                            double compound = vScore.getCompound();
                            dao.getIndexDao().indexEmotion( user.getOrganisation_id(), url, sentence_id, compound, document.getAclHash() );

                            // record threshold levels of this document
                            if ( compound < negativeThreshold && compound < documentNegative ) {
                                documentNegative = compound;
                                negativeSentenceId = sentence_id;
                            }
                            if ( compound > positiveThreshold && compound > documentPositive ) {
                                documentPositive = compound;
                                positiveSentenceId = sentence_id;
                            }

                            sentence_id = sentence_id + 1;
                        }

                        dao.getIndexDao().flushIndexes();
                    }

                    // save the emotional threshold levels for this document if it has exceeded some max/min value
                    if ( documentNegative != 0.0 || documentPositive != 0.0 ) {
                        dao.getClusterDao().setDocumentEmotion( user.getOrganisation_id(), url, positiveSentenceId, documentPositive,
                                                                negativeSentenceId, documentNegative );
                    }

                    // update timestamp on the index
                    document.setTs_emotion_analysed(System.currentTimeMillis());
                    dao.getDocumentDao().update(user.getOrganisation_id(), document);

                    return Response.status(200).entity(new JsonMessage("ok", null)).build();

                } else {
                    return Response.status(400).entity(new JsonMessage("document not found " + url)).build();
                }

            }
        } catch (Exception ex) {
            logger.error("emotional/analysis", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }

    /**
     * return a list of positive/negative ordered emotions
     * @param request the http request
     * @param sessionID the session id of the user
     * @param page start page
     * @param pageSize size of each page
     * @param asc ascending or descending (boolean)
     * @return a list of url / emotions
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("positive-negative/{sessionID}/{page}/{pageSize}/{asc}")
    public Response positive_negative(@Context HttpServletRequest request,
                                      @PathParam("sessionID") String sessionID,
                                      @PathParam("page") int page,
                                      @PathParam("pageSize") int pageSize,
                                      @PathParam("asc") boolean asc ) {
        if ( !slAnalysisActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( sessionID == null ) {
                logger.debug("emotional/positive-negative @POST invalid session-id (null)");
                return Response.status(500).entity(new JsonMessage("invalid session-id")).build();
            }
            // check session
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null ) {
                logger.debug("emotional/positive-negative @POST invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else {
                logger.debug("emotional/positive-negative @POST");
                List<UrlValue> urlValueList = dao.getClusterDao().getDocumentEmotion( user.getOrganisation_id(), asc, page * pageSize, pageSize );

                // get details for the urls
                SearchResultList obj = new SearchResultList();
                obj.setPage(page);
                obj.setItems_per_page(page);
                obj.setTotal_document_count(0);
                obj.setOrganisation_id(user.getOrganisation_id());

                if ( urlValueList != null ) {
                    for ( UrlValue value : urlValueList ) {
                        if ( (asc && value.getValue() > 0.0) || (!asc && value.getValue() < 0.0) ) {
                            SearchResult searchResult = processDocumentFragment(dao, user.getOrganisation_id(), value.getUrl(), value.getSentence_id(), value.getValue());
                            obj.getSearch_result_list().add(searchResult);
                        }
                    }
                }

                return Response.status(200).entity(obj).build();
            }
        } catch (Exception ex) {
            logger.error("emotional/positive-negative @POST", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


    /**
     * get data for a set of urls
     *
     * @param request the http request
     * @param sessionID the session id of the user
     * @param urlList the list to get data for
     * @return the response, a set of details for the urls
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("analysis/{sessionID}")
    public Response get(@Context HttpServletRequest request,
                        @PathParam("sessionID") String sessionID,
                        StringList urlList ) {
        if ( !slAnalysisActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( urlList == null || urlList.getString_list() == null ) {
                logger.debug("emotional/analysis @GET invalid string_list (null)");
                return Response.status(500).entity(new JsonMessage("invalid string_list")).build();
            }
            // check session
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null ) {
                logger.debug("emotional/analysis @GET invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else {
                logger.debug("emotional/analysis @GET (" + urlList.getString_list().size() + " emotional sets)");

                EmotionalSetList emotionalSetList = new EmotionalSetList();
                for ( String url : urlList.getString_list() ) {
                    EmotionalSet emotionalSet = dao.getIndexDao().getEmotionSet(user.getOrganisation_id(), url);
                    emotionalSetList.getEmotionalSetList().add( emotionalSet );
                }
                return Response.status(200).entity(emotionalSetList).build();
            }
        } catch (Exception ex) {
            logger.error("emotional/analysis @GET", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////

    /**
     * create a special fragment
     * @param dao the data access layer accesss
     * @param organisation_id the organisation
     * @param url the url of the document to get
     * @param score the score of the document
     * @return a search-result fragment
     * @throws IOException
     */
    private SearchResult processDocumentFragment(IDao dao, UUID organisation_id, String url, int sentence_id, double score ) throws IOException {
        if ( organisation_id != null && url != null ) {

            // first read the parsed document
            Map<String, byte[]> documentData = dao.getDocumentDao().getDocumentParseTreeMap(organisation_id, url);
            byte[] data = documentData.get(Document.META_BODY);
            if (data != null) {

                // convert these trees into a list of tokens corresponding to the offsets
                SentenceFromBinary converter = new SentenceFromBinary();
                List<Token> tokenList = converter.convertToTokenList(data);

                // does it have a title?
                String title = null;
                byte[] titleData = documentData.get(Document.META_TITLE);
                if (titleData != null) {
                    List<Token> titleList = converter.convertToTokenList(titleData);
                    title = new Tokenizer().toString(titleList);
                }

                // does it have an author?
                String author = null;
                byte[] authorData = documentData.get(Document.META_AUTHOR);
                if (authorData != null) {
                    List<Token> authorList = converter.convertToTokenList(authorData);
                    author = new Tokenizer().toString(authorList);
                }

                // does it have a created date/time?
                String created = null;
                byte[] createdData = documentData.get(Document.META_CREATED_DATE_TIME);
                if (createdData != null) {
                    List<Token> createdList = converter.convertToTokenList(createdData);
                    created = new Tokenizer().toString(createdList);
                }

                // show the most emotional sentence as the result
                List<String> highlightStrings = new ArrayList<>();
                byte[] bodyData = documentData.get(Document.META_BODY);
                if (bodyData != null) {
                    List<Sentence> sentenceList = converter.convert(bodyData);
                    if ( sentence_id >= 0 && sentence_id < sentenceList.size() ) {
                        highlightStrings.add( sentenceList.get(sentence_id).toString() );
                    }
                }

                SearchResult searchResult = new SearchResult(url, highlightStrings, (float)score);
                searchResult.setAuthor( author );
                if ( title != null && !title.equals(url) ) {
                    searchResult.setTitle(title);
                }
                searchResult.setCreated_date( created );
                return searchResult;
            }
        }
        return null;
    }


}



