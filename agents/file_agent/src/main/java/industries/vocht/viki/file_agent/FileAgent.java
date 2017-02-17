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

package industries.vocht.viki.file_agent;

import com.google.common.util.concurrent.RateLimiter;
import industries.vocht.viki.agent_common.AgentCommon;
import industries.vocht.viki.agent_common.AgentException;
import industries.vocht.viki.agent_common.IAgent;
import industries.vocht.viki.agent_common.client_interface.AgentAcl;
import industries.vocht.viki.agent_common.client_interface.AgentClientInterface;
import industries.vocht.viki.agent_common.client_interface.AgentDocument;
import industries.vocht.viki.agent_common.client_interface.AgentUser;
import industries.vocht.viki.agent_common.database.KaiAgentDao;
import industries.vocht.viki.agent_common.database.KaiFileDao;
import industries.vocht.viki.agent_common.database.model.KaiAgent;
import industries.vocht.viki.agent_common.database.model.KaiFile;
import jcifs.smb.ACE;
import jcifs.smb.SID;
import jcifs.smb.SmbFile;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Principal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.net.MalformedURLException;


/**
 * Created by peter on 16/06/16.
 *
 * the file agent itself
 *
 * // sample insert script
 * insert into kai_agent values (1, 'file-agent001','administrator','Password1','domain2008','10.17.1.120','documents','',10,'','peter@peter.co.nz','password','localhost',10080,'localhost',14080);
 *
 */
@Component
public class FileAgent extends AgentCommon implements IAgent {

    private static Logger logger = LoggerFactory.getLogger(FileAgent.class);

    @Autowired
    private KaiAgentDao kaiAgentDao;

    @Autowired
    private KaiFileDao kaiFileDao;

    @Value("${file.agent.name:file-agent001}")
    private String agent_name;

    @Value("${file.agent.sleep.in.mins.after.complete:30}")
    private long sleepTimeAfterCompleteInMins;

    // the connection to the remote server
    private SMBConnection smbConnection;

    // connection to KAI
    private AgentClientInterface clientInterface;

    // the session for communicating with KAI
    private AgentUser agentUser;

    // date time formatter
    private SimpleDateFormat formatter;

    // a rate limiter for slowing down the agent
    private RateLimiter rateLimiter;

    // keep a list of folders "to do" instead of recursing the structures
    // in order to keep memory usage to a minimum
    private List<SmbFile> fileList;

    public FileAgent() {
    }


    /**
     * spring init
     */
    public void init() {
        formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    }

    /**
     * start the file agent running
     */
    public void start() throws SQLException, InterruptedException, AgentException {
        // forever run this agent
        do {
            // get the agent
            KaiAgent agent = getAgentFromDb(kaiAgentDao, agent_name);

            // setup a rate limiter
            rateLimiter = RateLimiter.create(agent.getFiles_per_second());

            // setup the interface to talk to KAI
            clientInterface = new AgentClientInterface(agent.getKai_login_server(), agent.getKai_login_port(),
                    agent.getKai_document_server(), agent.getKai_document_port());

            try {
                agentUser = clientInterface.login(agent.getKai_username(), agent.getKai_password());
            } catch (IOException ex) {
                throw new AgentException("login to KAI: " + ex.getMessage());
            }

            // setup the list of files
            fileList = new ArrayList<>();
            logger.info("agent \"" + agent.getName() + "\" starting");

            boolean hasError = false;
            do {
                // establish a connection
                try {
                    smbConnection = new SMBConnection(agent.getServer(), agent.getUsername(), agent.getPassword(), agent.getDomain());
                } catch (MalformedURLException ex) {
                    throw new AgentException(ex.getMessage());
                }

                try {
                    // only add the base files if we haven't had a repeat through an error
                    if (!hasError) {
                        fileList.addAll(smbConnection.getFilesAt(agent.getPath()));
                    } else {
                        hasError = false;
                    }
                    crawl(agent);
                } catch (IOException ex) {
                    logger.error("samba exception: " + ex.getMessage() + ", waiting 30 seconds and re-trying");
                    Thread.sleep(30000);
                    hasError = true;
                }

            } while (hasError);

            logger.info("agent \"" + agent.getName() + "\" finished");

            if (sleepTimeAfterCompleteInMins > 0L) {
                logger.info("agent \"" + agent.getName() + "\" sleeping for " + sleepTimeAfterCompleteInMins + " minutes");
                Thread.sleep(sleepTimeAfterCompleteInMins * 60L * 1000L);
            }

        } while ( true ); // forever
    }

    /**
     * do the crawl, go into folder recursively and get all the files and folders inside
     * and traverse through them one at a time
     * @param agent the agent that is in charge / used
     * @throws IOException
     */
    private void crawl(KaiAgent agent) throws IOException, SQLException {

        // lookup parents for files (i.e. folders)
        Map<SmbFile, Integer> parentLookup = new HashMap<>();

        while ( agent != null && fileList.size() > 0 ) {

            // each iteration, process the files first
            int index = 0;
            while ( index < fileList.size() ) {
                SmbFile file = fileList.get(index);
                if ( file.isFile() ) {
                    rateLimiter.acquire(); // slow down if need be
                    if (file.canRead()) {
                        logger.debug("processing file " + file.getCanonicalPath());
                        Integer parentId = parentLookup.get(file);
                        if ( parentId == null ) {
                            parentId = -1; // top level
                        }
                        processFile(agent, parentId, file);
                        fileList.remove(index); // remove it
                    } else {
                        logger.warn("insufficient privileges to read file: " + file.getCanonicalPath());
                        fileList.remove(index); // remove it
                    }
                } else {
                    index = index + 1;
                }
            } // for each file

            // grab the next folder and expand it
            int folder_index = 0;
            boolean hasFiles = false;
            while ( folder_index < fileList.size() && !hasFiles ) {
                SmbFile folder = fileList.get(folder_index);
                if ( folder.isDirectory() ) {
                    rateLimiter.acquire(); // slow down if need be
                    if (folder.canRead()) {
                        logger.debug("processing folder " + folder.getCanonicalPath());
                        fileList.remove(folder_index); // remove it
                        // get any files in that folder
                        List<SmbFile> remoteFileList = smbConnection.getFilesAt(folder);
                        hasFiles = remoteFileList.size() > 0;  // stop if we find files - go back to file processing first
                        fileList.addAll( remoteFileList );

                        Integer parentId = parentLookup.get(folder);
                        if ( parentId == null ) {
                            parentId = -1; // top level
                        }
                        int folderId = processFolder( agent, parentId, folder ); // container save
                        if ( folderId > 0 ) {
                            // setup this folder's children with a parent
                            for ( SmbFile remote : remoteFileList ) {
                                parentLookup.put( remote, folderId );
                            }
                        }

                    } else {
                        logger.warn("insufficient privileges to read folder: " + folder.getCanonicalPath());
                        fileList.remove(folder_index); // remove it
                    }
                } else {
                    folder_index = folder_index + 1;
                }
            } // for each folder

        } // valid parameters
    }

    /**
     * process a folder
     * @param agent the agent in question
     * @param parentId the id of this folder's parent (or -1 if top level)
     * @param folder the folder to process
     * @return this folder's ID
     */
    private int processFolder( KaiAgent agent, int parentId, SmbFile folder ) throws SQLException, IOException {
        if ( agent != null && folder != null && folder.isDirectory() && folder.canRead() ) {
            String url = removeServerAndShare(folder.getUncPath());
            KaiFile kaiFile = kaiFileDao.getFileByFilename( agent.getId(), url );
            if ( kaiFile == null ) {
                kaiFile = new KaiFile();
                kaiFile.setHash("");
                kaiFile.setLast_uploaded(DateTime.now());
                kaiFile.setLast_checked(DateTime.now());
                kaiFile.setAgent_id(agent.getId());
                kaiFile.setFilename(url);
                kaiFile.setFile_type("folder");
                kaiFile.setParent_id(parentId);
                kaiFile.setMetadata_hash("");
            } else {
                kaiFile.setLast_checked(DateTime.now());
            }
            // always save the file and its timestamps
            return kaiFileDao.saveFile(kaiFile);
        }
        return -1; // invalid
    }

    /**
     * process an agent / file for that agent
     * @param agent the agent in question
     * @param file the file to process
     */
    private void processFile( KaiAgent agent, int parentId, SmbFile file ) throws SQLException, IOException {
        if ( agent != null && file != null && file.isFile() && file.canRead() ) {

            // get the document
            AgentDocument document = getDocumentFromSmb( agent, file );

            // does it exist in our db?
            KaiFile kaiFile = kaiFileDao.getFileByFilename( agent.getId(), document.getUrl() );
            byte[] documentContent = IOUtils.toByteArray(smbConnection.getSMBInputStream(file));

            // we have an existing file?
            boolean changed = true;
            if ( kaiFile != null ) {
                // has its contents changed?
                String hash = calculateHash( documentContent );
                changed = kaiFile.getHash() != null && !kaiFile.getHash().equals(hash);
                kaiFile.setHash(hash);
                kaiFile.setLast_checked(DateTime.now());
            } else {
                kaiFile = new KaiFile();
                kaiFile.setHash(calculateHash( documentContent ));
                kaiFile.setLast_uploaded(DateTime.now());
                kaiFile.setLast_checked(DateTime.now());
                kaiFile.setAgent_id(agent.getId());
                kaiFile.setFilename(document.getUrl());
                kaiFile.setFile_type("file");
                kaiFile.setParent_id(parentId);
                kaiFile.setMetadata_hash("");
            }

            // upload to KAI?
            if ( changed ) {
                // upload the document
                clientInterface.metadata(agentUser.getSessionID().toString(), agent.getName(), document.getUrl(), document);
                // upload the binary content
                clientInterface.upload(agentUser.getSessionID().toString(), document.getUrl(), documentContent);
            }

            // always save the file and its timestamps
            kaiFileDao.saveFile(kaiFile);
        }
    }

    /**
     * get all the required details of an SmbFile and put it inside an agent document
     * @param agent the agent doing the work (origin)
     * @param file the file being processed
     * @return the agent document containing all the required metadata
     * @throws IOException
     */
    private AgentDocument getDocumentFromSmb( KaiAgent agent, SmbFile file ) throws IOException {
        AgentDocument document = new AgentDocument();
        String uncPath = removeServerAndShare(file.getUncPath());
        document.setUrl(uncPath);
        document.setOrigin(agent.getName());
        Principal principal = file.getPrincipal();
        if ( principal != null ) {
            document.setAuthor(principal.getName());
        }
        document.setAcl_set(getAcls(agent, file));
        if ( file.createTime() > 0L ) {
            document.getName_value_set().put(AgentDocument.META_CREATED_DATE_TIME, formatter.format(new Date(file.createTime())));
        }
        if ( file.lastModified() > 0L ) {
            document.getName_value_set().put(AgentDocument.META_LAST_MODIFIED_DATE_TIME, formatter.format(new Date(file.lastModified())));
        }
        for ( String key : file.getHeaderFields().keySet() ) {
            List<String> items = file.getHeaderFields().get(key);
            System.out.println(items.size());
        }
        return document;
    }

    /**
     * remove the server and share name from the unc
     * @param unc the unc to process
     * @return the unc with the server and share names removed
     */
    private String removeServerAndShare( String unc ) {
        if ( unc != null && unc.startsWith("\\\\") ) {
            unc = unc.substring(2); // remove server path
            int index = unc.indexOf("\\");
            if ( index > 0 ) {
                unc = unc.substring(index+1);
            }
            index = unc.indexOf("\\");
            if ( index > 0 ) {
                unc = unc.substring(index);
            }
        }
        return unc;
    }

    /**
     * get the security principals for a file
     * @param file the file
     * @return a set of acls
     */
    private HashSet<AgentAcl> getAcls(KaiAgent agent, SmbFile file) throws IOException {
        HashSet<AgentAcl> aclSet = new HashSet<>();

        ACE[] aclArray = file.getSecurity(true);
        if ( aclArray != null ){
            for ( ACE ace : aclArray ) {
                SID sid = ace.getSID();
                if ( sid != null && sid.getDomainName() != null && sid.getAccountName() != null ) {
                    // don't use builtin or nt auth tokens - they aren't network tokens for users
                    if ( sid.getDomainName().compareToIgnoreCase("NT AUTHORITY") != 0 &&
                         sid.getDomainName().compareToIgnoreCase("BUILTIN") != 0 ) {
                        String sidStr = sid.getDomainName() + "\\" + sid.getAccountName();
                        aclSet.add(new AgentAcl(sidStr, ace.isAllow()));
                    }
                }
            }
        }

        // emergency - no acls - add agent as supervisor
        if ( aclSet.size() == 0 ) {
            aclSet.add( new AgentAcl(agent.getKai_username(),true) );
        }

        return aclSet;
    }








    public static void main( String[] args ) throws Exception {
        String url = "/home/peter/Desktop/Building-the-2021-Affordable-Military.pdf";
        byte[] fileData = Files.readAllBytes(Paths.get(url));
        AgentClientInterface clientInterface = new AgentClientInterface("localhost", 10080, "localhost", 14080);
        AgentUser user = clientInterface.login("peter@peter.co.nz", "password");
        AgentDocument document = new AgentDocument();
        document.setUrl(url);
        document.setOrigin("agent");
        document.getAcl_set().add( new AgentAcl("peter@peter.co.nz", true) );
        clientInterface.metadata(user.getSessionID().toString(), document.getOrigin(), url, document);
        clientInterface.upload(user.getSessionID().toString(), document.getUrl(), fileData);
    }










}

