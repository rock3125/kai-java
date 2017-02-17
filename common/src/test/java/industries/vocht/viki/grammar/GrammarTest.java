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

package industries.vocht.viki.grammar;

import industries.vocht.viki.model.Token;
import industries.vocht.viki.tokenizer.Tokenizer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

/**
 * Created by peter on 19/04/16.
 *
 * test the grammar parser library
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test-context.xml"})
public class GrammarTest {

    @Autowired
    private GrammarLibrary grammarLibrary;

    @Before
    public void setup() {
        Assert.notNull(grammarLibrary);
    }

    @Test
    public void testDateTime1() {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokenList = grammarLibrary.parse( tokenizer.tokenize("2016-04-18 15:59:07") );
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("2016-04-18 15:59:07") );
    }

    @Test
    public void testDateTime2() {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokenList = grammarLibrary.parse( tokenizer.tokenize("2016-04-18 15:59:07.123") );
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("2016-04-18 15:59:07.123") );
    }

    @Test
    public void testDateTime3() {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokenList = grammarLibrary.parse( tokenizer.tokenize("2011-01-31") );
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("2011-01-31") );
    }

    @Test
    public void testDateTime4() {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokenList = grammarLibrary.parse( tokenizer.tokenize("2016-04-12 11:22 PM") );
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("2016-04-12 11:22 PM") );
    }

    @Test
    public void testDateTime5() {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokenList = grammarLibrary.parse( tokenizer.tokenize("June 1, 2001") );
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("June 1, 2001") );
    }

    @Test
    public void testUrl1() {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokenList = grammarLibrary.parse( tokenizer.tokenize("mailto://Blair-l/customer___oneok/22.txt") );
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("mailto://Blair-l/customer___oneok/22.txt") );
    }

    @Test
    public void testUrl2() {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokenList = grammarLibrary.parse( tokenizer.tokenize("www.peter.co.nz") );
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("www.peter.co.nz") );
    }

    @Test
    public void testUrl3() {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokenList = grammarLibrary.parse( tokenizer.tokenize("http://www.peter.co.nz") );
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("http://www.peter.co.nz") );
    }

    @Test
    public void testUrl4() {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokenList = grammarLibrary.parse( tokenizer.tokenize("mailto://Blair-l/customer___oneok/22.txt,mailto://Blair-l/customer___oneok/23.txt") );
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 3);
        Assert.isTrue( tokenList.get(0).getText().equals("mailto://Blair-l/customer___oneok/22.txt") );
        Assert.isTrue( tokenList.get(2).getText().equals("mailto://Blair-l/customer___oneok/23.txt") );
    }

    @Test
    public void testTime1() {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokenList = grammarLibrary.parse( tokenizer.tokenize("11:23:00 AM") );
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("11:23:00 AM") );
    }

    @Test
    public void testTime2() {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokenList = grammarLibrary.parse( tokenizer.tokenize("23:23") );
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("23:23") );
    }

    @Test
    public void testTime3() {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokenList = grammarLibrary.parse( tokenizer.tokenize("23:23:00.000") );
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("23:23:00.000") );
    }

    @Test
    public void testTime4() {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokenList = grammarLibrary.parse( tokenizer.tokenize("00:00:00 PM") );
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("00:00:00 PM") );
    }

    @Test
    public void testTime5() {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokenList = grammarLibrary.parse( tokenizer.tokenize("00:00:00.000") );
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("00:00:00.000") );
    }

    @Test
    public void testTime6() {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokenList = grammarLibrary.parse( tokenizer.tokenize("23:59:59.999") );
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("23:59:59.999") );
    }

    @Test
    public void testPhone1() {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokenList = grammarLibrary.parse( tokenizer.tokenize("713-853-5660") );
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("713-853-5660") );
    }

    @Test
    public void testPhone2() {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokenList = grammarLibrary.parse( tokenizer.tokenize("(713) 853-5660") );
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1);
        Assert.isTrue( tokenList.get(0).getText().equals("(713) 853-5660") );
    }

}


