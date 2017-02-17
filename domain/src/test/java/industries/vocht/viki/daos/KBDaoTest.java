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
import industries.vocht.viki.model.knowledge_base.KBEntry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by peter on 4/01/17.
 *
 * test the knowledge base system
 *
 */

@SuppressWarnings("ConstantConditions")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/domain/test-context.xml"})
public class KBDaoTest {

    @Autowired
    private IDao dao;

    @Test
    public void testCrud1() throws IOException {
        UUID org = UUID.randomUUID();
        KBEntry e1 = createEntity(org, "type1", "origin1", "{\"attr\": \"test data 1\"}");
        dao.getKBDao().saveKBEntry(e1);
        Assert.isTrue( e1.getId() != null );

        KBEntry e3 = dao.getKBDao().getKBEntry(org, "type1", e1.getId());
        Assert.notNull(e3);
        Assert.isTrue( e3.getId() != null );
        Assert.isTrue( e3.getOrigin().equals(e1.getOrigin()) );
        Assert.isTrue( e3.getType().equals(e1.getType()) );
        Assert.isTrue( e3.getJson_data().equals(e1.getJson_data()) );
    }


    @Test
    public void testCrud2() throws IOException {
        UUID org = UUID.randomUUID();
        KBEntry e1 = createEntity(org, "type2", "origin2", "{\"attr\": \"test data 2\"}");
        dao.getKBDao().saveKBEntry(e1);
        Assert.isTrue( e1.getId() != null );

        // delete it
        dao.getKBDao().deleteKBEntry(org, "type2", e1.getId());

        // make sure its gone
        KBEntry entry = dao.getKBDao().getKBEntry(org, "type2", e1.getId());
        Assert.isTrue(entry == null);
    }


    //////////////////////////////////////////////////////////////////////////////////////////
    // helper functions

    private KBEntry createEntity(UUID organisation_id, String type, String origin, String json_data ) {
        KBEntry entity = new KBEntry();
        entity.setOrganisation_id(organisation_id);
        entity.setOrigin(origin);
        entity.setType(type);
        entity.setJson_data(json_data);
        return entity;
    }

}
