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

package industries.vocht.viki.daos;

import industries.vocht.viki.IDao;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.model.CompressedVector;
import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.model.Vector;
import industries.vocht.viki.model.similar.SimilarDocument;
import industries.vocht.viki.model.similar.SimilarDocumentSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.*;

/**
 * Created by peter on 20/06/16.
 *
 * test the document dao system
 *
 */
@SuppressWarnings("ConstantConditions")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/domain/test-context.xml"})
public class DocumentDaoTest {

    @Autowired
    private IDao dao;


    @Test
    public void testCrud1() throws IOException {
        UUID org = UUID.randomUUID();
        Document d1 = createDocument("url://1.txt");
        dao.getDocumentDao().create(org, d1);
        Document d2 = dao.getDocumentDao().read(org, "url://1.txt");
        Assert.isTrue( d2 !=null && d2.getUrl().equals("url://1.txt") && d2.getOrganisation_id().equals(org) );
    }


    @Test
    public void testCrud2() throws IOException {
        UUID org = UUID.randomUUID();
        String url = "url://2.txt";
        Document d1 = createDocument(url);
        dao.getDocumentDao().create(org, d1);

        Document d2 = dao.getDocumentDao().read(org, url);
        Assert.isTrue( d2 !=null && d2.getUrl().equals(url) && d2.getOrganisation_id().equals(org) );
        Assert.isTrue( d2 !=null && d2.getAuthor().equals("Peter") && d2.getOrganisation_id().equals(org) );

        d2.setAuthor("Peter de Vocht");
        dao.getDocumentDao().update(org, d2);

        Document d3 = dao.getDocumentDao().read(org, url);
        Assert.isTrue( d3 !=null && d3.getUrl().equals(url) && d3.getOrganisation_id().equals(org) );
        Assert.isTrue( d3 !=null && d3.getAuthor().equals("Peter de Vocht") );
    }


    @Test
    public void testCrud3() throws IOException {
        UUID org = UUID.randomUUID();
        String url = "url://2.txt";
        Document d1 = createDocument(url);
        dao.getDocumentDao().create(org, d1);

        Document d2 = dao.getDocumentDao().read(org, url);
        Assert.isTrue( d2 !=null && d2.getUrl().equals(url) && d2.getOrganisation_id().equals(org) );
        Assert.isTrue( d2 !=null && d2.getAuthor().equals("Peter") && d2.getOrganisation_id().equals(org) );

        dao.getDocumentDao().delete(org, url);

        Document d3 = dao.getDocumentDao().read(org, url);
        Assert.isNull(d3);
    }


    @Test
    public void testDocumentList1() throws IOException {
        UUID org = UUID.randomUUID();

        dao.getDocumentDao().create(org, createDocument("url://1.txt"));
        dao.getDocumentDao().create(org, createDocument("url://2.txt"));
        dao.getDocumentDao().create(org, createDocument("url://3.txt"));
        dao.getDocumentDao().create(org, createDocument("url://4.txt"));

        List<Document> documentList = dao.getDocumentDao().getDocumentList(org, null, 10);
        Assert.notNull(documentList);
        Assert.isTrue(documentList.size() == 4);
        Assert.isTrue( hasUrl(documentList, "url://1.txt") );
        Assert.isTrue( hasUrl(documentList, "url://2.txt") );
        Assert.isTrue( hasUrl(documentList, "url://3.txt") );
        Assert.isTrue( hasUrl(documentList, "url://4.txt") );
    }


    @Test
    public void testDocumentList2() throws IOException {
        UUID org = UUID.randomUUID();

        dao.getDocumentDao().create(org, createDocument("url://1a.txt"));
        dao.getDocumentDao().create(org, createDocument("url://2a.txt"));
        dao.getDocumentDao().create(org, createDocument("url://3a.txt"));
        dao.getDocumentDao().create(org, createDocument("url://4a.txt"));

        List<String> urlList = dao.getDocumentDao().getDocumentUrlList(org, null, 10);
        Assert.notNull(urlList);
        Assert.isTrue(urlList.size() == 4);
        Assert.isTrue( urlList.contains("url://1a.txt") );
        Assert.isTrue( urlList.contains("url://2a.txt") );
        Assert.isTrue( urlList.contains("url://3a.txt") );
        Assert.isTrue( urlList.contains("url://4a.txt") );
    }


    @Test
    public void testDocumentMap1() throws IOException {
        UUID org = UUID.randomUUID();

        Map<String, byte[]> binMap = new HashMap<>();
        binMap.put( "b1", new byte[125] );
        binMap.put( "b2", new byte[333] );

        dao.getDocumentDao().saveDocumentParseTreeMap(org, "url1", binMap);

        Map<String, byte[]> map2 = dao.getDocumentDao().getDocumentParseTreeMap(org, "url1");
        Assert.notNull(map2);
        Assert.isTrue( map2.size() == 2 );
        Assert.isTrue( map2.containsKey("b1") && map2.get("b1").length == 125 );
        Assert.isTrue( map2.containsKey("b2") && map2.get("b2").length == 333 );
    }


    @Test
    public void testDocumentBinary1() throws IOException {
        UUID org = UUID.randomUUID();
        dao.getDocumentDao().updateDocumentBinary( org, "url2", new byte[512] );

        byte[] data = dao.getDocumentDao().getDocumentBinary(org, "url2");
        Assert.notNull(data);
        Assert.isTrue( data.length == 512 );
    }



    @Test
    public void testDocumentHistorgrams() throws IOException {
        UUID org = UUID.randomUUID();
        Vector v1 = new Vector(25);
        v1.set(5, 125.0);
        CompressedVector vector = new CompressedVector(v1);
        dao.getDocumentDao().saveDocumentHistogram( org, "url2", vector );

        CompressedVector v2 = dao.getDocumentDao().loadDocumentHistogram(org, "url2");
        Assert.notNull(v2);
        Vector v3 = v2.convert();
        Assert.notNull(v3);
        Assert.isTrue( v3.size() == 25 && v3.get(5) == 125.0 && v3.get(0) == 0.0 );
    }


    @Test
    public void testDocumentSummary1() throws IOException {
        UUID org = UUID.randomUUID();
        Sentence s1 = new Sentence();
        s1.getTokenList().add( new Token("t1"));
        s1.getTokenList().add( new Token("t2"));
        dao.getDocumentDao().saveDocumentSummarizationSentenceSet( org, "url2", s1 );

        Sentence sentence = dao.getDocumentDao().loadDocumentSummarizationSentenceSet(org, "url2");
        Assert.notNull(sentence);
        Assert.isTrue(sentence.getTokenList().size() == 2);
        Assert.isTrue(sentence.getTokenList().get(0).getText().equals("t1"));
        Assert.isTrue(sentence.getTokenList().get(1).getText().equals("t2"));
    }


    @Test
    public void testDocumentSimilarity() throws IOException {
        UUID org = UUID.randomUUID();
        List<SimilarDocument> similarDocumentList = new ArrayList<>();
        similarDocumentList.add( new SimilarDocument("url1", "url2", 1.0));
        similarDocumentList.add( new SimilarDocument("url3", "url4", 0.5));
        dao.getDocumentDao().saveDocumentSimilarityList( org, similarDocumentList );

        List<SimilarDocumentSet> set1 = dao.getDocumentDao().loadSimilarDocumentList(org);
        Assert.notNull(set1);
        Assert.isTrue(set1.size() == 4);

        List<SimilarDocument> set2 = dao.getDocumentDao().loadSimilarDocumentList(org, "url1");
        Assert.notNull(set2);
        Assert.isTrue(set2.size() == 3);
        Assert.isTrue( contains(set2, "url2", 1.0) );
        Assert.isTrue( contains(set2, "url3", 0.75) );
        Assert.isTrue( contains(set2, "url4", 0.75) );
    }



    //////////////////////////////////////////////////////////////////////////////////////////
    // helper functions

    private Document createDocument( String url ) {
        Document document = new Document();
        document.setUrl(url);
        document.setAuthor("Peter");
        return document;
    }

    private boolean contains( List<SimilarDocument> list, String url, double score ) {
        for ( SimilarDocument item : list ) {
            if ( item.getUrl2().equals(url) && item.getSimilarity() == score ) {
                return true;
            }
        }
        return false;
    }

    private boolean hasUrl( List<Document> documentList, String url ) {
        for ( Document document : documentList ) {
            if ( document.getUrl().equals(url) ) {
                return true;
            }
        }
        return false;
    }

}


