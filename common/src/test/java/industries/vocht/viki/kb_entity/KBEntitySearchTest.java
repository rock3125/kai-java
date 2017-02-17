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

package industries.vocht.viki.kb_entity;

import industries.vocht.viki.IDao;
import industries.vocht.viki.model.knowledge_base.KBEntry;
import industries.vocht.viki.services.KBService;
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
 * Created by peter on 4/01/17.
 *
 * test the search system
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test-context.xml"})
public class KBEntitySearchTest {

    @Autowired
    private KBService kbService;

    @Autowired
    private IDao dao;


    @Test
    public void testFind1() throws IOException {
        UUID org = UUID.randomUUID();
        KBEntry e1 = createEntity(org, "type1", "origin1", "{\"attr\": \"test data 1\"}");
        dao.getKBDao().saveKBEntry(e1);
        Assert.isTrue( e1.getId() != null );
        kbService.index_entity(e1, 0);

        // test a few different kind of finds
        List<KBEntry> resultList = kbService.findPaginated(org, "attr", "type1", "test data", null, 10);
        Assert.notNull(resultList);
        Assert.isTrue(resultList.size() == 1);
        KBEntry entry = resultList.get(0);
        Assert.notNull(entry);
        Assert.notNull(entry.getId());
        Assert.isTrue( entry.getOrigin().equals(e1.getOrigin()) );
        Assert.isTrue( entry.getType().equals(e1.getType()) );
        Assert.isTrue( entry.getJson_data().equals(e1.getJson_data()) );
    }

    @Test
    public void testFind2() throws IOException {
        UUID org = UUID.randomUUID();
        KBEntry e1 = createEntity(org, "type2", "origin2", "{\"attr\": \"test data 1\"}");
        dao.getKBDao().saveKBEntry(e1);
        Assert.isTrue( e1.getId() != null );
        kbService.index_entity(e1, 0);

        // test a few different kind of finds
        List<KBEntry> resultList = kbService.findPaginated(org, "attr", "type2", "test data 1", null, 10);
        Assert.notNull(resultList);
        Assert.isTrue(resultList.size() == 1);
        KBEntry entry = resultList.get(0);
        Assert.notNull(entry);
        Assert.notNull(entry.getId());
        Assert.isTrue( entry.getOrigin().equals(e1.getOrigin()) );
        Assert.isTrue( entry.getType().equals(e1.getType()) );
        Assert.isTrue( entry.getJson_data().equals(e1.getJson_data()) );
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

