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

package industries.vocht.viki.srl_parser;

import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.model.semantics.Tuple;
import industries.vocht.viki.parser.NLParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 23/06/16.
 *
 * test the chunker runs in parallel
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test-context.xml"})
public class SRLParallelTest {

    @Autowired
    private NLParser parser;

    @Test
    public void testLargeFile1() throws InterruptedException, IOException {
        String text = loadTextFromResource("/common/mr_spaceship.txt");
        Assert.notNull(text);
        List<Sentence> sentenceList = parser.parseText(text);
        Assert.isTrue(sentenceList != null && sentenceList.size() > 1);

        List<Tuple> tupleList = new ArrayList<>();
        for ( Sentence sentence : sentenceList ) {
            if ( sentence.getTuple() != null ) {
                tupleList.add(sentence.getTuple());
            }
        }
        Assert.notNull(tupleList);

        int numLines = 0;
        int counter = 0;
        for ( Tuple tupleList1 : tupleList ) {
            numLines = numLines + 1;
            counter = counter + tupleList1.getRoot().retrieveAllTokens().size();
        }
        Assert.isTrue(counter == 9246 && numLines == 983);
    }

    ///////////////////////////////////////////////////////////////////////////////////////

    // helper - load text file from resource
    private String loadTextFromResource(String resourcePath) throws IOException {
        InputStream in = getClass().getResourceAsStream(resourcePath);
        Assert.notNull(in);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line).append("\n");
        }
        return out.toString();
    }

}


