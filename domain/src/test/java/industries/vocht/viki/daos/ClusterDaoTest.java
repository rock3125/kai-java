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
import industries.vocht.viki.model.UrlValue;
import industries.vocht.viki.model.k_means.kMeansCluster;
import industries.vocht.viki.model.k_means.kMeansValue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 20/06/16.
 *
 * test cluster dao
 *
 */
@SuppressWarnings("ConstantConditions")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/domain/test-context.xml"})
public class ClusterDaoTest {

    @Autowired
    private IDao dao;


    // save a cluster, and re-load it
    @Test
    public void testCrud1() throws IOException {
        UUID org = UUID.randomUUID();
        kMeansCluster cluster1 = create(1, "item 1", "item 2", "item 3");
        dao.getClusterDao().saveCluster(org, cluster1.getCluster_id(), cluster1);

        kMeansCluster cluster2 = dao.getClusterDao().loadFullClusterItem(org, 1);
        Assert.notNull( cluster2 );
        Assert.isTrue( cluster2.getCluster_id() == 1 && cluster2.getClusterDescription().contains("item 2") );

        kMeansCluster cluster3 = dao.getClusterDao().loadSummaryClusterItem(org, 1);
        Assert.notNull( cluster3 );
        Assert.isTrue( cluster3.getCluster_id() == 1 && cluster3.getClusterDescription().contains("item 2") );
    }


    // test clustering timestamp dates
    @Test
    public void testTimestamp1() throws IOException {
        UUID org = UUID.randomUUID();
        Assert.isTrue( dao.getClusterDao().getClusterLastChange(org) == 0L ); // initial value
        dao.getClusterDao().setClusterLastChange(org, 1234L);
        Assert.isTrue( dao.getClusterDao().getClusterLastChange(org) == 1234L );
    }


    // test cluster cosine timestamp dates
    @Test
    public void testTimestamp2() throws IOException {
        UUID org = UUID.randomUUID();
        Assert.isTrue( dao.getClusterDao().getCosineLastChange(org) == 0L ); // initial value
        dao.getClusterDao().setCosineLastChange(org, 12346L);
        Assert.isTrue( dao.getClusterDao().getCosineLastChange(org) == 12346L );
    }


    // test cluster last change timestamp dates
    @Test
    public void testTimestamp3() throws IOException {
        UUID org = UUID.randomUUID();
        Assert.isTrue( dao.getClusterDao().getClusterLastChange(org) == 0L ); // initial value
        dao.getClusterDao().setClusterLastClustered(org, 12345L);
        Assert.isTrue( dao.getClusterDao().getClusterLastClustered(org) == 12345L );
    }


    @Test
    public void testDocumentEmotion1() throws IOException {
        UUID org = UUID.randomUUID();
        dao.getClusterDao().setDocumentEmotion(org, "url://test1", 1, 1.0, 2, -1.0);
        List<UrlValue> urlList = dao.getClusterDao().getDocumentEmotion(org, true, 0, 10);
        Assert.isTrue(urlList != null && urlList.size() == 2);
        Assert.isTrue(urlList.get(0).getSentence_id() == 1 && urlList.get(0).getUrl().equals("url://test1") && urlList.get(0).getValue() == 1.0);
        Assert.isTrue(urlList.get(1).getSentence_id() == 2 && urlList.get(0).getUrl().equals("url://test1") && urlList.get(1).getValue() == -1.0);
    }


    @Test
    public void testDocumentEmotion2() throws IOException {
        UUID org = UUID.randomUUID();
        dao.getClusterDao().setDocumentEmotion(org, "url://test2", 1, 1.0, 2, -1.0);
        List<UrlValue> urlList = dao.getClusterDao().getDocumentEmotion(org, false, 0, 10);
        Assert.isTrue(urlList != null && urlList.size() == 2);
        Assert.isTrue(urlList.get(0).getSentence_id() == 2 && urlList.get(0).getUrl().equals("url://test2") && urlList.get(0).getValue() == -1.0);
        Assert.isTrue(urlList.get(1).getSentence_id() == 1 && urlList.get(0).getUrl().equals("url://test2") && urlList.get(1).getValue() == 1.0);
    }



    ///////////////////////////////////////////////////////////////////////////////////////
    // helpers

    private kMeansCluster create(int id, String... descriptionArray) {
        kMeansCluster cluster = new kMeansCluster();
        cluster.setCluster_id(id);
        List<kMeansValue> list = new ArrayList<>();
        list.add( new kMeansValue("url://1", 1.0, 0, 0) );
        cluster.setClusterContents(list);
        List<String> descriptionList = new ArrayList<>();
        descriptionList.addAll(Arrays.asList(descriptionArray));
        cluster.setClusterDescription(descriptionList);
        return cluster;
    }


}


