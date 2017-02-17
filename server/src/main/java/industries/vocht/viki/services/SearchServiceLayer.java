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
import industries.vocht.viki.IHazelcast;
import industries.vocht.viki.aiml.AimlManager;
import industries.vocht.viki.aiml.AimlPatternMatcher;
import industries.vocht.viki.aiml.AimlTemplate;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.jersey.JsonMessage;
import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.indexes.IndexList;
import industries.vocht.viki.model.search.SearchObject;
import industries.vocht.viki.model.search.SearchResult;
import industries.vocht.viki.model.search.SearchResultList;
import industries.vocht.viki.model.semantics.TupleResult;
import industries.vocht.viki.model.semantics.TupleResultList;
import industries.vocht.viki.model.super_search.ISSearchItem;
import industries.vocht.viki.model.user.User;
import industries.vocht.viki.model.super_search.SSearchParserException;
import industries.vocht.viki.parser.NLParser;
import industries.vocht.viki.semantic_search.SSearchExecutor;
import industries.vocht.viki.semantic_search.SuperSearch;
import industries.vocht.viki.semantic_search.TupleSearch;
import industries.vocht.viki.semantic_search.ViewSearchEntities;
import industries.vocht.viki.system_stats.AuthorFrequency;
import industries.vocht.viki.system_stats.AuthorFrequencySet;
import industries.vocht.viki.system_stats.DocumentWordCount;
import industries.vocht.viki.tokenizer.Tokenizer;
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
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by peter on 8/04/16.
 *
 * search service
 *
 */
@Component
@Path("/viki/search")
@Api(tags = "/viki/search")
public class SearchServiceLayer extends ServiceLayerCommon {

    private final Logger logger = LoggerFactory.getLogger(SearchServiceLayer.class);

    // message to the user if this service layer isn't active
    private static final String sl_inactive_message = "search service layer not active on this node";

    @Value("${sl.search.activate:true}")
    private boolean slSearchActive;

    // number of documents returned for social content investigations / authors
    @Value("${sl.num.docs.for.social:1000}")
    private int numDocumentsForSocial;

    @Autowired
    private UserService userService;

    @Autowired
    private SuperSearch superSearch;

    @Autowired
    private NLParser nlParser;

    @Autowired
    private SSearchExecutor searchExecutor;

    @Autowired
    private ViewSearchEntities viewSearchEntities;

    @Autowired
    private TupleSearch tupleSearch;

    @Autowired
    private AimlManager aimlManager;

    @Autowired
    private DocumentWordCount documentWordCount;

    @Autowired
    private IHazelcast hazelcast;

    @Autowired
    private KBService kbService;


    public SearchServiceLayer() {
    }

    /**
     * perform a super search
     * @param request the http request object for the search
     * @param sessionIDStr the session id
     * @param page the starting page
     * @param itemsPerPage the number of items to show per page
     * @param searchObject a search object with the complex power search for its search text
     * @return a search result set
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("search/{sessionID}/{page}/{itemsPerPage}/{maxDistanceAllowed}")
    public Response search( @Context HttpServletRequest request,
                            @PathParam("sessionID") String sessionIDStr,
                            @PathParam("page") int page,
                            @PathParam("itemsPerPage") int itemsPerPage,
                            @PathParam("maxDistanceAllowed") int maxDistanceAllowed,
                            SearchObject searchObject ) {
        if ( !slSearchActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {

            if ( searchObject != null && searchObject.getSearch_text() != null && sessionIDStr != null ) {
                User user = checkSession( userService, sessionIDStr, request );
                searchObject.setEmail(user.getEmail());
                logger.info("super-search on \"" + searchObject.getSearch_text() + "\"");

                // all the results
                List<SearchResult> resultList = new ArrayList<>();

                // is this an AIML answer-able entity
                AimlPatternMatcher matcher = new AimlPatternMatcher();
                List<AimlTemplate> aiList = matcher.match(searchObject.getSearch_text(), aimlManager);
                if ( aiList != null && aiList.size() > 0 ) {
                    // cleanup, {name} and {time} need setting as well as items related to KBEntry queries
                    List<String> aiResponseList = aimlManager.evaluate(aiList, kbService, user, documentWordCount, itemsPerPage);
                    logger.info("super-search/ai-query: (" + aiResponseList.size() + " results)");
                    for (String aiResponse : aiResponseList) {
                        List<String> textList = new ArrayList<>();
                        textList.add(aiResponse);
                        resultList.add(new SearchResult("KAI", textList, 1000.0f));
                    }
                }

                // is this a tuple query?
                List<Sentence> querySentenceList = nlParser.parseText(searchObject.getSearch_text());
                if ( isTupleQuery(querySentenceList) ) {
                    logger.info("super-search/tuple-query");
                    Format format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                    HashSet<Integer> accessSet = hazelcast.getUserAclMap(user.getOrganisation_id()).get(user.getEmail());
                    if (accessSet == null) {
                        throw new ApplicationException("invalid session (acccessSet null)");
                    }
                    if (accessSet.size() == 0) { // no acls, never any results
                        logger.info("access-set empty for " + user.getEmail() + " - returning null");
                        return null;
                    }

                    // perform tuple search
                    TupleResultList tupleList = null;
                    try {
                        tupleList = tupleSearch.tupleSearch(user.getOrganisation_id(), accessSet,
                                searchObject.getSearch_text(), page, itemsPerPage);
                    } catch (ApplicationException | SSearchParserException | IOException | InterruptedException ex ) {
                        logger.debug("skipping tupleSearch, not suitable");
                        tupleList = null;
                    }

                    if (tupleList != null && tupleList.getCaseTupleList() != null) {
                        Tokenizer tokenizer = new Tokenizer();
                        for (TupleResult tuple : tupleList.getCaseTupleList()) {
                            Document document = dao.getDocumentDao().read(user.getOrganisation_id(), tuple.getUrl());
                            if (document != null) {

                                SearchResult searchResult = new SearchResult();
                                searchResult.setUrl(tuple.getUrl());
                                searchResult.setAuthor(document.getAuthor());

                                // add highlight string if exists
                                searchResult.getText_list().add(tuple.getTupleText());

                                StringBuilder sb = new StringBuilder();
                                String textStr = "";
                                Map<String, byte[]> map = dao.getDocumentDao().getDocumentParseTreeMap(user.getOrganisation_id(), tuple.getUrl());
                                if (map != null && map.containsKey(Document.META_BODY)) {
                                    SentenceFromBinary sentenceFromBinary = new SentenceFromBinary();
                                    List<Sentence> sentenceList = sentenceFromBinary.convert(map.get(Document.META_BODY));
                                    if (sentenceList != null && tuple.getSentence() < sentenceList.size()) {
                                        textStr = tokenizer.toString(sentenceList.get(tuple.getSentence()).getTokenList());
                                    }
                                }
                                if (textStr.length() > 0) {
                                    sb.append(textStr);
                                }
                                searchResult.getText_list().add(sb.toString());

                                if (document.getCreated() > 0L) {
                                    searchResult.setCreated_date(format.format(new Date(document.getCreated())));
                                }
                                resultList.add(searchResult);
                            }
                        }
                    }
                }

                // normal search
                logger.info("super-search/kw-query");
                SearchResultList searchResultList = superSearch.doSearch(sessionIDStr, user.getEmail(), user.getOrganisation_id(), searchObject,
                        page, itemsPerPage, maxDistanceAllowed);

                if ( searchResultList != null ) {
                    searchResultList.getSearch_result_list().addAll(resultList);
                } else {
                    searchResultList = new SearchResultList(resultList);
                }
                searchResultList.sort(); // re-sort combined results

                return Response.status(200).entity(searchResultList).build();

            } else {
                logger.debug("PUT: search/super-search: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException | SSearchParserException | IOException ex) {
            logger.debug("PUT: search/super-search: " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("PUT: search/super-search: ",ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }

    /**
     * are they keywords like a tuple query?
     * @param queryList list of query words
     * @return true if this is like a tuple query
     */
    private boolean isTupleQuery(List<Sentence> queryList) {
        if ( queryList != null && queryList.size() == 1 ) {
            Sentence query = queryList.get(0);
            if (query.size() > 1) {
                int numVerbs = 0;
                int numNouns = 0;
                int numWhats = 0;
                // does it have a verb and a noun/adjective?
                for (Token token : query.getTokenList()) {
                    if ( token.getPennType() != null && token.getPennType().toString().startsWith("VB") ) {
                        numVerbs += 1;
                    }
                    if ( token.getPennType() != null && (token.getPennType().toString().startsWith("NN") ||
                            token.getPennType().toString().startsWith("JJ")) ) {
                        numNouns += 1;
                    }
                    if ( token.getPennType() != null && token.getPennType().toString().startsWith("WP")) {
                        numWhats += 1;
                    }
                }
                return numVerbs == 1 && (numNouns > 0 || numWhats > 0);
            }
        }
        return false;
    }

    /**
     * perform a super sub-search on the shards
     * @param request the http request object for the search
     * @param sessionIDStr the session id
     * @param searchItem a super search query object with its shards set
     * @return a search result set
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("sub-search/{sessionID}")
    public Response search( @Context HttpServletRequest request,
                            @PathParam("sessionID") String sessionIDStr,
                            ISSearchItem searchItem ) {
        if ( !slSearchActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {

            if ( searchItem != null && sessionIDStr != null ) {
                User user = checkSession( userService, sessionIDStr, request );
                IndexList indexList = new IndexList(searchExecutor.doSearch(user.getOrganisation_id(), user.getEmail(), searchItem));
                return Response.status(200).entity(indexList).build();

            } else {
                logger.debug("PUT: search/sub-search: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch ( Exception ex ) {
            logger.error("PUT: search/sub-search: ",ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }

    /**
     * given a set of urls (url_list) and an entity name in search_text
     * @param request the http security object
     * @param sessionIDStr the session's id
     * @param searchObject the search object
     * @return a list of highlight sentences for viewing
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("view-entity/{sessionID}")
    public Response viewEntity( @Context HttpServletRequest request,
                                @PathParam("sessionID") String sessionIDStr,
                                SearchObject searchObject ) {
        if ( !slSearchActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {

            if ( searchObject != null && searchObject.getSearch_text() != null && sessionIDStr != null ) {
                User user = checkSession( userService, sessionIDStr, request );
                searchObject.setEmail(user.getEmail());

                logger.info("search/view-entity on \"" + searchObject.getSearch_text() + "\"");

                SearchResultList searchResultList = viewSearchEntities.getSearchEntities(user.getOrganisation_id(), searchObject);
                return Response.status(200).entity(searchResultList).build();

            } else {
                logger.debug("PUT: search/view-entity: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex) {
            logger.debug("PUT: search/view-entity: " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("PUT: search/view-entity: ",ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }



    /**
     * return the top authors for a given search topic
     * @param request the http security object
     * @param sessionIDStr the session's id
     * @param searchObject the search object
     * @param minPercentage minimum percentage to consider before filtering
     * @return a list of authors with percentages on the topic (softmax)
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("topic-authors/{sessionID}/{minPercentage}")
    public Response topicAuthors( @Context HttpServletRequest request,
                                  @PathParam("sessionID") String sessionIDStr,
                                  @PathParam("minPercentage") float minPercentage,
                                  SearchObject searchObject ) {
        if ( !slSearchActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {

            if ( searchObject != null && searchObject.getSearch_text() != null && sessionIDStr != null ) {
                User user = checkSession( userService, sessionIDStr, request );
                searchObject.setEmail(user.getEmail());

                logger.info("search/topic-authors on \"" + searchObject.getSearch_text() + "\"");

                AuthorFrequencySet authorFrequencySet = new AuthorFrequencySet();
                authorFrequencySet.setQuery(searchObject.getSearch_text());

                List<String> urlList = superSearch.getURLList(sessionIDStr, user.getEmail(),
                        user.getOrganisation_id(), searchObject, numDocumentsForSocial);
                if ( urlList != null && urlList.size() > 0 ) {
                    SentenceFromBinary sentenceFromBinary = new SentenceFromBinary();
                    Map<String, Float> authorFrequency = new HashMap<>();
                    Map<String, byte[]> authorSet = dao.getDocumentDao().getAuthorsForUrlList(user.getOrganisation_id(), urlList);
                    float numAuthorsSeen = 0.0f;
                    if ( authorSet != null ) {
                        for (String url : authorSet.keySet()) {
                            List<Sentence> sentenceList = sentenceFromBinary.convert(authorSet.get(url));
                            if ( sentenceList != null && sentenceList.size() == 1) {
                                Sentence sentence = sentenceList.get(0);
                                String author = sentence.toString();
                                numAuthorsSeen += 1.0f;
                                if (!authorFrequency.containsKey(author)) {
                                    authorFrequency.put(author, 1.0f);
                                } else {
                                    authorFrequency.put(author, authorFrequency.get(author) + 1.0f);
                                }
                            }
                        }
                    }
                    if ( numAuthorsSeen > 0.0f ) {
                        for (String author : authorFrequency.keySet()) {
                            float value = authorFrequency.get(author);
                            float perc = value / numAuthorsSeen;
                            if ( perc >= minPercentage ) {
                                authorFrequencySet.getAuthorList().add(new AuthorFrequency(author, perc));
                            }
                        }
                    }
                }

                return Response.status(200).entity(authorFrequencySet).build();

            } else {
                logger.debug("PUT: search/view-entity: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex) {
            logger.debug("PUT: search/view-entity: " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("PUT: search/view-entity: ",ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }



}

