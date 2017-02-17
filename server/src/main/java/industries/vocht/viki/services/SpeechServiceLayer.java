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

import industries.vocht.viki.jersey.JsonMessage;
import industries.vocht.viki.mary_tts.MaryHttpClient;
import industries.vocht.viki.speech2text.STTResult;
import industries.vocht.viki.speech2text.SphinxSpeechToText;
import io.swagger.annotations.Api;
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
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


/**
 * Created by peter on 23/07/16.
 *
 * speech to text service layer
 *
 */
@Component
@Path("/viki/speech")
@Api(tags = "/viki/speech")
public class SpeechServiceLayer {

    private final static Logger logger = LoggerFactory.getLogger(SpeechServiceLayer.class);

    // message to the user if this service layer isn't active
    private static final String sl_inactive_message = "speech service layer not active on this node";

    @Value("${sl.speech.activate:true}")
    private boolean slSpeechActive;

    @Autowired
    private UserService userService;

    @Autowired
    private SphinxSpeechToText sphinxSpeechToText;

    //////////////////////////////////////////////////////////////////
    // text to speech server

    @Value("${speech.tts.server.port:9010}")
    private int ttsServerPort;

    @Value("${speech.tts.server.csv.list:localhost}")
    private String TTSServerList;
    private List<String> ttsClientList;
    private int ttsAddressRoundRobin;

    @Value("${speech.tts.server.voice:alice}") // charles or alice
    private String ttsVoice;


    public SpeechServiceLayer() {
    }

    // setup
    public void init() throws IOException {
        ttsAddressRoundRobin = 0;
        ttsClientList = new ArrayList<>();
        ttsClientList.addAll(Arrays.asList(TTSServerList.split(",")));
    }

    /**
     * convert speech to text - upload a wav and get back some text if all goes well
     * @param request the http request
     * @param sessionID the session to use
     * @param payload the payload / byte[] of the wav data
     * @return a JSON object if possible
     */
    @POST
    @Path("to-text/{sessionID}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response speechToText( @Context HttpServletRequest request,
                                  @FormDataParam("wave") final InputStream payload,
                                  @PathParam("sessionID") String sessionID ) {
        if ( !slSpeechActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( sessionID != null && payload != null ) {
                userService.getUser(UUID.fromString(sessionID), request.getRemoteAddr());
                logger.debug("speech/to-text " + sessionID);

                STTResult response = sphinxSpeechToText.wavToText(payload);
                if ( response != null && response.getText() != null ) {
                    return Response.status(200).entity(response).build();
                }

                logger.debug("speech/to-text invalid conversion");
                return Response.status(500).entity(new JsonMessage("invalid conversion")).build();

            } else {
                logger.debug("speech/to-text invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (Exception ex) {
            logger.error("speech/to-text", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }

    /**
     * convert text to speech using MaryTTS - invoke the server
     * (see parameters) and return a stream wave
     * @param request the http request
     * @param sessionID the security session id
     * @param text the text to "speak"
     * @return a wav stream
     */
    @GET
    @Path("to-speech/{sessionID}/{text}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("audio/x-wav")
    public Response textToSpeech( @Context HttpServletRequest request,
                                  @PathParam("sessionID") String sessionID,
                                  @PathParam("text") String text ) {
        if ( !slSpeechActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( sessionID != null && text != null && text.length() > 0 ) {
                logger.debug("to-speech " + sessionID);
                userService.getUser(UUID.fromString(sessionID), request.getRemoteAddr());

                // see if we have a valid Mary client
                if ( ttsClientList == null || ttsClientList.size() == 0 ) {
                    logger.info("speech service layer not active (no TTS servers setup)");
                    return Response.status(500).entity(new JsonMessage("TTS speech server(s) not enabled")).build();
                }

                logger.debug("speech/to-speech " + sessionID);

                String server = ttsClientList.get(ttsAddressRoundRobin);
                ttsAddressRoundRobin = (ttsAddressRoundRobin + 1) % ttsClientList.size();

                byte[] data = tts(text, ttsVoice, server, ttsServerPort);
                if ( data == null ) {
                    logger.error("tts: null data returned");
                    return Response.status(404).entity("null data returned").build();
                } else {
                    return Response.status(200).entity(data).build();
                }

            } else {
                logger.debug("speech/to-speech invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (Exception ex) {
            logger.error("speech/to-speech", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }

    /**
     * make a request to the speech server and return the wav data
     * @param text the text to convert to speech
     * @param ttsVoice the voice to use, right now either alice or charles
     * @param server the server to talk to
     * @param port the port of that server to talk to
     * @return a byte[] with wav data
     */
    private static byte[] tts(String text, String ttsVoice, String server, int port) throws IOException {
        // use Peter's new simple Mary server
        MaryHttpClient maryClient = new MaryHttpClient(server, port);
        return maryClient.tts(ttsVoice, text);
    }


}


