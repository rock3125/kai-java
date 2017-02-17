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

package industries.vocht.viki.data_structures;

import com.carrotsearch.hppc.IntHashSet;
import industries.vocht.viki.datastructures.IntList;
import org.junit.Test;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Created by peter on 5/06/16.
 *
 * test various aspects of the IntList data structure
 *
 */
public class IntListTest {

    @Test
    public void test1() {
        IntList list1 = new IntList();
        for ( int i = 0; i < 1000; i++ ) {
            list1.add(i);
        }
        Assert.isTrue(list1.size() == 1000);
        for ( int i = 0; i < 1000; i++ ) {
            Assert.isTrue( list1.get(i) == i );
        }
    }

    @Test
    public void test2() {
        IntList list1 = new IntList(1,2,3,4,5,6,7);
        Assert.isTrue(list1.size() == 7);
        for ( int i = 0; i < 7; i++ ) {
            Assert.isTrue( list1.get(i) == (i+1) );
        }
    }

    @Test
    public void test3() {
        IntList list1 = new IntList(3,1,5,1,23);
        Assert.isTrue(list1.size() == 5);
        list1.sort();
        Assert.isTrue(list1.get(0) == 1);
        Assert.isTrue(list1.get(1) == 1);
        Assert.isTrue(list1.get(2) == 3);
        Assert.isTrue(list1.get(3) == 5);
        Assert.isTrue(list1.get(4) == 23);
    }

    @Test
    public void test4() {
        IntList list1 = new IntList();
        for ( int i = 0; i < 100; i++ ) {
            list1.add(i);
        }
        Assert.isTrue(list1.size() == 100);
        for ( int i = 0; i < 100; i++ ) {
            Assert.isTrue( list1.get(i) == i );
        }
        int[] data1 = list1.getData();
        Assert.notNull(data1);
        Assert.isTrue(data1.length == 100);
        for ( int i = 0; i < 100; i++ ) {
            Assert.isTrue( data1[i] == i );
        }
    }

    @Test
    public void test5() {
        IntList list1 = new IntList(10);
        for ( int i = 0; i < 100; i++ ) {
            list1.add(i);
        }
        Assert.isTrue(list1.size() == 100);
        list1.reset();
        Assert.isTrue(list1.size() == 0);
        Assert.notNull(list1.getRawData());
        Assert.notNull(list1.getRawData().length == 10);
    }

    @Test
    public void test6() {
        IntList list1 = new IntList(10);
        for ( int i = 0; i < 100; i++ ) {
            list1.add(i / 2);
        }
        Assert.isTrue(list1.size() == 100);
        IntHashSet set = list1.asHashSet();
        Assert.notNull(set);
        Assert.isTrue(set.size() == 50);
    }

    @Test
    public void test7() {
        IntList list1 = new IntList(10);
        for ( int i = 0; i < 100; i++ ) {
            list1.add(i);
        }
        Assert.isTrue(list1.size() == 100);
        IntList list2 = list1.asList();
        Assert.notNull(list2);
        Assert.isTrue(list2.size() == 100);
        for ( int i = 0; i < 100; i++ ) {
            Assert.isTrue( list2.contains(i) );
        }
    }

    @Test
    public void testUnique1() {
        IntList list = new IntList(1,1,2,2,3,4,5,6);
        Assert.isTrue(list.size() == 8);
        IntList newList = list.toUniqueList();
        Assert.isTrue(newList != null && newList.size() == 6);
        Assert.isTrue(newList.contains(1));
        Assert.isTrue(newList.contains(2));
        Assert.isTrue(newList.contains(3));
        Assert.isTrue(newList.contains(4));
        Assert.isTrue(newList.contains(5));
        Assert.isTrue(newList.contains(6));
    }


    @Test
    public void testSort1() {
        IntList list = new IntList(100);
        list.add(6,3,5,4,1,2);
        list.sort();
        Assert.isTrue(list.get(0) == 1);
        Assert.isTrue(list.get(1) == 2);
        Assert.isTrue(list.get(2) == 3);
        Assert.isTrue(list.get(3) == 4);
        Assert.isTrue(list.get(4) == 5);
        Assert.isTrue(list.get(5) == 6);
    }


}
