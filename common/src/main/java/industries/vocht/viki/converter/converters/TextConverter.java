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

package industries.vocht.viki.converter.converters;

import industries.vocht.viki.VikiException;
import industries.vocht.viki.converter.IDocumentConverter;
import industries.vocht.viki.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/*
 * Created by peter on 18/10/15.
 *
 * simple text converter
 *
 */
public class TextConverter implements IDocumentConverter {

    final Logger logger = LoggerFactory.getLogger(TikaConverter.class);

    public TextConverter() {
    }

    /**
     * text conversion is not the default converter
     * @return false
     */
    public boolean isDefaultConverter() {
        return false;
    }

    /**
     * return true if the extension on the URL is recognized as a supported type
     * for this converter
     * @param url the url of the resource
     * @param mimeType if the url doesn't support the type, then the mimeType takes over
     * @return true if supported
     */
    public boolean isSupported(String url, String mimeType) {
        int index = url.lastIndexOf('.');
        if (index > 0 && (index + 1) < url.length()) {
            String extension = url.substring(index + 1).trim().toLowerCase();
            return (extension.equals("txt") || extension.equals("text"));
        }
        return false;
    }

    /**
     * return the text of  a text document, pretty basic
     * @param url the url of the resource
     * @param binaryData the binary data of the object
     * @return a map of the metadata items
     * @throws VikiException
     */
    public Map<String, String> getText(String url, byte[] binaryData) throws VikiException {
        logger.info("text converter converting: " + url);
        HashMap<String, String> documentMap = new HashMap<>();
        String str = new String(binaryData).trim();
        documentMap.put(Document.META_URL, url);
        if (str.length() > 0) {
            documentMap.put(Document.META_BODY, str);
        }
        return documentMap;
    }

}

