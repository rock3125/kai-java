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

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by peter on 16/06/16.
 *
 * a connection to a remove samba server
 *
 */
public class SMBConnection {

    // authentication system
    private NtlmPasswordAuthentication authentication;

    // the remote server
    private String server;

    // constructor
    public SMBConnection(String server, String username, String password, String domain) throws MalformedURLException {
        SMBAuthenticator authenticator = new SMBAuthenticator(username, password, domain);
        this.authentication = authenticator.getNtlmPasswordAuthentication();
        this.server = server;
    }

    private String convert( String unc ) {
        return "smb://" + server + "/" + unc;
    }

    /**
     * get a stream for the given file
     * @return the stream of the remote file
     * @throws IOException
     */
    public InputStream getSMBInputStream( SmbFile file ) throws IOException {
        if ( file != null && file.isFile() ) {
            return file.getInputStream();
        }
        return null;
    }

    /**
     * get a list of folders at the path
     * @param unc the path into the system
     * @return a list of folders (or empty list)
     * @throws IOException
     */
    public List<SmbFile> getFilesAt( String unc ) throws IOException {
        String folder = convert(unc);
        if ( !folder.endsWith("/") ) {
            folder = folder + "/";
        }
        return getFilesAt(new SmbFile(folder, authentication));
    }


    /**
     * get a list of folders at the path
     * @param currentFolder the folder of the file
     * @return a list of folders (or empty list)
     * @throws IOException
     */
    public List<SmbFile> getFilesAt( SmbFile currentFolder ) throws IOException {
        List<SmbFile> fileList = new ArrayList<>();
        if ( currentFolder != null && currentFolder.isDirectory() ) {
            SmbFile[] listFiles = currentFolder.listFiles();
            if (listFiles != null) {
                fileList.addAll(Arrays.asList(listFiles));
            }
        }
        return fileList;
    }


}

