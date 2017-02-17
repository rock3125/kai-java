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

package industries.vocht.viki.semantics;

import industries.vocht.viki.semantic_search.VerbsProtoTypes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import java.util.HashSet;

/**
 * Created by peter on 11/06/16.
 *
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test-context.xml"})
public class VerbProtoTypeTest {

    @Autowired
    private VerbsProtoTypes verbsProtoTypes;


    // test the verb proto-type system
    @Test
    public void testVerbProto1() {
        HashSet<String> protoTypeSet = verbsProtoTypes.getVerbProtoType("walked");
        Assert.notNull(protoTypeSet);
        Assert.isTrue(protoTypeSet.size() == 1);
        Assert.isTrue(protoTypeSet.contains("run"));
    }

    @Test
    public void testVerbProto2() {
        HashSet<String> protoTypeSet = verbsProtoTypes.getVerbProtoType("walking");
        Assert.notNull(protoTypeSet);
        Assert.isTrue(protoTypeSet.size() == 1);
        Assert.isTrue(protoTypeSet.contains("run"));
    }


    @Test
    public void testVerbProto3() {
        HashSet<String> protoTypeSet = verbsProtoTypes.getVerbProtoType("walks");
        Assert.notNull(protoTypeSet);
        Assert.isTrue(protoTypeSet.size() == 1);
        Assert.isTrue(protoTypeSet.contains("run"));
    }


    @Test
    public void testVerbProto4() {
        HashSet<String> protoTypeSet = verbsProtoTypes.getVerbProtoType("walk");
        Assert.notNull(protoTypeSet);
        Assert.isTrue(protoTypeSet.size() == 1);
        Assert.isTrue(protoTypeSet.contains("run"));
    }


    @Test
    public void testVerbProto5() {
        HashSet<String> protoTypeSet = verbsProtoTypes.getVerbProtoType("ran");
        Assert.notNull(protoTypeSet);
        Assert.isTrue(protoTypeSet.size() > 0);
        Assert.isTrue(protoTypeSet.contains("run"));
    }


    @Test
    public void testVerbProto6() {
        HashSet<String> protoTypeSet = verbsProtoTypes.getVerbProtoType("run");
        Assert.notNull(protoTypeSet);
        Assert.isTrue(protoTypeSet.size() > 0);
        Assert.isTrue(protoTypeSet.contains("run"));
    }



}
