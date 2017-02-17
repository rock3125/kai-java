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

package industries.vocht.viki.tokenizer;

import industries.vocht.viki.model.Token;
import org.junit.Test;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Created by peter on 6/05/16.
 *
 * test the system's tokenizer
 *
 */
public class TokenizerTest {

    // test the tokenization is correct test 1
    // test re-stringing as well with approximate good behaviour
    @Test
    public void testTokenizer1() {
        Tokenizer tokenizer = new Tokenizer();

        // test a simple string is split into the right tokens
        List<Token> tokenList = tokenizer.tokenize("This, is a  test string.  Is what I mean   to talk about?");
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 26); // includes spaces and punctuation

        // remove the spaces
        tokenList = tokenizer.filterOutSpaces(tokenList);
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 15); // includes spaces and punctuation

        // test the "toString" method for pretty print
        String prettyStr = tokenizer.toString(tokenList);
        Assert.isTrue( prettyStr.equals("This, is a test string. Is what I mean to talk about?"));
    }


    // check contractions are glued back together (since their grammatical category doesn't interest Booktrack)
    @Test
    public void testContractions1() {
        checkContraction("couldn't");
        checkContraction("didn't");
        checkContraction("don't");
        checkContraction("doesn't");
        checkContraction("he's");
        checkContraction("how's");
        checkContraction("I'd");
        checkContraction("I'll");
        checkContraction("I'm");
        checkContraction("it'd");
        checkContraction("isn't");
        checkContraction("it'll");
        checkContraction("it's");
        checkContraction("might've");
        checkContraction("mightn't");
        checkContraction("must've");
        checkContraction("mustn't");
        checkContraction("she's");
        checkContraction("she'll");
        checkContraction("she's");
        checkContraction("should've");
        checkContraction("shouldn't");
        checkContraction("we'd");
        checkContraction("we'll");
        checkContraction("we're");
        checkContraction("we've");
        checkContraction("weren't");
        checkContraction("what're");
        checkContraction("what've");
        checkContraction("when's");
        checkContraction("who'll");
        checkContraction("who's");
        checkContraction("won't");
        checkContraction("would've");
        checkContraction("wouldn't");
        checkContraction("you'll");
        checkContraction("you're");
    }

    // check the use of a contraction in a sentence
    @Test
    public void testContractions2() {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokenList = tokenizer.tokenize("You shouldn't have.");
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 6 && tokenList.get(2).getText().equals("shouldn't"));
    }

    @Test
    public void testContractions3() {
        checkContraction("CouLdN'T");
        checkContraction("Didn't");
    }

    // check a contraction is worth just one token in the tokenizer
    private void checkContraction( String contractionStr ) {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokenList = tokenizer.tokenize(contractionStr);
        Assert.notNull(tokenList);
        Assert.isTrue(tokenList.size() == 1 && tokenList.get(0).getText().equals(contractionStr));
    }


}


