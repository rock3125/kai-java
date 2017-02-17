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

import industries.vocht.viki.document.Document;
import industries.vocht.viki.model.indexes.Index;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 6/03/16.
 *
 */
public class CommonIndexDaoTest extends TestBase {

    @Test
    public void testIndexes1() {
        // write
        UUID organisation_id = org1.getId();
        for ( int i = 0; i < 1000; i++ ) {
            Index index = new Index("http://url1", "word" + i, 0, null, -1, Document.META_BODY, i, 0, null, i);
            dao.getIndexDao().addIndex(organisation_id, index);
        }
        dao.getIndexDao().flushIndexes();

        // re-read and check
        for ( int i = 0; i < 1000; i++ ) {
            List<Index> indexList = dao.getIndexDao().readIndex(organisation_id, "word" + i, 0, Document.META_BODY);
            Assert.assertNotNull(indexList);
            Assert.assertEquals(indexList.size(), 1);
            Assert.assertEquals(indexList.get(0).getOffset(), i);
            Assert.assertEquals(indexList.get(0).getMeta_data(), Document.META_BODY);
            Assert.assertEquals(indexList.get(0).getUrl(), "http://url1");
            Assert.assertEquals(indexList.get(0).getWord_origin(), "");
            Assert.assertEquals(indexList.get(0).getAcl_hash(), i);
        }
    }

    @Test
    public void testIndexes2() {
        // write
        UUID organisation_id = org1.getId();
        for ( int i = 0; i < 1000; i++ ) {
            Index index = new Index("http://url" + (i%100), "word" + (i%100), 0, "", -1, Document.META_BODY, i, 0, null, i);
            dao.getIndexDao().addIndex(organisation_id, index);
        }
        dao.getIndexDao().flushIndexes();

        // re-read and check
        for ( int i = 0; i < 1000; i++ ) {
            List<Index> indexList = dao.getIndexDao().readIndex(organisation_id, "word" + (i%100), 0, Document.META_BODY);
            Assert.assertNotNull(indexList);
            Assert.assertEquals(indexList.size(), 10);
            for ( int j = 0; j < 10; j++ ) {
                Assert.assertEquals(indexList.get(j).getMeta_data(), Document.META_BODY);
                Assert.assertEquals(indexList.get(j).getUrl(), "http://url" + (i%100));
                Assert.assertEquals(indexList.get(j).getWord_origin(), "");
            }
        }
    }



}
