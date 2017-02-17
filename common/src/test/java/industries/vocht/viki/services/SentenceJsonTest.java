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

import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.model.Token;
import industries.vocht.viki.tokenizer.Tokenizer;
import industries.vocht.viki.utility.BinarySerializer;
import org.junit.Test;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Created by peter on 14/04/16.
 *
 * test the bson serialisation system works
 *
 */
public class SentenceJsonTest {

    // test serialisation
    @Test
    public void testSerialisation1() throws Exception {

        Tokenizer tokenizer = new Tokenizer();
        BinarySerializer serializer = new BinarySerializer(65536);

        // create initial object
        Sentence test1 = createSentence("This is a test sentence to tokenize 1.");
        Assert.notNull(test1);
        Assert.notNull(test1.getTokenList());
        Assert.isTrue(test1.getTokenList().size() > 5 );

        // check the tokenizer gave it the right value
        String str1 = tokenizer.toString(test1.getTokenList());
        Assert.isTrue( str1.equals("This is a test sentence to tokenize 1.") );

        // write to serialiser
        test1.write(serializer);
        byte[] data = serializer.getData();
        Assert.notNull(data);
        Assert.isTrue(data.length > 5);

        // back to object
        Sentence test2 = new Sentence();
        BinarySerializer serializer2 = new BinarySerializer(data);
        test2.read(serializer2);

        // test object
        Assert.notNull(test2);
        Assert.notNull(test2.getTokenList());
        Assert.isTrue(test2.getTokenList().size() > 5 );
        String str2 = tokenizer.toString(test2.getTokenList());
        Assert.notNull(str2);
        Assert.isTrue( str2.equals("This is a test sentence to tokenize 1.") );
    }


    /**
     * tokenize the string text
     * @param text the string to tokenize
     * @return a sentence with the tokenized text
     */
    private Sentence createSentence( String text ) {
        Tokenizer tokenizer = new Tokenizer();
        List<Token> tokenList = tokenizer.tokenize(text);
        return new Sentence( tokenList );
    }


}


