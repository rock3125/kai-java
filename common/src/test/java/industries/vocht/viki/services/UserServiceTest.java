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

import industries.vocht.viki.model.user.User;
import industries.vocht.viki.model.user.UserList;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

/**
 * Created by peter on 4/03/16.
 *
 */
public class UserServiceTest extends TestBase {

    @Test
    public void testUser1() throws Exception {
        // create 100 users
        for ( int i = 0; i < 100; i++ ) {

            // create some extra users
            String uname = "User" + i;
            String sname = "Two";
            String email = "user.two" + "_" + i + "@org.com";
            String password = "Password2" + i;

            User user = userService.createUser(sessionID, createUser(org1.getId(), uname, sname, email), password, IP_ADDRESS_1);

            Assert.assertNotNull(user);
            Assert.assertEquals(user.getEmail(), email);
            Assert.assertEquals(user.getFirst_name(), uname);
            Assert.assertEquals(user.getSurname(), sname);
            Assert.assertEquals(user.getOrganisation_id(), org1.getId());
            Assert.assertNotNull(user.getId());

            UUID activationID = dao.getUserDao().getAccountActivation(email);
            Assert.assertNotNull(activationID);

            // activate the user
            userService.confirmAccount(email, activationID);

            // login as the new user
            UUID sessionID2 = userService.login(email, password, IP_ADDRESS_1);
            Assert.assertNotNull(sessionID2);
        }

        // get all users
        UserList userList1 = userService.getPaginatedUserList(sessionID, 0, 1000, IP_ADDRESS_1);
        Assert.assertNotNull(userList1);
        Assert.assertEquals(userList1.getOrganisation_id(), org1.getId());
        Assert.assertEquals(userList1.getItems_per_page(), 1000);
        Assert.assertEquals(userList1.getPage(), 0);
        Assert.assertNotNull(userList1.getUser_list());
        Assert.assertEquals(userList1.getUser_list().size(), 101);

        // test user update
        User testUser = userList1.getUser_list().get(50);
        testUser.setFirst_name("Something");
        userService.updateUser(sessionID, testUser, null, IP_ADDRESS_1);

        // get the list again
        userList1 = userService.getPaginatedUserList(sessionID, 0, 1000, IP_ADDRESS_1);
        Assert.assertNotNull(userList1);
        Assert.assertEquals(userList1.getOrganisation_id(), org1.getId());
        Assert.assertEquals(userList1.getItems_per_page(), 1000);
        Assert.assertEquals(userList1.getPage(), 0);
        Assert.assertNotNull(userList1.getUser_list());
        Assert.assertEquals(userList1.getUser_list().size(), 101);

        // test user update
        boolean found = false;
        for ( int i = 0; i < userList1.getUser_list().size(); i++ ) {
            if ( userList1.getUser_list().get(i).getFirst_name().equals("Something") ) {
                found = true;
            }
        }
        Assert.assertEquals(found, true);

        // remove all of these users
        for ( int i = 0; i < 100; i++ ) {
            String email = "user.two" + "_" + i + "@org.com";
            userService.deleteUser(sessionID, org1.getId(), email, IP_ADDRESS_1);
        }

        // check they're gone
        UserList userList2 = userService.getPaginatedUserList(sessionID, 0, 100, IP_ADDRESS_1);
        Assert.assertNotNull(userList2);
        Assert.assertEquals(userList2.getOrganisation_id(), org1.getId());
        Assert.assertEquals(userList2.getItems_per_page(), 100);
        Assert.assertEquals(userList2.getPage(), 0);
        Assert.assertNotNull(userList2.getUser_list());
        Assert.assertEquals(userList2.getUser_list().size(), 1);
    }

    // test the activation process
    @Test
    public void testAccountActivate() throws Exception {
        userService.createUser(sessionID, createUser(org1.getId(), "test", "de Vocht", "test@test.com"), "Password123", IP_ADDRESS_1);
        User user = userService.getUserByEmail("test@test.com", IP_ADDRESS_1);
        Assert.assertNotNull(user);
        Assert.assertEquals(user.isConfirmed(), false);

        UUID activationID = userService.createAccountActivation("test@test.com");
        userService.confirmAccount("test@test.com", activationID);

        User user2 = userService.getUserByEmail("test@test.com", IP_ADDRESS_1);
        Assert.assertNotNull(user2);
        Assert.assertEquals(user2.isConfirmed(), true);
    }

    @Test
    public void testPasswordReset() throws Exception {
        UUID sessionID = userService.login(EMAIL_USER_1, PASSWORD_USER_1, IP_ADDRESS_1);
        User user = userService.getUser(sessionID, IP_ADDRESS_1);
        Assert.assertNotNull(user);
        String password = user.getPassword_sha256();
        UUID passwordResetRequest = userService.resetPasswordRequest(EMAIL_USER_1);
        userService.resetPassword(EMAIL_USER_1, passwordResetRequest, "newPassword");
        User user2 = userService.getUser(sessionID, IP_ADDRESS_1);
        Assert.assertNotNull(user2);
        // login using the new password
        UUID sessionID2 = userService.login(EMAIL_USER_1, "newPassword", IP_ADDRESS_1);
        Assert.assertNotNull(sessionID2);
    }

    // you can't login twice - get different sessions
    @Test
    public void testDoubleLogin() throws Exception {
        userService.createUser(sessionID, createUser(org1.getId(), "test", "de Vocht", "test1@test.com"), "Password123", IP_ADDRESS_1);
        User user = userService.getUserByEmail("test1@test.com", IP_ADDRESS_1);
        Assert.assertNotNull(user);
        Assert.assertEquals(user.isConfirmed(), false);

        UUID activationID = userService.createAccountActivation("test1@test.com");
        userService.confirmAccount("test1@test.com", activationID);

        User user2 = userService.getUserByEmail("test1@test.com", IP_ADDRESS_1);
        Assert.assertNotNull(user2);
        Assert.assertEquals(user2.isConfirmed(), true);

        // login 1
        UUID sessionID1 = userService.login("test1@test.com", "Password123", IP_ADDRESS_1);
        Assert.assertNotNull(sessionID1);

        UUID sessionID2 = userService.login("test1@test.com", "Password123", IP_ADDRESS_1);
        Assert.assertNotNull(sessionID2);

        Assert.assertNotEquals(sessionID2, sessionID1);
    }


}
