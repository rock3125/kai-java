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

package industries.vocht.viki.daos;

import industries.vocht.viki.IDao;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.model.indexes.Index;
import industries.vocht.viki.model.indexes.TimeIndex;
import industries.vocht.viki.model.indexes.TimeSelectorSetWithBoundaries;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 22/06/16.
 *
 * test the time indexes part of index dao
 *
 */
@SuppressWarnings("ConstantConditions")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/domain/test-context.xml"})
public class IndexDaoTimeTest {

    @Autowired
    private IDao dao;

    // test simple create and re-read single items for months
    @Test
    public void testTimeIndexes1() {
        UUID org = UUID.randomUUID();
        addIndex(org, "url1", new DateTime(2016,6,26,13,0), 0, 1);
        addIndex(org, "url1", new DateTime(2016,5,26,13,0), 0, 1);

        // re-read
        List<TimeIndex> list1 = dao.getIndexDao().readTimeIndexes(org, 2016, 5);
        Assert.isTrue( list1 != null && contains(list1, "url1", new DateTime(2016,5,26,13,0), 0, 1) );
        List<TimeIndex> list2 = dao.getIndexDao().readTimeIndexes(org, 2016, 6);
        Assert.isTrue( list2 != null && contains(list2, "url1", new DateTime(2016,6,26,13,0), 0, 1) );

        // read non existent
        List<TimeIndex> list3 = dao.getIndexDao().readTimeIndexes(org, 2016, 4);
        Assert.isTrue( list3 == null );
    }

    // test simple create and re-read single items for months
    @Test
    public void testTimeIndexes2() {
        UUID org = UUID.randomUUID();
        addIndex(org, "url3", new DateTime(2016,6,26,13,0), 0, 1);
        addIndex(org, "url3", new DateTime(2016,6,27,15,0), 1, 2);

        // re-read
        List<TimeIndex> list2 = dao.getIndexDao().readTimeIndexes(org, 2016, 6);
        Assert.isTrue( list2 != null && contains(list2, "url3", new DateTime(2016,6,26,13,0), 0, 1) );
        Assert.isTrue( list2 != null && contains(list2, "url3", new DateTime(2016,6,27,15,0), 1, 2) );
    }

    // test simple create and re-read single items for days
    @Test
    public void testTimeIndexes3() {
        UUID org = UUID.randomUUID();
        addIndex(org, "url4", new DateTime(2015,6,26,13,10), 0, 1);
        addIndex(org, "url4", new DateTime(2015,5,26,13,10), 0, 1);

        // re-read
        List<TimeIndex> list1 = dao.getIndexDao().readTimeIndexes(org, 2015, 5, 26);
        Assert.isTrue( list1 != null && contains(list1, "url4", new DateTime(2015,5,26,13,10), 0, 1) );
        List<TimeIndex> list2 = dao.getIndexDao().readTimeIndexes(org, 2015, 6, 26);
        Assert.isTrue( list2 != null && contains(list2, "url4", new DateTime(2015,6,26,13,10), 0, 1) );

        // read non existing
        List<TimeIndex> list3 = dao.getIndexDao().readTimeIndexes(org, 2015, 6, 27);
        Assert.isTrue( list3 == null );
    }

    // test simple create and re-read single items for hours
    @Test
    public void testTimeIndexes4() {
        UUID org = UUID.randomUUID();
        addIndex(org, "url5", new DateTime(2015,6,26,13,10), 0, 1);
        addIndex(org, "url5", new DateTime(2015,6,26,14,25), 0, 1);

        // re-read
        List<TimeIndex> list1 = dao.getIndexDao().readTimeIndexes(org, 2015, 6, 26, 13);
        Assert.isTrue( list1 != null && contains(list1, "url5", new DateTime(2015,6,26,13,10), 0, 1) );
        List<TimeIndex> list2 = dao.getIndexDao().readTimeIndexes(org, 2015, 6, 26, 14);
        Assert.isTrue( list2 != null && contains(list2, "url5", new DateTime(2015,6,26,14,25), 0, 1) );

        // read non existing
        List<TimeIndex> list3 = dao.getIndexDao().readTimeIndexes(org, 2015, 6, 27, 12);
        Assert.isTrue( list3 == null );

        List<TimeIndex> list4 = dao.getIndexDao().readTimeIndexes(org, 2015, 6, 27, 15);
        Assert.isTrue( list4 == null );
    }


    @Test
    public void testTimeIndexesBeforeAndAfter() {
        UUID org = UUID.randomUUID();
        addIndex(org, "url6", new DateTime(2015,6,26,13,10), 0, 1);
        addIndex(org, "url6", new DateTime(2015,6,26,14,25), 1, 1);
        addIndex(org, "url6", new DateTime(2015,6,26,15,10), 2, 1);
        addIndex(org, "url6", new DateTime(2015,6,26,16,0), 3, 1);

        // read before
        TimeSelectorSetWithBoundaries ts1 = dao.getIndexDao().getTimeSelectorsBefore(2015, 6, 26, 13, 11, 10);
        List<TimeIndex> list1 = dao.getIndexDao().getIndexListForRange( org, ts1);
        Assert.isTrue( list1 != null && list1.size() == 1 && contains(list1, "url6", new DateTime(2015,6,26,13,10), 0, 1) );

        // read after
        TimeSelectorSetWithBoundaries ts2 = dao.getIndexDao().getTimeSelectorsAfter(2015, 6, 26, 13, 11, 10);
        List<TimeIndex> list2 = dao.getIndexDao().getIndexListForRange( org, ts2);
        Assert.isTrue( list2 != null && list2.size() == 3 );
        Assert.isTrue( contains(list2, "url6", new DateTime(2015,6,26,14,25), 1, 1) );
        Assert.isTrue( contains(list2, "url6", new DateTime(2015,6,26,15,10), 2, 1) );
        Assert.isTrue( contains(list2, "url6", new DateTime(2015,6,26,16,0), 3, 1) );
    }


    @Test
    public void testTimeIndexesBetween() {
        UUID org = UUID.randomUUID();
        addIndex(org, "url7", new DateTime(2015,6,26,13,10), 0, 1);
        addIndex(org, "url7", new DateTime(2015,6,26,14,25), 1, 1);
        addIndex(org, "url7", new DateTime(2015,6,26,15,10), 2, 1);
        addIndex(org, "url7", new DateTime(2015,6,26,16,0), 3, 1);

        // read before
        TimeSelectorSetWithBoundaries ts1 = dao.getIndexDao().getTimeSelectorsForRange(2015,6,26,13,10, 2015,6,26,15,10);
        List<TimeIndex> list1 = dao.getIndexDao().getIndexListForRange( org, ts1);
        Assert.isTrue( list1 != null && list1.size() == 3 );
        Assert.isTrue( contains(list1, "url7", new DateTime(2015,6,26,13,10), 0, 1) );
        Assert.isTrue( contains(list1, "url7", new DateTime(2015,6,26,14,25), 1, 1) );
        Assert.isTrue( contains(list1, "url7", new DateTime(2015,6,26,15,10), 2, 1) );
    }


    /////////////////////////////////////////////////////////////////////////////
    // helpers

    // add a time index to the system
    private void addIndex( UUID org, String url, DateTime dateTime, int offset, int acl ) {
        List<TimeIndex> timeIndexList = new ArrayList<>();
        timeIndexList.add( new TimeIndex(url, offset, dateTime.toDate().getTime(), acl) );
        dao.getIndexDao().addTimeIndexes(org, timeIndexList);
    }

    // check a list of time indexes contains an exact value
    private boolean contains( List<TimeIndex> list, String url, DateTime dateTime, int offset, int acl ) {
        if ( list != null && url != null ) {
            for ( TimeIndex index : list ) {
                if ( index.getUrl().equals(url) && index.getAcl_hash() == acl && index.getOffset() == offset ) {
                    if ( index.getDate_time() == dateTime.toDate().getTime() ) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


}

