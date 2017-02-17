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
 * Created by peter on 2/04/16.
 *
 */
public class ConverterClientInterface extends ClientInterfaceCommon {

    // document system is default port 14080
    public ConverterClientInterface(String host, int port ) {
        super(host, port);
    }

    /**
     * convert a document from binary to text
     * @param sessionID the user's session
     * @param url the url of the document
     * @return true if it is done
     * @throws IOException on failure
     */
    public boolean convert(String sessionID, String url) throws IOException {
        HttpPut put = new HttpPut( serverAddress + "/viki/convert/start/" + URLEncoder.encode(sessionID,"UTF-8") + "/" +
                URLEncoder.encode(url,"UTF-8"));
        HttpResponse httpResponse = send(put);
        return (httpResponse.getStatusLine().getStatusCode() == 200); // check 200
    }

}

