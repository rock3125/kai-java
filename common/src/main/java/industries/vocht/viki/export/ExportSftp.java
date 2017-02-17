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

package industries.vocht.viki.export;

import com.jcraft.jsch.*;
import org.slf4j.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Created by peter on 13/06/16.
 *
 * export a file / report using sftp
 *
 */
@Component
public class ExportSftp {

    private static final Logger logger = LoggerFactory.getLogger(ExportSftp.class);

    @Value("${sftp.known.hosts.file.location:/root/.ssh/known_hosts}")
    private String knownHostsFile;

    public ExportSftp() {
    }

    /**
     * perform an SFTP export using the JSch library
     * @param remoteUsername the remote username
     * @param remotePassword the remote password
     * @param remoteHost the remote host
     * @param remoteFile the remote filename (path and filename!)
     * @throws IOException
     */
    public void export( String remoteUsername, String remotePassword, String remoteHost, String remoteFile,
                        String content ) throws IOException {
        ChannelSftp sftpChannel = null;
        Session session = null;
        try {
            JSch transfer = new JSch();
            transfer.setKnownHosts(knownHostsFile);

            session = transfer.getSession(remoteUsername, remoteHost);
            // relies on host key being in known-hosts file
            session.setPassword(remotePassword);

            session.connect();
            Channel channel = session.openChannel("sftp");
            channel.connect();

            sftpChannel = (ChannelSftp) channel;
            sftpChannel.put( new ByteArrayInputStream(content.getBytes()), remoteFile );

        } catch (JSchException | SftpException ex) {
            throw new IOException(ex.getMessage());
        } finally {
            if ( sftpChannel != null ) {
                try {
                    sftpChannel.exit();
                } catch (Exception ex) {
                    logger.debug(ex.getMessage());
                }
            }
            if ( session != null ) {
                try {
                    session.disconnect();
                } catch (Exception ex) {
                    logger.debug(ex.getMessage());
                }
            }
        } // finally

    }

}

