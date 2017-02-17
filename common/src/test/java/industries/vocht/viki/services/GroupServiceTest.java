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

import com.fasterxml.jackson.databind.ObjectMapper;
import industries.vocht.viki.model.group.Group;
import industries.vocht.viki.model.group.GroupList;
import org.junit.Test;
import org.springframework.util.Assert;
import java.util.UUID;

/**
 * Created by peter on 13/03/16.
 *
 */
public class GroupServiceTest extends TestBase {

    @Test
    public void testJackson1() throws Exception {
        GroupList groupList = new GroupList();
        groupList.getGroup_list().add( new Group(UUID.randomUUID(), "group1", "Peter", "Mark") );

        ObjectMapper mapper = new ObjectMapper();
        String jsonStr = mapper.writeValueAsString(groupList);

        // and back to a group list object
        GroupList groupList1 = mapper.readValue(jsonStr, GroupList.class);
        Assert.isTrue(groupList1 != null && groupList1.getGroup_list() != null);
        Assert.isTrue(groupList1.getGroup_list().size() == 1);
        Assert.isTrue(groupList1.getGroup_list().get(0).getName().equals(groupList.getGroup_list().get(0).getName()));
        Assert.isTrue(groupList1.getGroup_list().get(0).getOrganisation_id().equals(groupList.getGroup_list().get(0).getOrganisation_id()));
        Assert.isTrue(groupList1.getGroup_list().get(0).getUser_list() != null);
        Assert.isTrue(groupList1.getGroup_list().get(0).getUser_list().contains("Peter"));
        Assert.isTrue(groupList1.getGroup_list().get(0).getUser_list().contains("Mark"));
    }

    @Test
    public void testGroup1() throws Exception {
        // create a new entity
        Group g1 = new Group();
        g1.setName("Peter de Vocht");
        g1.setOrganisation_id( org1.getId() );
        g1.addUser("user 1").addUser("user 2");

        Group g1a = groupService.createGroup(sessionID, g1, IP_ADDRESS_1);
        Assert.notNull(g1a);

        // test it can be re-read
        Group g1b = groupService.getGroup(sessionID, g1a.getName(), IP_ADDRESS_1);
        Assert.notNull(g1b);
        Assert.notNull(g1b.getOrganisation_id());

        Assert.isTrue(g1b.getOrganisation_id().equals(org1.getId()));
        Assert.isTrue(g1b.getName().equals(g1.getName()));
        Assert.isTrue(g1b.getUser_list().size() == g1.getUser_list().size());

        // test delete
        groupService.deleteGroup(sessionID, g1a.getName(), IP_ADDRESS_1);

        // test it can't be re-read
        Group g1c = groupService.getGroup(sessionID, g1a.getName(), IP_ADDRESS_1);
        Assert.isNull(g1c);
    }

    @Test
    public void testGroup2() throws Exception {
        // create 100 entities
        Group g0 = null;
        for ( int i = 0; i < 100; i++ ) {
            // create a new entity
            Group g1 = new Group();
            if ( g0 == null ) {
                g0 = g1;
            }
            g1.setName("group " + i);
            g1.addUser("Peter").addUser("de Vocht").addUser("VOCHT").addUser("Adam " + i);
            g1.setOrganisation_id( org1.getId() );
            Group g1a = groupService.createGroup(sessionID, g1, IP_ADDRESS_1);

            Assert.notNull(g1a);
            Assert.notNull(g1a.getName());
            Assert.notNull(g1a.getOrganisation_id());
        }

        // get them all paginated
        GroupList groupList = groupService.getPaginatedGroupList(sessionID, 0, 1000, IP_ADDRESS_1);
        Assert.notNull(groupList);
        Assert.isTrue(groupList.getGroup_list().size() == 100);
        Assert.isTrue(groupList.getTotal_group_count() == 100);

        for ( int i = 0; i < 100; i++ ) {
            Group g1b = groupList.getGroup_list().get(i);
            Assert.notNull(g1b);
            Assert.notNull(g1b.getName());
            Assert.notNull(g1b.getOrganisation_id());

            Assert.isTrue(g1b.getOrganisation_id().equals(org1.getId()));
            Assert.isTrue(g1b.getUser_list().size() == g0.getUser_list().size());
        }
    }


}
