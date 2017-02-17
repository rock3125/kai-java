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

import industries.vocht.viki.IHazelcast;
import industries.vocht.viki.dao.PennType;
import industries.vocht.viki.document_orchestrator.DocumentOrchestrator;
import industries.vocht.viki.hazelcast.DocumentAction;
import industries.vocht.viki.parser.NLParser;
import industries.vocht.viki.system_stats.DocumentWordCount;
import industries.vocht.viki.grammar.GrammarLibrary;
import industries.vocht.viki.parser.NLTimeResolver;
import industries.vocht.viki.jersey.HtmlWrapper;
import industries.vocht.viki.jersey.JsonMessage;
import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.user.User;
import industries.vocht.viki.utility.BinarySerializer;
import industries.vocht.viki.utility.SentenceFromBinary;
import industries.vocht.viki.wordcloud.DocumentWordCloudToImage;
import io.swagger.annotations.Api;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Created by peter on 17/10/15.
 *
 * service for managing documents
 *
 */
@Component
@Path("/viki/parser")
@Api(tags = "/viki/parser")
public class ParserServiceLayer extends ServiceLayerCommon {

    final Logger logger = LoggerFactory.getLogger(ParserServiceLayer.class);

    // message to the user if this service layer isn't active
    private static final String sl_inactive_message = "parser service layer not active on this node";
    private static final String sl_tn_inactive_message = "parser service layer/thumbnailing not active on this node";

    @Value("${sl.parser.activate:true}")
    private boolean slParserActive;

    @Value("${sl.thumbnail.activate:true}")
    private boolean slThumbnailActive;

    @Autowired
    private DocumentOrchestrator documentOrchestrator;

    @Value("${doc.image.min.word.length:4}")
    private int minWordLength;

    @Value("${doc.image.num.words:200}")
    private int numWords;

    @Value("${doc.image.padding:2}")
    private int padding;

    @Value("${doc.image.width:640}")
    private int width;

    @Value("${doc.image.height:480}")
    private int height;

    @Value("${doc.bg.image:data/wordcloud_backgrounds/kai_bg_640_480.png}")
    private String cloudBackgroundImage;

    @Autowired
    private NLParser parser;

    @Autowired
    private GrammarLibrary grammarLibrary;

    @Autowired
    private DocumentWordCount documentWordCount;

    private HashSet<String> metadataTagsNotToParse;


    public ParserServiceLayer() {
        metadataTagsNotToParse = new HashSet<>();
        metadataTagsNotToParse.add(Document.META_ACLS);
        metadataTagsNotToParse.add(Document.META_CLASSIFICATION);
        metadataTagsNotToParse.add(Document.META_URL);
        metadataTagsNotToParse.add(Document.META_ORIGIN);
    }

    /**
     * view the parsed text of a previously processed document
     *
     * @param request  the context of the request for ip purposes
     * @param url the file name of the document to be viewed
     * @return a byte[] stream of the object
     */
    @GET
    @Produces("text/html")
    @Path("view/{sessionID}/{url}")
    public Response parsed(@Context HttpServletRequest request,
                           @PathParam("sessionID") String sessionID,
                           @PathParam("url") String url) {
        if ( !slParserActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            // check session
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("document/parse invalid session (" + sessionID + ")");
                return Response.status(500).entity("invalid session").build();
            } else {
                logger.debug("document/parse (" + url + ")");
                Map<String, byte[]> documentMap = dao.getDocumentDao().getDocumentParseTreeMap(user.getOrganisation_id(), url);
                logger.info("text " + url);
                if (documentMap == null) {
                    return Response.status(404).entity(new HtmlWrapper()
                            .wrap("parsed map for " + url + " not found, perhaps not yet processed.  please try again later."))
                            .build();
                } else {
                    return Response.status(200).entity(new HtmlWrapper().wrap(mapToHtml(documentMap))).build();
                }
            }
        } catch (Exception ex) {
            logger.error("document/parse", ex);
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
    public Response parse(@Context HttpServletRequest request,
                          @PathParam("sessionID") String sessionID,
                          @PathParam("url") String url) {
        if ( !slParserActive ) {
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

                    // do we have a previous document that needs un-statting?
                    Map<String, byte[]> documentMap = dao.getDocumentDao().getDocumentParseTreeMap(user.getOrganisation_id(), url);
                    if ( documentMap != null && documentMap.containsKey(Document.META_BODY) ) {
                        byte[] dataBody = documentMap.get(Document.META_BODY);
                        SentenceFromBinary sentenceFromBinary = new SentenceFromBinary();
                        List<Sentence> sentenceList = sentenceFromBinary.convert(dataBody);
                        if ( sentenceList != null ) {
                            documentWordCount.removeDocument( user.getOrganisation_id(), url, sentenceList );
                        }
                    }

                    // parse the document
                    documentMap = parseDocument(user.getOrganisation_id(), url);
                    if (documentMap != null) {
                        // update time-stamps
                        document.setTs_parsed(System.currentTimeMillis());
                        // update document registry
                        dao.getDocumentDao().update(user.getOrganisation_id(), document);
                        // mark the k-means cluster as out-of-date at the current date
                        dao.getClusterDao().setClusterLastChange(user.getOrganisation_id(), System.currentTimeMillis() );

                        // add this document as an entity that can now be vectorized, indexed, summarized, analyzed emotionally
                        // and graphically thumbnailed
                        DocumentAction documentAction = new DocumentAction(user.getOrganisation_id(), url);
                        documentOrchestrator.offer(IHazelcast.QueueType.Vectorize, documentAction);
                        documentOrchestrator.offer(IHazelcast.QueueType.Index, documentAction);
                        documentOrchestrator.offer(IHazelcast.QueueType.Summarize, documentAction);
                        documentOrchestrator.offer(IHazelcast.QueueType.Emotion, documentAction);
                        documentOrchestrator.offer(IHazelcast.QueueType.Thumbnail, documentAction);

                        return Response.status(200).entity(new JsonMessage("ok", null)).build();

                    } else {
                        return Response.status(500).entity(new JsonMessage("document text not found")).build();
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
     * generate a graphical thumbnail for an image
     *
     * @param request  the context of the request for ip purposes
     * @param url the file name of the document to be thumbnailed
     * @return 200 ok when its done
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("thumbnail/{sessionID}/{url}")
    public Response thumbnail(@Context HttpServletRequest request,
                          @PathParam("sessionID") String sessionID,
                          @PathParam("url") String url) {
        if ( !slThumbnailActive ) {
            return Response.status(404).entity(new JsonMessage(sl_tn_inactive_message)).build();
        }
        try {
            // check session
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("parser/thumbnail invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else {

                Document document = dao.getDocumentDao().read(user.getOrganisation_id(), url);
                if ( document != null ) {

                    // do we have a document ?
                    Map<String, byte[]> documentMap = dao.getDocumentDao().getDocumentParseTreeMap(user.getOrganisation_id(), url);
                    if ( documentMap != null && documentMap.containsKey(Document.META_BODY) ) {
                        // generate an image for this document
                        DocumentWordCloudToImage documentWordCloudToImage = new DocumentWordCloudToImage();
                        byte[] docPng = documentWordCloudToImage.doc2FreqPng(documentMap, minWordLength, numWords,
                                padding, width, height, cloudBackgroundImage);
                        if ( docPng == null || docPng.length < 100 ) {
                            logger.debug("removing document image for " + document.getUrl());
                            dao.getDocumentDao().removeDocumentImage(user.getOrganisation_id(), document.getUrl());
                        } else {
                            logger.debug("saving document image for " + document.getUrl());
                            dao.getDocumentDao().saveDocumentImage(user.getOrganisation_id(), document.getUrl(), docPng);
                        }
                    }

                    return Response.status(200).entity(new JsonMessage("ok", null)).build();

                } else {
                    return Response.status(400).entity(new JsonMessage("document not found")).build();
                }
            }
        } catch (Exception ex) {
            logger.error("parser/thumbnail", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


    /**
     * parse a document from text to binary parse-tree
     * @param organisation_id the owner of the document
     * @param url the url / file id of the object
     * @return the parsed document binary on success, otherwise null
     */
    private Map<String, byte[]> parseDocument( UUID organisation_id, String url )
            throws IOException, InterruptedException {

        logger.debug("parser/start (" + organisation_id + "," + url + ")");

        Document document = dao.getDocumentDao().read(organisation_id, url);
        Map<String, String> documentMap = (document != null) ? document.getName_value_set() : null;
        if ( documentMap != null ) {

            // get a reference time from the meta-data
            DateTime referenceDateTime = null;
            if ( document.getCreated() != 0L ) {
                referenceDateTime = new DateTime(document.getCreated());
            }

            SentenceFromBinary sentenceFromBinary = new SentenceFromBinary();

            // 4. parse this document to a parse-tree
            logger.info("parsing text of " + url);
            Map<String, byte[]> map = new HashMap<>();
            StringBuilder parserPackage = new StringBuilder();
            for ( String key : documentMap.keySet() ) {

                String text = documentMap.get(key);

                // don't parse URLs, ACLs, Origin tags - or any other tags deemed not suitable for parsing
                if ( metadataTagsNotToParse.contains(key) ) {

                    List<Sentence> sentenceList = new ArrayList<>();
                    Sentence sentence = new Sentence();
                    Token token = new Token(text, PennType.NNP);
                    sentence.getTokenList().add(token);
                    sentenceList.add(sentence);

                    // convert it to a binary AFTER the tokens have had time applied above
                    byte[] data = sentenceFromBinary.convert(sentenceList);
                    map.put(key, data);

                } else {

                    parserPackage.append("<<<!").append(key).append("!>>>");
                    parserPackage.append(text);
                }

            } // for each metadata item

            Map<String, List<Sentence>> map_set = parser.parsePackage(parserPackage.toString());
            if ( map_set != null ) {
                for (String key : map_set.keySet()) {
                    List<Sentence> sentenceList = map_set.get(key);
                    if (key.equals(Document.META_BODY)) {
                        documentWordCount.addDocument(organisation_id, url, sentenceList);
                    }

                    List<Token> dateTimeTokenList = new ArrayList<>();
                    int sentence_counter = 0;
                    for (Sentence sentence : sentenceList) {
                        // set tuple's org-id and url
                        if ( sentence.getTuple() != null ) {
                            sentence.getTuple().setOrganisation_id(organisation_id);
                            sentence.getTuple().setUrl(url);
                            sentence.getTuple().setSentence_id(sentence_counter);
                        }
                        for (Token word : sentence.getTokenList()) {
                            // is this a date or time based token?  need special indexing
                            if (word.getGrammarRuleName() != null && (word.getGrammarRuleName().startsWith("time.") ||
                                    word.getGrammarRuleName().startsWith("date."))) {
                                dateTimeTokenList.add(word);
                            }
                        } // for each token
                        sentence_counter += 1;
                    } // for each sentence

                    // update any time tokens where possible
                    new NLTimeResolver().resolveTimeTokens(dateTimeTokenList, referenceDateTime,
                            grammarLibrary.getGrammarConversionMap());

                    // convert it to a binary AFTER the tokens have had time applied above
                    byte[] data = sentenceFromBinary.convert(sentenceList);
                    map.put(key, data);
                }
            }

            // save the parser document
            dao.getDocumentDao().saveDocumentParseTreeMap(organisation_id, url, map);

            return map;

        } else {
            return null;
        }
    }

    /**
     * pretty print a parse-tree hashmap to html
     * @param map the parse-tree hashmap to pretty print
     * @return an html string of the map
     */
    private String mapToHtml(Map<String, byte[]> map) throws IOException {
        if ( map != null && map.size() > 0 ) {
            StringBuilder sb = new StringBuilder();
            for (String key : map.keySet()) {
                sb.append("<h3>").append(key).append("<h3><p>");
                byte[] data = map.get(key);
                BinarySerializer blob = new BinarySerializer(data);
                int numEntries = blob.readInt();
                for (int i = 0; i < numEntries; i++) {
                    Sentence sentence = new Sentence();
                    sentence.read(blob);
                    sb.append(sentence.toString()).append("<br/>");
                }
                sb.append("</p><br/><br/>");
            }
            return sb.toString();
        }
        return "<h2>map empty or null</h2>";
    }



}
