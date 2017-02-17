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

import com.fasterxml.jackson.databind.ObjectMapper;
import industries.vocht.viki.loaddata.comms.KAIClientInterface;
import industries.vocht.viki.model.Organisation;
import industries.vocht.viki.model.group.Group;
import industries.vocht.viki.model.knowledge_base.KBEntry;
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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 24/03/16.
 *
 * pre-load the system with available data
 *
 */
@Component
public class DataLoader {

    private final static Logger logger = LoggerFactory.getLogger(DataLoader.class);

    private final static String log4jPath = "/opt/kai/loaddata/conf/log4j2.xml";

    @Value("${lexicon.names.folder:/opt/kai/data/lexicon/names}")
    private String lexiconNamesFolder;


    public static void main( String[] args ) throws Exception {

        // create Options object
        Options options = new Options();
        options.addOption("u", true, "the user's email to use for access (-u, default peter@peter.co.nz)");
        options.addOption("p", true, "the user's password (-p, default password)");
        options.addOption("o", true, "the organisation to create (optional) (-o, default 'Rock Corp')");
        options.addOption("host", true, "the server to communicate with (optional) (-host, default 'localhost')");
        options.addOption("port", true, "the server's port (optional) (-port, default 10080)");
        options.addOption("h", false, "help, this message (-h)");
        options.addOption("fn", true, "the user's first name (optional) (-fn, default 'Rock')");
        options.addOption("sn", true, "the user's surname (optional) (-sn, default 'de Vocht')");

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
        if ( line.hasOption("h") ) {
            help(options);
            System.exit(1);
        }

        // get sentiment system to use
        String email = line.getOptionValue("u", "peter@peter.co.nz");
        String password = line.getOptionValue("p", "password");
        String organisationName = line.getOptionValue("o", "Rock Corp");
        String firstName = line.getOptionValue("fn", "Rock");
        String surname = line.getOptionValue("sn", "de Vocht");
        String host = line.getOptionValue("host", "localhost");
        String portStr = line.getOptionValue("port", "10080");
        int port = Integer.parseInt(portStr);

        // setup logging
        LoggerContext l4j2context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
        l4j2context.setConfigLocation(new URI("file://" + log4jPath));

        // get loader
        ApplicationContext context = new ClassPathXmlApplicationContext("/application-context.xml");
        DataLoader loader = context.getBean(DataLoader.class);
        loader.setupInterface(host, port);

        // check/setup objects
        loader.setupObjects(organisationName, firstName, surname, email, password);

        loader.loadNames();

        System.exit(0);
    }

    /**
     * display help for the options
     * @param options the options
     */
    private static void help(Options options) {
        System.err.println(options.getOption("u").getDescription());
        System.err.println(options.getOption("p").getDescription());
        System.err.println(options.getOption("fn").getDescription());
        System.err.println(options.getOption("sn").getDescription());
        System.err.println(options.getOption("h").getDescription());
        System.err.println(options.getOption("host").getDescription());
        System.err.println(options.getOption("port").getDescription());
    }

    // the organisation to create along with the initial user
    private UUID sessionID;
    private UserWithExtras user;
    private KAIClientInterface kaiClientInterface;

    public DataLoader() {
    }

    public void setupInterface( String host, int port ) {
        kaiClientInterface = new KAIClientInterface(host, port);
    }

    /**
     * setup the required accounts (or checking existing) for the data-loader
     * @param organisationName the name of the organisation
     * @param firstName the first name of the main user
     * @param surname the surname of the main user
     * @param email the email of the main user
     * @param password the password of the main user
     * @throws IOException error
     */
    private void setupObjects(String organisationName, String firstName, String surname, String email, String password)
            throws IOException, URISyntaxException {

        // do we have the organisation called for?
        logger.info("getting organisation by name " + organisationName);
        Organisation organisation = kaiClientInterface.getOrganisationByName(organisationName);

        // create a new one
        if ( organisation == null ) {
            logger.info("organisation " + organisationName + " does not exist, creating");

            user = new UserWithExtras();
            user.setEmail(email);
            user.setFirst_name(firstName);
            user.setSurname(surname);
            user.setPassword(password);

            // create the initial organisation
            organisation = kaiClientInterface.createUserOrganisation(organisationName, user);

            // cheat - get the activation id from the db
            logger.info("getting activation id for " + email);
            UUID activationID = kaiClientInterface.getUserActivationID(email);
            if ( activationID == null ) {
                throw new IOException("can't get user's activation id");
            }
            logger.info("activating user " + email);
            kaiClientInterface.activateUser(email, activationID);
        }

        // login the user
        logger.info("logging in " + email);
        user = kaiClientInterface.login(email, password);
        sessionID = user.getSessionID();
        if ( sessionID != null ) {

            logger.info("user successfully logged in");
            logger.info("adding groups");
            addGroup( organisation.getId(), "users", user.getEmail());
            addGroup( organisation.getId(), "administrators", user.getEmail());
            addGroup( organisation.getId(), "low-security", user.getEmail());
            addGroup( organisation.getId(), "medium-security", user.getEmail());
            addGroup( organisation.getId(), "top-secret", user.getEmail());

        } else {
            throw new IOException("invalid session, not logged in");
        }
    }

    /**
     * add a new group to the system
     * @param organisation_id the organisation
     * @param groupName the name of the group
     * @param email the "content" / users of this group
     * @throws IOException error
     */
    private void addGroup(UUID organisation_id, String groupName, String email) throws IOException {
        // add first groups
        Group group1 = new Group();
        group1.setName(groupName);
        group1.setOrganisation_id(organisation_id);
        group1.getUser_list().add(email);
        try {
            kaiClientInterface.createGroup(sessionID, group1);
        } catch ( IOException ex ) {
            logger.warn("group:" + ex.getMessage());
        }
    }

    private class NameEntity {
        public String name;
        public String isa;
        public List<String> alias_list;
    }

    /**
     * load all the names of males, females, and surnames
     */
    private void loadNames() throws IOException {
        logger.info("loading entities from " + lexiconNamesFolder);
        int counter = 0;
        ObjectMapper mapper = new ObjectMapper();
        File[] list = new File(lexiconNamesFolder).listFiles();
        if ( list != null ) {
            for ( File file :  list ) {
                if (file.isFile()) {
                    List<KBEntry> entityList = new ArrayList<>();
                    logger.info("processing " + file.getAbsolutePath());
                    List<String> nameList = Files.readAllLines(Paths.get(file.getAbsolutePath()));
                    for (String str : nameList) {
                        // skip empty lines and comments
                        if ( str.trim().length() == 0 || str.startsWith("#") || str.startsWith("//") ) {
                            continue;
                        }
                        String[] nameSet = str.trim().split(",");
                        if (nameSet.length >= 3) {

                            KBEntry entity = new KBEntry();
                            entity.setOrganisation_id(user.getOrganisation_id());
                            entity.setId(UUID.fromString(nameSet[0]));
                            entity.setOrigin("init");
                            entity.setType("entity");

                            NameEntity entity1 = new NameEntity();
                            entity1.name =  nameSet[1].trim();
                            entity1.isa =  nameSet[2].trim();
                            if ( nameSet.length > 3 ) {
                                List<String> aliasList = new ArrayList<>();
                                for ( int i = 3; i < nameSet.length; i++ ) {
                                    aliasList.add(nameSet[i].trim());
                                }
                                entity1.alias_list = aliasList;
                            }
                            entity.setJson_data(mapper.writeValueAsString(entity1));

                            entityList.add( entity );
                            counter = counter+ 1;
                        } // if valid entity

                    } // for each entity

                    logger.info("uploading " + file.getAbsolutePath() + ", " + entityList.size() + " items");
                    kaiClientInterface.createEntityList(sessionID, entityList);

                } // if is file
            }

            logger.info("uploaded " + counter + " entities");

        } // if has list of file names
    }


}

