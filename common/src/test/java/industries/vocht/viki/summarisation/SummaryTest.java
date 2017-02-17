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

package industries.vocht.viki.summarisation;

import industries.vocht.viki.model.Sentence;
import industries.vocht.viki.parser.NLParser;
import industries.vocht.viki.summarize.Summarize;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.List;

/**
 * Created by peter on 13/04/16.
 *
 * fixtures for testing summarisation system
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test-context.xml"})
public class SummaryTest {

    @Autowired
    private NLParser parser;

    @Test
    public void testSummary1() throws IOException, InterruptedException {

        String plainText = "Rabbit island.  Daytimes we paddled all over the island in the canoe. It was mighty cool and shady in the deep woods. " +
                "Even if the sun was blazing outside.  We went winding in and out amongst the trees. " +
                "Sometimes the vines hung so thick we had to back away and go some other way.  " +
                "Well, on every old broken-down tree you could see rabbits and snakes and such things. " +
                "When the island had been overflowed a day or two they got so tame.  On account of being hungry.  " +
                "That you could paddle right up and put your hand on them if you wanted to. " +
                "But not the snakes and turtles—they would slide off in the water.  " +
                "The ridge our cavern was in was full of them. We could a had pets enough if we'd wanted them.";

        String summaryText = "well, on every old broken - down tree you could see rabbits and snakes and such things.";
        summariseText( plainText, summaryText );
    }

    @Test
    public void testSummary2() throws IOException, InterruptedException {
        String plainText = "That night.  One night we catched a little section of a lumber raft—nice pine planks. It was twelve foot wide and about fifteen or sixteen foot long.  The top stood above water six or seven inches—a solid, level floor.  We could see saw-logs go by in the daylight sometimes. We let them go.  We didn't show ourselves in daylight.";
        String summaryText = "One night we catched a little section of a lumber raft - nice pine planks.";
        summariseText( plainText, summaryText );
    }


    private void summariseText( String plainText, String summaryText ) throws IOException, InterruptedException {
        // parse the text
        List<Sentence> sentenceList = parser.parseText(plainText);
        Summarize summarize = new Summarize(sentenceList, null);
        List<Sentence> sentenceList2 = summarize.calculateTopX(10);
        Assert.notNull(sentenceList2);
        Assert.isTrue( sentenceList2.size() > 0 );
        String str2 = sentenceList2.get(0).toString();
        Assert.isTrue( str2.compareToIgnoreCase(summaryText) == 0 );
    }

}


