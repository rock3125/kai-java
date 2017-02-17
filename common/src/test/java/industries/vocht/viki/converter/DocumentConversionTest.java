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

import industries.vocht.viki.BaseTest;
import industries.vocht.viki.VikiException;
import industries.vocht.viki.document.Document;
import org.junit.Test;
import org.springframework.util.Assert;

import javax.print.Doc;
import java.io.IOException;
import java.util.Map;

/**
 * Created by peter on 20/06/16.
 *
 * test the tika system and others can convert documents
 * as we need them
 *
 */
public class DocumentConversionTest {

    // test a pdf file can be converted without knowledge of its type
    @Test
    public void testPdf1() throws IOException, VikiException {
        byte[] raw = new BaseTest().loadItemFromResources("/common/go.pdf");
        Assert.notNull(raw);
        DocumentConverter converter = new DocumentConverter();
        Map<String, String> textMap = converter.getText("some file", raw);
        Assert.notNull(textMap);
        Assert.isTrue( textMap.containsKey(Document.META_BODY) );
        String bodyText = textMap.get(Document.META_BODY);
        Assert.isTrue(bodyText.length() > 1024);
        Assert.isTrue(bodyText.contains("I’ve always had a love-hate relationship when it comes to learning new languages"));
    }

    // test a pdf file can be converted WITH knowledge of its type
    @Test
    public void testPdf2() throws IOException, VikiException {
        byte[] raw = new BaseTest().loadItemFromResources("/common/go.pdf");
        Assert.notNull(raw);
        DocumentConverter converter = new DocumentConverter();
        Map<String, String> textMap = converter.getText("go.pdf", raw);
        Assert.notNull(textMap);
        Assert.isTrue( textMap.containsKey(Document.META_BODY) );
        String bodyText = textMap.get(Document.META_BODY);
        Assert.isTrue(bodyText.length() > 1024);
        Assert.isTrue(bodyText.contains("I’ve always had a love-hate relationship when it comes to learning new languages"));
    }

    // test an open office odt file
    @Test
    public void testOdt1() throws IOException, VikiException {
        byte[] raw = new BaseTest().loadItemFromResources("/common/KiwiNet_1_Way_NDA_Template.odt");
        Assert.notNull(raw);
        DocumentConverter converter = new DocumentConverter();
        Map<String, String> textMap = converter.getText("some file", raw);
        Assert.notNull(textMap);
        Assert.isTrue( textMap.containsKey(Document.META_BODY) );
        String bodyText = textMap.get(Document.META_BODY);
        Assert.isTrue(bodyText.length() > 1024);
        Assert.isTrue(bodyText.contains("The Discloser agrees to disclose to the Recipient certain Confidential Information"));
    }


    // test a plain text file
    @Test
    public void testText1() throws IOException, VikiException {
        byte[] raw = new BaseTest().loadItemFromResources("/common/the_crystal_crypt.txt");
        Assert.notNull(raw);
        DocumentConverter converter = new DocumentConverter();
        Map<String, String> textMap = converter.getText("some file.txt", raw);
        Assert.notNull(textMap);
        Assert.isTrue( textMap.containsKey(Document.META_BODY) );
        String bodyText = textMap.get(Document.META_BODY);
        Assert.isTrue(bodyText.length() > 1024);
        Assert.isTrue(bodyText.contains("an anxious passenger asked one of the pilots"));
    }


    // test a plain text file with an unknown file extension
    @Test
    public void testText2() throws IOException, VikiException {
        byte[] raw = new BaseTest().loadItemFromResources("/common/the_crystal_crypt.txt");
        Assert.notNull(raw);
        DocumentConverter converter = new DocumentConverter();
        Map<String, String> textMap = converter.getText("some file", raw);
        Assert.notNull(textMap);
        Assert.isTrue( textMap.containsKey(Document.META_BODY) );
        String bodyText = textMap.get(Document.META_BODY);
        Assert.isTrue(bodyText.length() > 1024);
        Assert.isTrue(bodyText.contains("an anxious passenger asked one of the pilots"));
    }


    // test a microsoft XLSX file
    @Test
    public void testXlsx1() throws IOException, VikiException {
        byte[] raw = new BaseTest().loadItemFromResources("/common/usernames_2.xlsx");
        Assert.notNull(raw);
        DocumentConverter converter = new DocumentConverter();
        Map<String, String> textMap = converter.getText("some file", raw);
        Assert.notNull(textMap);
        Assert.isTrue( textMap.containsKey(Document.META_BODY) );
        String bodyText = textMap.get(Document.META_BODY);
        Assert.isTrue(bodyText.length() > 100);
        Assert.isTrue(bodyText.contains("malcolm.reese"));
    }


    // test a microsoft XLS file (older)
    @Test
    public void testXls1() throws IOException, VikiException {
        byte[] raw = new BaseTest().loadItemFromResources("/common/capbudg.xls");
        Assert.notNull(raw);
        DocumentConverter converter = new DocumentConverter();
        Map<String, String> textMap = converter.getText("some file", raw);
        Assert.notNull(textMap);
        Assert.isTrue( textMap.containsKey(Document.META_BODY) );
        String bodyText = textMap.get(Document.META_BODY);
        Assert.isTrue(bodyText.length() > 100);
        Assert.isTrue(bodyText.contains("OPERATING CASHFLOWS"));
    }


}

