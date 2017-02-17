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
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/*
 * Created by peter on 18/10/15.
 *
 * use Tika to convert an office document / object to text (if possible)
 *
 */
public class TikaConverter implements IDocumentConverter {

    final Logger logger = LoggerFactory.getLogger(TikaConverter.class);

    // fast lookup structures
    private HashSet<String> supportedExtensionSet;

    // fast lookup for existing metadata items
    private HashSet<String> reservedMetadataItemSet;

    public TikaConverter(HashSet<String> reservedMetadataItemSet) {
        this.reservedMetadataItemSet = reservedMetadataItemSet;
        // setup the supported extensions
        supportedExtensionSet = new HashSet<>((supportedExtensionList.length * 3) / 2);
        for (String str : supportedExtensionList) {
            supportedExtensionSet.add(str);
        }
    }

    /**
     * tika is the default converter
     * @return true
     */
    public boolean isDefaultConverter() {
        return true;
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
        return index > 0 && (index + 1) < url.length() &&
                supportedExtensionSet.contains(url.substring(index + 1).trim().toLowerCase());
    }

    /**
     * return the text of  a tika compatible converter document
     * see http://stackoverflow.com/questions/6656849/extract-the-text-from-urls-using-tika
     * @param url the url of the resource
     * @param binaryData the binary data of the object
     * @return a map of the metadata items
     * @throws VikiException
     */
    public Map<String, String> getText(String url, byte[] binaryData) throws VikiException {
        try {
            logger.info("tika converter converting: " + url);
            HashMap<String, String> documentMap = new HashMap<>();
            documentMap.put(Document.META_URL, url);

            // try and auto-detect the Tika object
            AutoDetectParser parser = new AutoDetectParser();
            // -1 avoids the : Your document contained more than 100000 characters  error
            BodyContentHandler textHandler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();
            //context.getXMLReader();

            InputStream inputStream = new ByteArrayInputStream(binaryData);
            parser.parse(inputStream, textHandler, metadata, context);

            if ( metadata.names() != null ) {
                for (String name : metadata.names()) {
                    String safeName = name;
                    // make sure metadata names don't collide with ours
                    if ( reservedMetadataItemSet.contains(name) ) {
                        safeName = "internal " + name;
                    }
                    documentMap.put(safeName, metadata.get(name));
                }
            }
            String textBody = textHandler.toString().trim();
            if (textBody.length() > 0) {
                documentMap.put(Document.META_BODY, textBody);
            }
            return documentMap;
        } catch (Exception ex) {
            throw new VikiException(url + " : " + ex.getMessage());
        }
    }

    // http://en.wikipedia.org/wiki/List_of_Microsoft_Office_filename_extensions
    // http://tika.apache.org/1.6/formats.html
    private static String[] supportedExtensionList = new String[]
            {
                    "doc", "docx", "dotx", "dotm", "docb", "dot",
                    "xls", "xlt", "xlm", "xlsx", "xlsm", "xltx", "xltm",
                    "ppt", "pot", "pps", "pptx", "pptm", "potx", "ppam", "ppsx", "ppsm", "sldx", "sldm",
                    "xml", "html", "htm", "xhtml",
                    "odf",
                    "wav", "aiff", "mid", "mp3", "mp4", "m4v", "flac", "ogg", "ogm", "oga", "flv",
                    //"chm", // tika isn't very good at converting chm files
                    "dwg",
                    "epub",
                    "rss", "atom",
                    "bmp", "png", "ico", "gif", "xcf", "tiff", "tif", "jpeg", "jpg",
                    "psd",
                    "apxl", "iwa", "key-tef", "mmbtemplate", "pages-tef",
                    "tnef",
                    "odt", "fodt", "ods", "fods", "odp", "fodp", "odg", "fodg", "odf",
                    "rtf",
                    "pdf"
            };

}
