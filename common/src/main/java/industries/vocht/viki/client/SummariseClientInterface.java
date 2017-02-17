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

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;

import java.io.IOException;
import java.net.URLEncoder;

/**
 * Created by peter on 2/04/16.
 *
 */
public class SummariseClientInterface extends ClientInterfaceCommon {

    public SummariseClientInterface(String host, int port) {
        super(host, port);
    }

    /**
     * summarise a document using its own sentences
     * @param sessionID the session id to use for security
     * @param url the url of the document
     * @return true if successful, exception otherwise
     */
    public boolean summarise(String sessionID, String url) throws IOException {
        HttpPut put = new HttpPut( serverAddress + "/viki/summarization/summarize/" + URLEncoder.encode(sessionID,"UTF-8") + "/" +
                URLEncoder.encode(url,"UTF-8"));
        HttpResponse httpResponse = send(put);
        return (httpResponse.getStatusLine().getStatusCode() == 200); // check 200
    }


}

