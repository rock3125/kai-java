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

package industries.vocht.viki.services;

import industries.vocht.viki.model.Acl;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.document.DocumentList;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;

/**
 * Created by peter on 5/03/16.
 *
 */
public class DocumentServiceTest extends TestBase {

    // get a document that doesn't exist means null
    @Test
    public void testDocument1() throws Exception {
        Document d1 = documentService.getDocument(sessionID, "none-existent-url", IP_ADDRESS_1);
        Assert.assertNull(d1);
    }

    // create a document and get it back and check its meta-data
    @Test
    public void testDocument2() throws Exception {
        HashSet<Acl> aclSet = new HashSet<>();
        aclSet.add(new Acl("peter@peter.co.nz", true));
        aclSet.add(new Acl("mark@peter.co.nz", false));

        Document test = new Document();
        test.setAcl_set(aclSet);
        test.setOrigin("origin1");

        Document d2 = new Document();
        d2.setUrl("http://document1");
        d2.setAcl_set(aclSet);
        d2.setOrigin("origin1");

        // create it
        Document d1 = documentService.saveDocument(sessionID, d2, IP_ADDRESS_1);
        Assert.assertNotNull(d1);

        // get it back and check it
        Document d3 = documentService.getDocument(sessionID, "http://document1", IP_ADDRESS_1);
        Assert.assertNotNull(d3);
        Assert.assertNotNull(d3.getOrganisation_id());
        Assert.assertTrue(d3.getDate_time_uploaded() == 0L);
        Assert.assertEquals(d3.getUrl(), "http://document1");
        Assert.assertEquals(d3.getOrganisation_id(), org1.getId());
        Assert.assertNotNull(d3.getAcl_set());
        Assert.assertEquals(d3.aclsToPrettyString(), test.aclsToPrettyString());
        Assert.assertEquals(d3.getOrigin(), test.getOrigin());

        // check the meta-data records for this document
        String value = d3.getName_value_set().get(Document.META_URL);
        Assert.assertNotNull(value);
        Assert.assertEquals(value, "http://document1");

        String value2 = d3.getName_value_set().get(Document.META_ACLS);
        Assert.assertNotNull(value2);
        Assert.assertEquals(value2, test.aclsToPrettyString());

        String value3 = d3.getName_value_set().get(Document.META_UPLOAD_DATE_TIME);
        Assert.assertNotNull(value3);

        String value4 = d3.getName_value_set().get(Document.META_ORIGIN);
        Assert.assertNotNull(value4);
        Assert.assertEquals(value4, test.getOrigin());
    }

    private String createUrl(int i) {
        if ( i < 10 ) {
            return "http://document0" + i;
        } else {
            return "http://document" + i;
        }
    }

    // create a few documents and test delete
    @Test
    public void testDocument3() throws Exception {
        // fake acls
        HashSet<Acl> aclSet = new HashSet<>();
        aclSet.add(new Acl("peter@peter.co.nz", true));
        aclSet.add(new Acl("mark@peter.co.nz", false));

        Document test = new Document();
        test.setAcl_set(aclSet);

        // create 10 documents
        for ( int i = 0; i < 10; i++ ) {
            Document d2 = new Document();
            d2.setUrl(createUrl(i+1));
            d2.setAcl_set(aclSet);
            documentService.saveDocument(sessionID, d2, IP_ADDRESS_1);
        }

        // get a paginated list set of 5 out of 10
        DocumentList documentListObj = documentService.getPaginatedDocumentList(sessionID, null, 5, IP_ADDRESS_1);
        List<Document> documentList = documentListObj.getDocument_list();
        Assert.assertNotNull(documentList);
        Assert.assertEquals(documentList.size(), 5);

        // delete one of them
        documentService.deleteDocument(sessionID, "http://document01", IP_ADDRESS_1);

        // check the paginated set
        documentListObj = documentService.getPaginatedDocumentList(sessionID, null, 10, IP_ADDRESS_1);
        documentList = documentListObj.getDocument_list();
        Assert.assertNotNull(documentList);
        Assert.assertEquals(documentList.size(), 9);

        // test these 9 documents are as expected
        documentListObj.sort(); // sort and sort documentList ref in the process
        int counter = 2;
        for ( Document d4 : documentList ) {
            Assert.assertNotNull(d4);
            Assert.assertNotNull(d4.getOrganisation_id());
            Assert.assertTrue(d4.getDate_time_uploaded() == 0L);
            Assert.assertEquals(d4.getUrl(), createUrl(counter));
            Assert.assertEquals(d4.getOrganisation_id(), org1.getId());
            Assert.assertNotNull(d4.getAcl_set());
            Assert.assertEquals(d4.aclsToPrettyString(), test.aclsToPrettyString());
            counter++;
        }
    }

    // test document upload
    @Test
    public void testDocument4() throws Exception {
        // fake acls
        HashSet<Acl> aclSet = new HashSet<>();
        aclSet.add(new Acl("peter@peter.co.nz", true));
        aclSet.add(new Acl("mark@peter.co.nz", false));

        Document d2 = new Document();
        d2.setUrl(createUrl(1));
        d2.setAcl_set(aclSet);
        documentService.saveDocument(sessionID, d2, IP_ADDRESS_1);

        byte[] data = new byte[10];
        for ( int i = 0 ; i < data.length; i++ ) {
            data[i] = (byte)i;
        }
        documentService.uploadDocument(sessionID, createUrl(1), data, IP_ADDRESS_1);

        byte[] data2 = documentService.getDocumentBinary(sessionID, createUrl(1), IP_ADDRESS_1);
        Assert.assertNotNull(data2);
        Assert.assertEquals(data.length, data2.length);
        for ( int i = 0; i < data.length; i++ ) {
            Assert.assertEquals(data[i], data2[i]);
        }

        // check its additional meta-data
        String value = d2.getName_value_set().get(Document.META_UPLOAD_DATE_TIME);
        Assert.assertNotNull(value);
    }


}
