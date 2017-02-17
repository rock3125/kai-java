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

package industries.vocht.viki.parser;

import industries.vocht.viki.model.Sentence;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 23/06/16.
 *
 * check the nlparser operates in parallel with a larger file
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test-context.xml"})
public class PosParallelTest {

    @Autowired
    private NLParser parser;

    @Test
    public void testLargeFile1() throws InterruptedException, IOException {
        String text = loadTextFromResource("/common/mr_spaceship.txt");
        Assert.notNull(text);

        long start = System.currentTimeMillis();
        List<Sentence> sentenceList = parser.parseText(text);
        long dt = System.currentTimeMillis() - start;

        long tokens = 0;
        for ( Sentence sentence : sentenceList ) {
            tokens += sentence.getTokenList().size();
        }

        double tps = ((double)tokens / (double)dt) * 1000.0;
        System.out.println("speed: " + tokens + " in " + dt + "ms (~ " + tps + " tokens per second)");

        Assert.notNull(sentenceList);
        Assert.isTrue(sentenceList.size() > 100);
    }

    // test the largest data-set to view any quirks and test the treading of the parser pos system
    @Ignore
    @Test
    public void testLargeFile2() throws InterruptedException, IOException {

        String trainingSetFilename = "/media/peter/data2/dev/research/datasets/large-text.txt";

        try ( BufferedReader br = new BufferedReader(new FileReader(trainingSetFilename) ) ) {
            int articleCount = 0;
            long start = System.currentTimeMillis();
            List<Sentence> sentenceList = new ArrayList<>();
            for (String line; (line = br.readLine()) != null && articleCount < 100;) {
                List<Sentence> temp = parser.parseText(line);
                if ( temp != null ) {
                    sentenceList.addAll(temp);
                }
                articleCount = articleCount + 1;
            }
            long dt = System.currentTimeMillis() - start;

            long tokens = 0;
            for ( Sentence sentence : sentenceList ) {
                tokens += sentence.getTokenList().size();
            }

            double tps = ((double)tokens / (double)dt) * 1000.0;
            System.out.println("speed: " + tokens + " in " + dt + "ms (~ " + tps + " tokens per second)");
        }
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
            out.append(line);
        }
        return out.toString();
    }

}


