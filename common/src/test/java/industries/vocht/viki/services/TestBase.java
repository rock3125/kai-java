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

import com.hazelcast.core.IMap;
import industries.vocht.viki.IDao;
import industries.vocht.viki.IHazelcast;
import industries.vocht.viki.model.Organisation;
import industries.vocht.viki.model.user.User;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

/**
 * Created by peter on 4/03/16.
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test-context.xml"})
public class TestBase {

    protected final String NAME_ORG_1 = "First Organisation";
    protected final String EMAIL_ORG_1 = "first@organisation.com";
    protected final String FIRST_NAME_USER_1 = "Peter";
    protected final String SURNAME_USER_1 = "de Vocht";
    protected final String EMAIL_USER_1 = "peter@organisation.com";
    protected final String PASSWORD_USER_1 = "Password1";
    protected final String IP_ADDRESS_1 = "192.168.0.1";
    protected final int ACL = 1;

    @Autowired
    protected IDao dao; // dao for tests

    @Autowired
    protected OrganisationService organisationService;

    @Autowired
    protected UserService userService;

    @Autowired
    protected IHazelcast hazelcast;

    @Autowired
    protected DocumentService documentService;

    @Autowired
    protected GroupService groupService;

    protected UUID sessionID;
    protected Organisation org1;

    /**
     * setup basic data, the in memory database
     * a simple organisation and first user
     * and login to the in memory database
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {
        dao.getOrganisationDao().setUnitTest(); // skip duplicate detection

        // create a fake org
        org1 = createOrg_1();

        // activate this user
        dao.getUserDao().confirmAccount(EMAIL_USER_1);

        // setup security
        IMap<String, HashSet<Integer>> map = hazelcast.getUserAclMap(org1.getId());
        HashSet<Integer> set = new HashSet<>();
        set.add(ACL);
        map.put( EMAIL_USER_1, set);

        IMap<Integer, List<String>> map2 = hazelcast.getHashAclMap(org1.getId());
        List<String> set2 = new ArrayList<>();
        set2.add(EMAIL_USER_1 + ":true");
        map2.put( ACL, set2 );

        // test acl set
        HashSet<Integer> aclSet = hazelcast.getUserAclMap(org1.getId()).get(EMAIL_USER_1);
        Assert.assertNotNull(aclSet);
        Assert.assertTrue(aclSet.size() == 1);

        // login
        sessionID = userService.login(EMAIL_USER_1, PASSWORD_USER_1, IP_ADDRESS_1);
        Assert.assertNotNull(sessionID);
    }

    @Test
    public void testAutowired() {
        Assert.assertNotNull(dao);
    }

    // create org 1
    private Organisation createOrg_1() throws Exception {
        // only if it doesn't exist, create it
        return organisationService.createOrganisation(
                createOrganisation(NAME_ORG_1, EMAIL_ORG_1),
                createOrgUser(FIRST_NAME_USER_1, SURNAME_USER_1, EMAIL_USER_1),
                PASSWORD_USER_1);
    }

    private Organisation createOrganisation(String name, String email) {
        Organisation organisation = new Organisation();
        organisation.setName(name);
        return organisation;
    }

    public User createOrgUser(String firstName, String surname, String email) {
        User user = new User();
        user.setFirst_name(firstName);
        user.setSurname(surname);
        user.setEmail(email);
        return user;
    }

    public User createUser(UUID organisation_id, String firstName, String surname, String email) {
        User user = new User();
        user.setOrganisation_id(organisation_id);
        user.setFirst_name(firstName);
        user.setSurname(surname);
        user.setEmail(email);
        return user;
    }

}
