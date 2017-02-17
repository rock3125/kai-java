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

import industries.vocht.viki.model.Organisation;
import industries.vocht.viki.model.user.User;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by peter on 4/03/16.
 *
 */
public class OrganisationServiceTest extends TestBase {

    @Test
    public void testCreateOrganisation() throws Exception {
        // check we can get organisation 1
        Organisation organisation = organisationService.getOrganisationByName(NAME_ORG_1);
        Assert.assertNotNull(organisation);
        Assert.assertNotNull(organisation.getId());
        Assert.assertNotNull(organisation.getPrimary_user());
        Assert.assertEquals(organisation.getName(), NAME_ORG_1);

        // check the user object of org 1
        User user = userService.getUser(sessionID, IP_ADDRESS_1);
        Assert.assertNotNull(user);
        Assert.assertEquals(user.getEmail(), EMAIL_USER_1);
        Assert.assertEquals(user.getFirst_name(), FIRST_NAME_USER_1);
        Assert.assertEquals(user.getSurname(), SURNAME_USER_1);
        Assert.assertEquals(user.getOrganisation_id(), organisation.getId());
        Assert.assertNotNull(user.getId());

        // the first user is the primary user
        Assert.assertEquals(organisation.getPrimary_user(), user.getId());

        // security sensitive - never passed down
        Assert.assertNull(user.getPassword_sha256());
        Assert.assertNull(user.getSalt());

        // update the organisation
        organisationService.updateOrganisation(sessionID, org1, IP_ADDRESS_1);

        // test it "set"
        Organisation org1Update = organisationService.getOrganisationByName(NAME_ORG_1);
        Assert.assertNotNull(org1Update);
        Assert.assertEquals(org1Update.getName(), NAME_ORG_1);
        Assert.assertEquals(org1Update.getPrimary_user(), user.getId());
    }


}
