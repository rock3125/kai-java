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

package industries.vocht.viki.loaddata;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import industries.vocht.viki.loaddata.comms.KAIClientInterface;
import industries.vocht.viki.model.knowledge_base.KBEntry;
import industries.vocht.viki.model.knowledge_base.KBEntryList;
import industries.vocht.viki.model.user.UserWithExtras;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

/**
 * Created by peter on 24/03/16.
 *
 * pre-load the system with geography
 *
 */
@Component
public class UploadGeography {

    private final static Logger logger = LoggerFactory.getLogger(UploadGeography.class);

    private final static String log4jPath = "/opt/kai/loaddata/conf/log4j2.xml";

    public static void main( String[] args ) throws Exception {

        // create Options object
        Options options = new Options();
        options.addOption("u", true, "the user's email to use for access (-u, default peter@peter.co.nz)");
        options.addOption("p", true, "the user's password (-p, default password)");
        options.addOption("h", false, "help, this message (-h)");
        options.addOption("host", true, "the server to communicate with (optional) (-host, default 'localhost')");
        options.addOption("port", true, "the server's port (optional) (-port, default 10080)");
        options.addOption("geography", true, "the geography input file (-geography)");

        CommandLineParser cmdParser = new DefaultParser();
        CommandLine line = null;
        try {
            // parse the command line arguments
            line = cmdParser.parse( options, args );
        } catch( ParseException exp ) {
            // oops, something went wrong
            System.err.println( "invalid command line: " + exp.getMessage() );
            System.exit(0);
        }

        // check compulsory items
        if ( line.hasOption("h")) {
            help(options);
            System.exit(1);
        }

        // get sentiment system to use
        String email = line.getOptionValue("u", "peter@peter.co.nz");
        String password = line.getOptionValue("p", "password");
        String host = line.getOptionValue("host", "localhost");
        String portStr = line.getOptionValue("port", "10080");
        int port = Integer.parseInt(portStr);

        String geographyFile = line.getOptionValue("geography");
        if ( geographyFile == null || !new File(geographyFile).exists() ) {
            help(options);
            System.exit(1);
        }

        LoggerContext l4j2context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
        l4j2context.setConfigLocation(new URI("file://" + log4jPath));
        ApplicationContext context = new ClassPathXmlApplicationContext("/application-context.xml");

        // get bean
        UploadGeography loader = context.getBean(UploadGeography.class);
        loader.setupInterface(host, port);

        loader.triggerUpload(email, password, geographyFile);

        System.exit(0);
    }

    /**
     * display help for the options
     * @param options the options
     */
    private static void help(Options options) {
        System.err.println(options.getOption("u").getDescription());
        System.err.println(options.getOption("p").getDescription());
        System.err.println(options.getOption("host").getDescription());
        System.err.println(options.getOption("port").getDescription());
        System.err.println(options.getOption("h").getDescription());
        System.err.println(options.getOption("geography").getDescription());
    }


    private KAIClientInterface kaiClientInterface;

    @Value("${geography.batch.size:10000}")
    private int batchSize;


    public UploadGeography() {
    }

    public void setupInterface( String host, int port ) {
        kaiClientInterface = new KAIClientInterface(host, port);
    }

    // upload class
    private class Geography {
        public String name;
        public String state;
        public String country;
        public String continent;
        public double x;
        public double y;
    }


    /**
     * upload geography files
     * @param email the email of the user to login with
     * @param password their password
     * @param geographyFile the tgz geography file
     * @throws IOException file error
     */
    public void triggerUpload(String email, String password, String geographyFile) throws IOException {
        // login the user
        logger.info("logging in " + email);
        UserWithExtras user = kaiClientInterface.login(email, password);
        UUID sessionID = user.getSessionID();
        if (sessionID != null) {
            logger.info("user successfully logged in");
        } else {
            throw new IOException("invalid session, not logged in");
        }

        ObjectMapper mapper = new ObjectMapper();
        BufferedReader in = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(geographyFile))));
        String content;

        List<KBEntry> geographyList = new ArrayList<>();

        int line = 0;
        while ((content = in.readLine()) != null) {
            String[] parts = content.split("\\|");
            if ( parts.length == 6 && line > 0 ) {

                KBEntry geography = new KBEntry();
                geography.setOrigin("init");
                geography.setType("geography");
                geography.setOrganisation_id(user.getOrganisation_id());

                Geography g = new Geography();
                g.name = parts[0].trim();
                g.state = parts[1].trim();
                g.country = parts[2].trim();
                g.continent = parts[3].trim();
                g.x = Double.parseDouble(parts[4].trim());
                g.y = Double.parseDouble(parts[5].trim());
                geography.setJson_data(mapper.writeValueAsString(g));

                geographyList.add( geography );
                if ( geographyList.size() >= batchSize ) {
                    uploadList(sessionID, geographyList);
                    geographyList.clear();
                }
            }
            line = line + 1;
            if ( line % 100_000 == 0 ) {
                logger.info("uploaded " + line + " geography items");
            }
        }

        if ( geographyList.size() > 0 ) {
            uploadList(sessionID, geographyList);
        }

        logger.info("geography upload done");
    }

    /**
     * upload a batch of geography items
     * @param geographyList the list of items to upload
     */
    private void uploadList(UUID sessionID, List<KBEntry> geographyList) throws IOException {
        try {
            kaiClientInterface.createEntityList(sessionID, geographyList);
        } catch (JsonParseException ex) {
            logger.error(ex.getMessage());
        }
    }



}

