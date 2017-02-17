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

package industries.vocht.viki.relationships_stemming;

import industries.vocht.viki.lexicon.Lexicon;
import industries.vocht.viki.lexicon.Undesirables;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

/**
 * Created by peter on 15/04/16.
 *
 * test a few aspects of the lexicon
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test-context.xml"})
public class LexiconTest {

    @Autowired
    private Lexicon lexicon;

    @Autowired
    private Undesirables undesirables;

    @Before
    public void setup() {
        Assert.notNull(lexicon);
        Assert.notNull(undesirables);
    }

    // test basic stemming is operational
    @Test
    public void testStemming1() {
        Assert.isTrue( lexicon.getStem("gasses").equals("gas") );
        Assert.isTrue( lexicon.getStem("swimming").equals("swim") );
        Assert.isTrue( lexicon.getStem("wrote").equals("write") );
    }

    // test inverse stemming works (from stem -> list of others)
    @Test
    public void testStemming2() {
        List<String> stringList = lexicon.getStemmedList("gas");
        Assert.notNull(stringList);
        Assert.isTrue(stringList.contains("gasses"));
        Assert.isTrue(stringList.contains("gassed"));
        Assert.isTrue(stringList.contains("gassing"));


        List<String> stringList2 = lexicon.getStemmedList("walk");
        Assert.notNull(stringList2);
        Assert.isTrue(stringList2.contains("walked"));
        Assert.isTrue(stringList2.contains("walking"));
        Assert.isTrue(stringList2.contains("walks"));
    }


    // test the undesirables work
    @Test
    public void testUndesirables1() {
        Assert.isTrue( undesirables.isUndesirable("the") );
        Assert.isTrue( undesirables.isUndesirable("a") );
        Assert.isTrue( undesirables.isUndesirable("an") );
        Assert.isTrue( undesirables.isUndesirable("in") );
        Assert.isTrue( undesirables.isUndesirable("my") );
    }


    @Test
    public void testRelationships1() {
        List<String> synonymList = lexicon.getSynonymList("AGA");
        Assert.notNull(synonymList);
        Assert.isTrue( synonymList.size() == 1 );
        Assert.isTrue( synonymList.contains("agha") );

        List<String> synonymList2 = lexicon.getSynonymList("gas");
        Assert.notNull(synonymList2);
        Assert.isTrue( synonymList2.size() > 0 );
        Assert.isTrue( synonymList2.contains("accelerator") );
        Assert.isTrue( synonymList2.contains("air-tight") );
    }

}

