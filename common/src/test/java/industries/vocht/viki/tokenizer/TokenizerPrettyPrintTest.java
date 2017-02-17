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
import industries.vocht.viki.tokenizer.Tokenizer;
import org.junit.Test;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Created by peter on 19/04/16.
 *
 * test the pretty print of the tokenizer to get nice looking text out
 *
 */
public class TokenizerPrettyPrintTest {

    @Test
    public void testPrettyPrint1() {
        testTextEqualsAfterTokenize("Peter's best efforts");
    }

    @Test
    public void testPrettyPrint2() {
        testTextEqualsAfterTokenize("low, fellas, is relative to");
    }

    @Test
    public void testPrettyPrint3() {
        testTextEqualsAfterTokenize("this \"text\".");
    }

    @Test
    public void testPrettyPrint4() {
        testTextEqualsAfterTokenize("this \"text\" then.");
    }

    @Test
    public void testPrettyPrint5() {
        testTextEqualsAfterTokenize("So, this is some test \"text\" to see if it all; works!");
    }

    ///////////////////////////////////////////////////////////////////////

    /**
     * test the text can be tokenized and come back as the original text
     * @param text the text to process
     */
    private void testTextEqualsAfterTokenize( String text ) {
        String processedText = tokenizeText(text);
        Assert.isTrue( processedText.equals(text) );
    }

    /**
     * tokenize text back and forth
     * @param text the text to tokenize
     * @return the pretty printed version of the tokenized text
     */
    private String tokenizeText( String text ) {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokenList = tokenizer.filterOutSpaces(tokenizer.tokenize(text));
        return tokenizer.toString(tokenList);
    }


}


