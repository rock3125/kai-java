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

package industries.vocht.viki.document_orchestrator;

import com.google.common.util.concurrent.RateLimiter;
import industries.vocht.viki.ApplicationException;
import industries.vocht.viki.IDao;
import industries.vocht.viki.IHazelcast;
import industries.vocht.viki.client.*;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.hazelcast.DocumentAction;
import industries.vocht.viki.hazelcast.Hazelcast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.Socket;

/**
 * Created by peter on 2/04/16.
 *
 * the document processor system
 *
 */
@Component
public class DocumentProcessor implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(DocumentProcessor.class);

    // this flag waits for the system to be ready - set when the web port is open
    private boolean systemReady = false;

    @Value("${web.port}")
    private int port;

    ///////////////////////////////////////////////////////////////////////////////////

    @Value("${sl.converter.activate:true}")
    private boolean slConverterActive;

    @Value("${sl.summarisation.activate}")
    private boolean slSummarisationActive;

    @Value("${sl.analysis.activate}")
    private boolean slAnalysisActive;

    @Value("${sl.parser.activate}")
    private boolean slParserActive;

    @Value("${sl.index.activate}")
    private boolean slIndexActive;

    @Value("${sl.vectorize.activate}")
    private boolean slVectorizeActive;

    @Value("${sl.thumbnail.activate}")
    private boolean slThumbnailActive;

    @Autowired
    private DocumentOrchestrator documentOrchestrator;

    @Autowired
    private IDao dao;

    // if the thread is active - keep looping
    private boolean active = true;

    // setup a rate limiter
    private RateLimiter sharedRateLimiter;

    public DocumentProcessor() {
    }

    public void init() {
        sharedRateLimiter = documentOrchestrator.getSharedRateLimiter();
    }

    /**
     * stop the thread
     */
    public void stop() {
        this.active = false;
    }


    @Override
    public void run() {

        while (slConverterActive || slParserActive || slVectorizeActive ||
                slSummarisationActive || slIndexActive || slAnalysisActive || slThumbnailActive) {

            // wait for the system to come up properly if it hasn't already (init)
            if (!systemReady) {
                waitForWeb();
            }

            try {

                int numOps = 0; // done nothing yet

                // grab the next document for conversion
                if (slConverterActive) {
                    DocumentAction document = documentOrchestrator.getNextMessageFromQueue(Hazelcast.QueueType.Convert);
                    if (document != null) {
                        sharedRateLimiter.acquire();
                        String sessionID = documentOrchestrator.getSystemUserSessionForOrganisation(document.getOrganisation_id());
                        Document doc = dao.getDocumentDao().read(document.getOrganisation_id(), document.getUrl());
                        if (doc != null) {
                            ConverterClientInterface client = new ConverterClientInterface("localhost", port);
                            client.convert(sessionID, doc.getUrl());
                            numOps += 1;
                        }
                    }
                }

                // grab the next document for parsing
                if (slParserActive) {
                    DocumentAction document = documentOrchestrator.getNextMessageFromQueue(Hazelcast.QueueType.Parse);
                    if (document != null) {
                        sharedRateLimiter.acquire();
                        String sessionID = documentOrchestrator.getSystemUserSessionForOrganisation(document.getOrganisation_id());
                        Document doc = dao.getDocumentDao().read(document.getOrganisation_id(), document.getUrl());
                        if (doc != null) {
                            ParserClientInterface client = new ParserClientInterface("localhost", port);
                            client.parse(sessionID, doc.getUrl());
                            numOps += 1;
                        }
                    }
                }


                // grab the next document for vectorization
                if (slVectorizeActive) {
                    DocumentAction document = documentOrchestrator.getNextMessageFromQueue(Hazelcast.QueueType.Vectorize);
                    if (document != null) {
                        sharedRateLimiter.acquire();
                        String sessionID = documentOrchestrator.getSystemUserSessionForOrganisation(document.getOrganisation_id());
                        Document doc = dao.getDocumentDao().read(document.getOrganisation_id(), document.getUrl());
                        if (doc != null) {
                            VectorizeClientInterface client = new VectorizeClientInterface("localhost", port);
                            client.vectorize(sessionID, doc.getUrl());
                            numOps += 1;
                        }
                    }
                }


                // grab the next document for text/word summarization
                if (slSummarisationActive) {
                    DocumentAction document = documentOrchestrator.getNextMessageFromQueue(Hazelcast.QueueType.Summarize);
                    if (document != null) {
                        sharedRateLimiter.acquire();
                        String sessionID = documentOrchestrator.getSystemUserSessionForOrganisation(document.getOrganisation_id());
                        Document doc = dao.getDocumentDao().read(document.getOrganisation_id(), document.getUrl());
                        if (doc != null) {
                            SummariseClientInterface client = new SummariseClientInterface("localhost", port);
                            client.summarise(sessionID, doc.getUrl());
                            numOps += 1;
                        }
                    }
                }


                // grab the next document for indexing
                if (slIndexActive) {
                    DocumentAction document = documentOrchestrator.getNextMessageFromQueue(Hazelcast.QueueType.Index);
                    if (document != null) {
                        sharedRateLimiter.acquire();
                        String sessionID = documentOrchestrator.getSystemUserSessionForOrganisation(document.getOrganisation_id());
                        Document doc = dao.getDocumentDao().read(document.getOrganisation_id(), document.getUrl());
                        if (doc != null) {
                            IndexClientInterface client = new IndexClientInterface("localhost", port);
                            client.index(sessionID, doc.getUrl());
                            numOps += 1;
                        }
                    }
                }


                // grab the next document for emotional analysis
                if (slAnalysisActive) {
                    DocumentAction document = documentOrchestrator.getNextMessageFromQueue(Hazelcast.QueueType.Emotion);
                    if (document != null) {
                        sharedRateLimiter.acquire();
                        String sessionID = documentOrchestrator.getSystemUserSessionForOrganisation(document.getOrganisation_id());
                        Document doc = dao.getDocumentDao().read(document.getOrganisation_id(), document.getUrl());
                        if (doc != null) {
                            EmotionAnalyseClientInterface client = new EmotionAnalyseClientInterface("localhost", port);
                            client.analyse(sessionID, doc.getUrl());
                            numOps += 1;
                        }
                    }
                }


                // grab the next document for thumbnail generation - if we've got nothing else to do
                if (slThumbnailActive && numOps == 0) {
                    DocumentAction document = documentOrchestrator.getNextMessageFromQueue(Hazelcast.QueueType.Thumbnail);
                    if (document != null) {
                        sharedRateLimiter.acquire();
                        String sessionID = documentOrchestrator.getSystemUserSessionForOrganisation(document.getOrganisation_id());
                        Document doc = dao.getDocumentDao().read(document.getOrganisation_id(), document.getUrl());
                        if (doc != null) {
                            ParserClientInterface client = new ParserClientInterface("localhost", port);
                            client.generateThumbnail(sessionID, doc.getUrl());
                            numOps += 1;
                        }
                    }
                }


                // idle?
                if (numOps == 0) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        logger.debug("interrupted, exit: " + ex.getMessage());
                        break;
                    }
                }

            } catch (ApplicationException | IOException ex) {
                logger.error("document processor:" + ex.getMessage());
            }

        } // while active
    }


    /**
     * wait for the web server system to open its ports
     */
    private void waitForWeb() {
        logger.info("DocumentProcessor waiting for system to come up");
        boolean open = false;
        while (!open) {
            try {
                Socket socket = new Socket("localhost", port);
                open = true;
                socket.close();
            } catch (IOException ex) {
                // not yet
                try {
                    Thread.sleep(1000); // wait a second!
                } catch (InterruptedException ex2) {
                    logger.debug("waitForWeb" + ex2.getMessage());
                }
            }
        }
        systemReady = true;
        logger.info("DocumentProcessor local web-server active");
    }



}

