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
import industries.vocht.viki.hazelcast_messages.HMsgNNetResetCaches;
import industries.vocht.viki.jersey.JsonMessage;
import industries.vocht.viki.lexicon.AmbiguousLexicon;
import industries.vocht.viki.lexicon.LexiconSynset;
import industries.vocht.viki.messaging.HazelcastSystemMessageProcessor;
import industries.vocht.viki.model.nnet.NNetModelData;
import industries.vocht.viki.model.user.User;
import io.swagger.annotations.Api;
import org.apache.commons.compress.utils.IOUtils;
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
import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Component
@Path("/viki/nnet")
@Api(tags = "/viki/nnet")
public class NNetServiceLayer {

    final Logger logger = LoggerFactory.getLogger(NNetServiceLayer.class);

    // message to the user if this service layer isn't active
    private static final String sl_inactive_message = "nnet service layer not active on this node";

    @Value("${sl.nnet.activate:true}")
    private boolean slNNetActive;

    @Autowired
    private IDao dao;

    @Autowired
    private AmbiguousLexicon ambiguousLexicon;

    @Autowired
    private HazelcastSystemMessageProcessor messageProcessor;

    @Value("${nnet.file.directory.for.upload:/opt/kai/data/nnet}")
    private String nnetFileDirectory;

    @Value("${nnet.training.iterations:75}")
    private int iterations;

    @Value("${nnet.training.set.split:0.8}")
    private double trainingSetSizePc;


    public NNetServiceLayer() {
    }

    /**
     * start training a new neural network for the specified word
     * @param request the http request
     * @param sessionID the session of the trainer requester
     * @param word the word to be trained for
     * @return s'ok or not
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("train/{sessionID}/{word}")
    public Response train(@Context HttpServletRequest request,
                            @PathParam("sessionID") String sessionID,
                            @PathParam("word") String word ) {
        if ( !slNNetActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            // check session
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("nnet/train invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid parameters")).build();
            } else {

                // start a new thread for this word
                List<LexiconSynset> synsetList = ambiguousLexicon.getSynset(word);
                if ( synsetList == null || synsetList.size() < 2 ) {
                    return Response.status(404).entity(new JsonMessage("noun \"" + word + "\" is not an ambiguous word")).build();
                } else {

//                    // start training in a separate thread
//                    NNetTrain trainer = new NNetTrain( user.getOrganisation_id(), dao, word, synsetList.size(),
//                                                       iterations, trainingSetSizePc, messageProcessor );
//
//                    Thread thread = new Thread(trainer);
//                    thread.setName("nnet trainer for " + word);
//                    thread.start();

                    // report back it was done!
                    return Response.status(200).entity(new JsonMessage("ok", null)).build();
                }

            }
        } catch (Exception ex) {
            logger.error("nnet/train", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }

    }


    /**
     * upload all the available neural networks to the database store from file
     * but only if they don't already exist!!!
     * @param request the http request
     * @param sessionID the session of the trainer requester
     * @return s'ok or not
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("upload/{sessionID}")
    public Response upload_nn( @Context HttpServletRequest request,
                               @PathParam("sessionID") String sessionID ) {
        if ( !slNNetActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            // check session
            User user = dao.getUserDao().getUserForSession(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("nnet/upload invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid parameters")).build();
            } else {

                logger.info("neural network upload request starting for \"" + user.getEmail() + "\"");

                // go through each possible item in the ambiguous lexicon and check
                // the lexicon file exists
                HashSet<String> focusList = ambiguousLexicon.getWordFocusSet();
                for ( String word : focusList ) {
                    // musts be the singular form of the noun
                    if ( word.equals( ambiguousLexicon.getSingular(word) ) ) {

                        File jsonFile = new File(nnetFileDirectory + "/" + word + "-conf.json");
                        File binFile = new File(nnetFileDirectory + "/" + word + "-coefficients.bin");

                        // does it exist?
                        if ( jsonFile.exists() && binFile.exists() ) {
                            // do we already have a neural network for it uploaded?
                            // if so - skip
                            NNetModelData model = dao.getNNetDao().loadModel(user.getOrganisation_id(), word);
                            if ( model == null ) {
                                // does not exist - upload it!
                                logger.info("uploading neural net for: \"" + word + "\"");
                                String jsonStr = new String(IOUtils.toByteArray(new FileInputStream(jsonFile.getAbsoluteFile())));
                                byte[] binaryData = IOUtils.toByteArray(new FileInputStream(binFile.getAbsoluteFile()));
                                dao.getNNetDao().saveModel( user.getOrganisation_id(), word, new NNetModelData(jsonStr, binaryData, System.currentTimeMillis()) );
                            } else {
                                logger.debug("upload neural nets: \"" + word + "\" already has an existing neural network, skipping");
                            }

                        } else {
                            logger.debug("no file neural net found for: \"" + word + "\"");
                        }

                    } // if is singular

                } // for each neural network "word"

                // tell the connected interested nodes that there are some new networks available
                messageProcessor.publish( new HMsgNNetResetCaches() );

                // report back it was done!
                return Response.status(200).entity(new JsonMessage("ok", null)).build();

            }
        } catch (Exception ex) {
            logger.error("nnet/upload", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }

    }


}

