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
import industries.vocht.viki.model.group.Group;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 21/06/16.
 *
 * test the group dao system
 *
 */
@SuppressWarnings("ConstantConditions")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/domain/test-context.xml"})
public class GroupDaoTest {

    @Autowired
    private IDao dao;

    // create a group and re-read it
    @Test
    public void testCrud1() throws IOException {
        UUID org = UUID.randomUUID();
        Group g1 = createGroup(org, "g1");
        Group g2 = dao.getGroupDao().create(org, g1);
        Assert.isTrue( g2 != null );

        Group g3 = dao.getGroupDao().read(org, "g1");
        Assert.isTrue( g3 != null );
        Assert.isTrue( g3.getName().equals(g1.getName()) );
        Assert.isTrue( g3.getUser_list() != null && g3.getUser_list().contains("peter@peter.co.nz") );
    }


    // create a group update it, check it
    @Test
    public void testCrud2() throws IOException {
        UUID org = UUID.randomUUID();
        Group g1 = createGroup(org, "g1");
        Group g2 = dao.getGroupDao().create(org, g1);
        Assert.isTrue( g2 != null );

        Group g3 = dao.getGroupDao().read(org, "g1");
        Assert.isTrue( g3 != null );
        Assert.isTrue( g3.getName().equals(g1.getName()) );
        Assert.isTrue( g3.getUser_list() != null && g3.getUser_list().contains("peter@peter.co.nz") );

        g3.setName("g2");
        dao.getGroupDao().update(org, g3);

        Group g4 = dao.getGroupDao().read(org, "g2");
        Assert.isTrue( g4 != null );
        Assert.isTrue( g4.getName().equals("g2") );
        Assert.isTrue( g4.getUser_list() != null && g3.getUser_list().contains("peter@peter.co.nz") );
    }


    // create a group, delete it, check it is gone
    @Test
    public void testCrud3() throws IOException {
        UUID org = UUID.randomUUID();
        Group g1 = createGroup(org, "g12");
        Group g2 = dao.getGroupDao().create(org, g1);
        Assert.isTrue( g2 != null );

        Group g3 = dao.getGroupDao().read(org, "g12");
        Assert.isTrue( g3 != null );
        Assert.isTrue( g3.getName().equals(g1.getName()) );
        Assert.isTrue( g3.getUser_list() != null && g3.getUser_list().contains("peter@peter.co.nz") );

        dao.getGroupDao().delete(org, "g12");

        Group g4 = dao.getGroupDao().read(org, "g12");
        Assert.isTrue( g4 == null );
    }


    // create a set of groups, read them paginated
    @Test
    public void testCrud4() throws IOException {
        UUID org = UUID.randomUUID();
        for ( int i = 0; i < 100; i++ ) {
            Group g1 = createGroup(org, "g12" + i);
            Group g2 = dao.getGroupDao().create(org, g1);
            Assert.isTrue(g2 != null);
        }

        List<Group> groupList = dao.getGroupDao().readAllGroups(org);
        Assert.isTrue(groupList != null && groupList.size() == 100);

    }


    //////////////////////////////////////////////////////////////////////////////////////////
    // helper functions

    private Group createGroup(UUID organisation_id, String groupName) {
        Group group = new Group();
        group.setOrganisation_id(organisation_id);
        group.setName(groupName);
        group.getUser_list().add("peter@peter.co.nz");
        return group;
    }


}

