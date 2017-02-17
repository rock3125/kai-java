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

package industries.vocht.viki.agent_common;

import industries.vocht.viki.agent_common.database.KaiAgentDao;
import industries.vocht.viki.agent_common.database.model.KaiAgent;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

/**
 * Created by peter on 16/06/16.
 *
 * code utilities common to all agents
 *
 */
public class AgentCommon {

    private static Logger logger = LoggerFactory.getLogger(AgentCommon.class);

    /**
     * get the agent by name from the database so we can start
     * @throws SQLException
     * @throws InterruptedException
     */
    protected KaiAgent getAgentFromDb(KaiAgentDao kaiAgentDao, String agent_name) throws SQLException, InterruptedException {
        logger.info("getAgentFromDb: file.agent.name=" + agent_name);
        KaiAgent agent = kaiAgentDao.getAgentByName(agent_name);
        while ( agent == null ) {
            logger.warn("getAgentFromDb: agent \"" + agent_name + "\" not found, waiting 30 seconds and trying again.");
            Thread.sleep(30000);
            agent = kaiAgentDao.getAgentByName(agent_name);
        }
        return agent;
    }

    /**
     * generate an MD5 hash of a String, return the hash string
     * @param str the binary array, must not be null, or empty
     * @return the generated hash as a string
     */
    protected String calculateHash( String str ) {
        if ( str != null && str.length() > 0 ) {
            try {
                return calculateHash(str.getBytes("UTF-8"));
            } catch ( UnsupportedEncodingException ex ) {
                logger.error("UTF-8 encoding not supported, FATAL");
                System.exit(1);
            }
        }
        return null;
    }

    /**
     * generate an MD5 hash of a binary array, return the hash string
     * @param data the binary array, must not be null, or empty
     * @return the generated hash as a string
     */
    protected String calculateHash( byte[] data ) {
        if ( data != null && data.length > 0 ) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] digest = md.digest(data);
                if (digest != null && digest.length > 0) {
                    return Hex.encodeHexString(digest);
                }
            } catch ( NoSuchAlgorithmException ex ) {
                logger.error("MD5 digest not supported, FATAL");
                System.exit(1);
            }
        }
        return null;
    }


}

