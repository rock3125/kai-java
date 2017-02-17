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

package industries.vocht.viki.converter;

import industries.vocht.viki.VikiException;
import industries.vocht.viki.converter.converters.TextConverter;
import industries.vocht.viki.converter.converters.TikaConverter;
import industries.vocht.viki.document.Document;
import org.springframework.stereotype.Component;

import java.util.*;

/*
 * Created by peter on 18/10/15.
 *
 * apply all known converts to convert a binary document
 *
 */
@Component
public class DocumentConverter {

    private List<IDocumentConverter> documentConverterList;

    public DocumentConverter() {

        // fast lookup of reservedMetadataItems
        HashSet<String> reservedMetadataItemSet = new HashSet<>();
        reservedMetadataItemSet.add(Document.META_BODY);
        reservedMetadataItemSet.add(Document.META_URL);
        reservedMetadataItemSet.add(Document.META_TITLE);
        reservedMetadataItemSet.add(Document.META_AUTHOR);
        reservedMetadataItemSet.add(Document.META_ORIGIN);
        reservedMetadataItemSet.add(Document.META_ACLS);
        reservedMetadataItemSet.add(Document.META_UPLOAD_DATE_TIME);
        reservedMetadataItemSet.add(Document.META_CREATED_DATE_TIME);

        documentConverterList = new ArrayList<>();
        documentConverterList.add(new TikaConverter(reservedMetadataItemSet));
        documentConverterList.add(new TextConverter());
    }

    // convert a document / url combination to text (and get all other metadata)
    public Map<String, String> getText(String url, byte[] data) throws VikiException {
        if ( data != null && data.length > 0 ) {
            // ask the converters if they support this document
            for (IDocumentConverter converter : documentConverterList) {
                if (converter.isSupported(url, null)) {
                    return converter.getText(url, data);
                }
            }
            // if the document could not be identified, we pass it to the default converter
            for (IDocumentConverter converter : documentConverterList) {
                if (converter.isDefaultConverter()) {
                    return converter.getText(url, data);
                }
            }
            // no defaults - bad!!!
            throw new VikiException("document format not supported or unknown: " + url);
        }
        return new HashMap<>();
    }

}

