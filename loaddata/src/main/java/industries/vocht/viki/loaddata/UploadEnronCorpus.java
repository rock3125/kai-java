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

import industries.vocht.viki.loaddata.comms.KAIClientInterface;
import industries.vocht.viki.model.Acl;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.model.user.UserWithExtras;
import org.apache.commons.cli.*;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.joda.time.DateTime;
import org.joda.time.IllegalInstantException;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

/**
 * Created by peter on 11/04/16.
 *
 * process the enron corpus and upload all the files
 *
 */
@Component
public class UploadEnronCorpus {

    private final static Logger logger = LoggerFactory.getLogger(UploadEnronCorpus.class);

    private final static String log4jPath = "/opt/kai/loaddata/conf/log4j2.xml";

    private final static int SLEEP_TIME_IN_MS = 25_000;

    public static void main( String[] args ) throws Exception {

        // create Options object
        Options options = new Options();
        options.addOption("u", true, "the user's email to use for access (-u, default peter@peter.co.nz)");
        options.addOption("p", true, "the user's password (-p, default password)");
        options.addOption("h", false, "help, this message (-h)");
        options.addOption("enron", true, "the path to the enron tgz file (-enron)");
        options.addOption("limit", true, "the number of items to process before stopping (-limit, default 1000)");
        options.addOption("skip", true, "the number of items to skip before starting (-skip, default 0)");
        options.addOption("host", true, "the server to communicate with (optional) (-host, default 'localhost')");
        options.addOption("port", true, "the server's port (optional) (-port, default 10080)");

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
        if ( line.getOptionValue("enron") == null || line.hasOption("h")) {
            help(options);
            System.exit(1);
        }

        // get sentiment system to use
        String email = line.getOptionValue("u", "peter@peter.co.nz");
        String password = line.getOptionValue("p", "password");
        String enron = line.getOptionValue("enron");
        int limit = Integer.parseInt(line.getOptionValue("limit", "1000"));
        int skip = Integer.parseInt(line.getOptionValue("skip", "0"));
        String host = line.getOptionValue("host", "localhost");
        String portStr = line.getOptionValue("port", "10080");
        int port = Integer.parseInt(portStr);

        // setup logger
        LoggerContext l4j2context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
        l4j2context.setConfigLocation(new URI("file://" + log4jPath));

        // get uploader
        ApplicationContext context = new ClassPathXmlApplicationContext("/application-context.xml");
        UploadEnronCorpus loader = context.getBean(UploadEnronCorpus.class);
        loader.setupInterface(host, port);

        // setup the connections and the file
        loader.setEnronFilename(enron);

        // login the user so we can proceed
        loader.login(email, password);

        // do the work
        loader.uploadEnron(skip, limit);

        System.exit(0);
    }

    /**
     * display help for the options
     * @param options the options
     */
    private static void help(Options options) {
        System.err.println(options.getOption("u").getDescription());
        System.err.println(options.getOption("p").getDescription());
        System.err.println(options.getOption("enron").getDescription());
        System.err.println(options.getOption("limit").getDescription());
        System.err.println(options.getOption("host").getDescription());
        System.err.println(options.getOption("port").getDescription());
        System.err.println(options.getOption("h").getDescription());
    }


    //////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private String enronFilename;
    private String sessionID;
    private UUID organisation_id;
    private KAIClientInterface kaiClientInterface;

    public UploadEnronCorpus() {
    }

    public void setupInterface( String host, int port ) {
        kaiClientInterface = new KAIClientInterface(host, port);
    }

    public void login(String username, String password) throws IOException {
        UserWithExtras user = kaiClientInterface.login(username, password);
        this.sessionID = user.getSessionID().toString();
        this.organisation_id = user.getOrganisation_id();
    }

    public void setEnronFilename(String enronFilename) {
        this.enronFilename = enronFilename;
    }

    public void uploadEnron( int skip, int limit ) throws IOException {
        TarArchiveInputStream tarInput =
                new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(enronFilename)));

        int numMessages = 0;
        int numFailed = 0;
        int index = 0;
        int limitCounter = limit;
        TarArchiveEntry entry;
        while ( (entry = tarInput.getNextTarEntry()) != null && limitCounter > 0 ) {

            //if the entry in the tar is a directory, it needs to be created, only files can be extracted
            if ( entry.isFile() ) {
                String fileName = entry.getName();
                if ( fileName.startsWith("maildir/") ) {
                    fileName = fileName.substring(8);
                }
                if ( !fileName.endsWith(".") ) {
                    fileName = fileName + ".";
                }
                String url = "enron://" + fileName + "txt";

                byte[] content = new byte[(int) entry.getSize()];
                int offset = 0;
                int size;
                while ( (size = tarInput.read(content, offset, content.length - offset)) > 0 ) {
                    offset = offset + size;
                }
                if ( offset == content.length ) {

                    //  if index is past the skip point
                    if ( index >= skip ) {
                        String message = new String(content);
                        String[] messageList = message.split("\n");

                        Document document = new Document();
                        document.setProcessingPipeline(-1L); // process all
                        document.setUrl(url);
                        document.setOrigin("enron");
                        document.getAcl_set().add(new Acl("users", true)); // all users have access
                        document.setOrganisation_id(organisation_id);

                        String from = getXFrom(messageList);
                        if (from == null) {
                            from = getFrom(messageList);
                        }
                        if (from != null) {
                            document.setAuthor(from);
                            document.getName_value_set().put(Document.META_AUTHOR, from);
                        }
                        String to = getTo(messageList);
                        if (to != null && to.length() > 0) {
                            document.getName_value_set().put("{recipients-to}", to);
                        }
                        String toCc = getToCc(messageList);
                        if (toCc != null && toCc.length() > 0) {
                            document.getName_value_set().put("{recipients-cc}", toCc);
                        }
                        String toBcc = getTo(messageList);
                        if (toBcc != null && toBcc.length() > 0) {
                            document.getName_value_set().put("{recipients-bcc}", toBcc);
                        }
                        String subject = getSubject(messageList);
                        if (subject != null) {
                            document.setTitle(subject);
                            document.getName_value_set().put(Document.META_TITLE, subject);
                        }
                        DateTime createdDateTime = getCreatedDateTime(messageList);
                        if (createdDateTime != null) {
                            document.setCreated(createdDateTime.toDate().getTime());
                            String dateTimeStr = createdDateTime.toString("yyyy-MM-dd hh:mm:ss");
                            document.getName_value_set().put(Document.META_CREATED_DATE_TIME, dateTimeStr);
                        }

                        String messageBody = getMessageBody(messageList);
                        if (messageBody != null) {

                            // upload with re-try
                            boolean error = false;
                            do {
                                try {

                                    numMessages = upload(url, document, messageBody.getBytes(), numMessages);
                                    error = false;

                                    // decrease limit
                                    limitCounter = limitCounter - 1;
                                    if (limitCounter <= 0) {
                                        logger.info("limit of " + limit + " reached, exiting");
                                    }

                                } catch (IOException ex) {

                                    // try again later
                                    if (ex.getMessage().contains("queue full")) {
                                        logger.info("queue full, sleeping " + (SLEEP_TIME_IN_MS / 1000) + " seconds (@ " + numMessages + ")");
                                        error = true;
                                        try {
                                            Thread.sleep(SLEEP_TIME_IN_MS);
                                        } catch (InterruptedException ex2) {
                                        }
                                    } else {
                                        logger.error(ex.getMessage());
                                        numFailed = numFailed + 1;
                                    }
                                }

                            } while (error);

                        }

                    } // if skipping

                    index = index + 1;


                } // if length is right

            } // if entry is a file

        } // for each entry

        tarInput.close();

        logger.info("number of emails uploaded: " + numMessages);
        logger.info("number of emails failed upload: " + numFailed);
    }

    /**
     * upload a document interface
     * @param url the url of the document
     * @param document the document data-structure
     * @param content the content of the message
     * @param numMessages the message counter
     * @return updateed message counter
     * @throws IOException
     */
    private int upload( String url, Document document, byte[] content, int numMessages ) throws IOException {
        // upload the document to the server
        kaiClientInterface.metadata(sessionID, "enron", url, document);

        // upload document content and start processing
        kaiClientInterface.upload(sessionID, url, content);

        numMessages = numMessages + 1;
        if ( numMessages % 1000 == 0 ) {
            logger.info("processed " + numMessages);
        }
        return numMessages;
    }

    // get from field from an email
    public String getSubject( String[] messageList ) {
        for ( String str : messageList ) {
            if ( str.startsWith("Subject: ") ) {
                return str.substring(9).trim();
            }
        }
        return null;
    }

    // get from field from an email
    public DateTime getCreatedDateTime( String[] messageList ) {
        for ( String str : messageList ) {
            if ( str.startsWith("Date: ") && str.length() > 31 ) {
                String dateStr = str.substring(10,31).trim();
                if ( dateStr.length() > 0 ) {
                    try {
                        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("dd MMM yyyy HH:mm:ss");
                        return dateTimeFormatter.parseDateTime(dateStr);
                    } catch (IllegalInstantException ex ) { // solve dst problems (missing time-zones)
                        logger.error(ex.getMessage());
                    }
                }
            }
        }
        return null;
    }

    // get from field from an email
    public String getFrom( String[] messageList ) {
        for ( String str : messageList ) {
            if ( str.startsWith("From: ") ) {
                return str.substring(6).trim();
            }
        }
        return null;
    }

    // get from field from an email
    public String getXFrom( String[] messageList ) {
        for ( String str : messageList ) {
            if ( str.startsWith("X-From: ") ) {
                String name = str.substring(8).trim();
                int index = name.indexOf('<');
                if ( index > 0 ) {
                    name = name.substring(0, index).trim();
                }
                String[] nameSet = name.split(",");
                if ( nameSet.length == 2 ) { // reverse name?
                    return nameSet[1].trim() + " " + nameSet[0].trim();
                }
                return name;
            }
        }
        return null;
    }

    // get to field from an email
    public String getTo( String[] messageList ) {
        StringBuilder toList = new StringBuilder();
        for ( int i = 0; i < messageList.length; i++ ) {
            String str = messageList[i];
            if ( str.startsWith("To: ") ) {
                String part1 = str.substring(4).trim();
                toList.append(part1);
                while ( part1.endsWith(",") ) {
                    i++;
                    if ( i < messageList.length ) {
                        part1 = messageList[i].trim();
                        toList.append(part1);
                    }
                }
            }
        }
        return toList.toString();
    }

    // get to field from an email
    public String getToCc( String[] messageList ) {
        StringBuilder toList = new StringBuilder();
        for ( int i = 0; i < messageList.length; i++ ) {
            String str = messageList[i];
            if ( str.startsWith("Cc: ") ) {
                String part1 = str.substring(4).trim();
                toList.append(part1);
                while ( part1.endsWith(",") ) {
                    i++;
                    part1 = messageList[i].trim();
                    toList.append(part1);
                }
            }
        }
        return toList.toString();
    }

    // get to field from an email
    public String getToBcc( String[] messageList ) {
        StringBuilder toList = new StringBuilder();
        for ( int i = 0; i < messageList.length; i++ ) {
            String str = messageList[i];
            if ( str.startsWith("Bcc: ") ) {
                String part1 = str.substring(4).trim();
                toList.append(part1);
                while ( part1.endsWith(",") ) {
                    i++;
                    part1 = messageList[i].trim();
                    toList.append(part1);
                }
            }
        }
        return toList.toString();
    }


    /**
     * find the message body of an Enron email
     * they all have X- attributes ending the header
     * @param messageList the list of lines of an enron email
     * @return the message body string of the email without the extra meta-data
     */
    public String getMessageBody( String[] messageList ) {
        boolean started = false;
        int offset = -1;
        for ( int i = 0; i < messageList.length; i++ ) {
            String message = messageList[i];
            if ( message.startsWith("X-From:") ) {
                started = true;
            }
            if ( started ) {
                if ( !message.startsWith("X-") ) {
                    offset = i;
                    break;
                }
            }
        }
        if ( offset > 0 ) {
            StringBuilder sb = new StringBuilder();
            for ( int i = offset; i < messageList.length; i++ ) {
                messageList[i] = messageList[i].replace("=09", " ");
                messageList[i] = messageList[i].replace("=20", " ");
                if ( messageList[i].endsWith("=\r") && messageList[i].length() > 2 ) {
                    sb.append(messageList[i].substring(0, messageList[i].length() - 2));
                } else {
                    sb.append(messageList[i]).append("\n");
                }
            }
            return sb.toString();
        }
        return null; // not found
    }

}
