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

package industries.vocht.viki.client;

import industries.vocht.viki.jersey.JsonMessage;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.net.URLEncoder;

/**
 * Created by peter on 27/05/16.
 *
 * neural network trainer client access interface
 *
 */
public class NNetClientInterface extends ClientInterfaceCommon {

    // document system is default port 14080
    public NNetClientInterface(String host, int port ) {
        super(host, port);
    }


    /**
     * convert a document from binary to text
     * @param sessionID the user's session
     * @param wordStr the neural network to start training
     * @return true if it is done / started
     * @throws IOException on failure
     */
    public boolean train(String sessionID, String wordStr) throws IOException {
        HttpPut put = new HttpPut( serverAddress + "/viki/nnet/train/" + URLEncoder.encode(sessionID,"UTF-8") + "/" +
                URLEncoder.encode(wordStr,"UTF-8"));
        HttpResponse httpResponse = send(put);
        return (httpResponse.getStatusLine().getStatusCode() == 200); // check 200
    }


    /**
     * tell the system to upload any missing files it has in the standard folder /opt/kai/data/nnet/*
     * to the database - helper for setup
     * @param sessionID the session to use for auth
     * @return true when its ok
     * @throws IOException error
     */
    public boolean startUploadFromFiles(String sessionID) throws IOException {
        HttpPut put = new HttpPut( serverAddress + "/viki/nnet/upload/" + URLEncoder.encode(sessionID,"UTF-8"));
        HttpResponse httpResponse = send(put);
        return (httpResponse.getStatusLine().getStatusCode() == 200); // check 200
    }


}


