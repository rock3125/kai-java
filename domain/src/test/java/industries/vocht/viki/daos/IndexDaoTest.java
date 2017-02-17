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
import industries.vocht.viki.model.emotions.EmotionalItem;
import industries.vocht.viki.model.emotions.EmotionalSet;
import industries.vocht.viki.model.indexes.Index;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 22/06/16.
 *
 * test the index system performs as expected
 *
 */
@SuppressWarnings("ConstantConditions")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/domain/test-context.xml"})
public class IndexDaoTest {

    @Autowired
    private IDao dao;

    // test simple create and re-read single items
    @Test
    public void testIndexes1() {
        UUID org = UUID.randomUUID();
        addIndex(org, "url1", 0, 0, "Peter");
        addIndex(org, "url1", 1, 0, "Mark");
        dao.getIndexDao().flushIndexes();

        // re-read
        List<Index> indexList1 = dao.getIndexDao().readIndex(org, "peter", 0, Document.META_BODY);
        Assert.isTrue( indexList1 != null && indexList1.size() == 1 && indexList1.get(0).getUrl().equals("url1") );

        List<Index> indexList2 = dao.getIndexDao().readIndex(org, "MaRk", 0, Document.META_BODY);
        Assert.isTrue( indexList2 != null && indexList2.size() == 1 && indexList2.get(0).getUrl().equals("url1") );
    }


    // test simple create and re-read multiple items
    @Test
    public void testIndexes2() {
        UUID org = UUID.randomUUID();
        addIndex(org, "url2", 0, 0, "Peter");
        addIndex(org, "url2", 1, 0, "PeTeR");
        dao.getIndexDao().flushIndexes();

        // re-read
        List<Index> indexList1 = dao.getIndexDao().readIndex(org, "peter", 0, Document.META_BODY);
        Assert.isTrue( indexList1 != null && indexList1.size() == 2 );
        Assert.isTrue( indexList1.get(0).getUrl().equals("url2") && indexList1.get(0).getShard() == 0);
    }


    // test simple create and delete with multiple indexes gone
    @Test
    public void testIndexes3() {
        UUID org = UUID.randomUUID();
        addIndex(org, "url2", 0, 0, "Peter");
        addIndex(org, "url2", 1, 0, "PeTeR");
        addIndex(org, "url3", 0, 0, "Peter");
        addIndex(org, "url3", 1, 0, "Mark");
        dao.getIndexDao().flushIndexes();

        // re-read
        List<Index> indexList1 = dao.getIndexDao().readIndex(org, "peter", 0, Document.META_BODY);
        Assert.isTrue( indexList1 != null && indexList1.size() == 3 );
        List<Index> indexList2 = dao.getIndexDao().readIndex(org, "mark", 0, Document.META_BODY);
        Assert.isTrue( indexList2 != null && indexList2.size() == 1 );

        // delete
        dao.getIndexDao().removeIndex(org, "url3", Document.META_BODY);

        // check
        List<Index> indexList3 = dao.getIndexDao().readIndex(org, "peter", 0, Document.META_BODY);
        Assert.isTrue( indexList3 != null && indexList3.size() == 2 );
        List<Index> indexList4 = dao.getIndexDao().readIndex(org, "mark", 0, Document.META_BODY);
        Assert.isTrue( indexList4 == null );
    }

    @Test
    public void testIndexEmotions1() {
        UUID org = UUID.randomUUID();
        dao.getIndexDao().indexEmotion(org, "url4", 1, -1.0, 1);
        dao.getIndexDao().indexEmotion(org, "url4", 2, 1.0, 1);
        dao.getIndexDao().indexEmotion(org, "url4", 3, 0.5, 1);

        // test a non-existent set
        EmotionalSet set1 = dao.getIndexDao().getEmotionSet(org, "url5");
        Assert.isTrue( set1 != null && set1.getEmotional_list() != null && set1.getEmotional_list().size() == 0 );

        // re read these emotions
        EmotionalSet set2 = dao.getIndexDao().getEmotionSet(org, "url4");
        Assert.isTrue( set2 != null && set2.getEmotional_list() != null && set2.getEmotional_list().size() == 3 );
        Assert.isTrue( contains(set2, 1, -1.0, 1) );
        Assert.isTrue( contains(set2, 2, 1.0, 1) );
        Assert.isTrue( contains(set2, 3, 0.5, 1) );
    }


    /////////////////////////////////////////////////////////////////////////////
    // helpers

    // simplest of index adds - does not flush
    private void addIndex( UUID org, String url, int offset, int shard, String wordStr ) {
        dao.getIndexDao().addIndex(org, new Index(url, wordStr, shard, null, -1, Document.META_BODY, 1, 0, "NN1", offset));
    }

    // check the emotional set contains a specified emotion
    private boolean contains( EmotionalSet set, int sentence, double value, int acl ) {
        if ( set != null ) {
            for (EmotionalItem item : set.getEmotional_list()) {
                if ( item.getAcl_hash() == acl && item.getSentence_id() == sentence && item.getValue() == value ) {
                    return true;
                }
            }
        }
        return false;
    }

}


