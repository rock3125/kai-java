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

package industries.vocht.viki.dao;

import industries.vocht.viki.IDatabase;
import industries.vocht.viki.model.group.Group;

import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 4/03/16.
 *
 */
public class GroupDao {

    private IDatabase db;

    public GroupDao(IDatabase db) {
        this.db = db;
    }

    public Group create(UUID organisation_id, Group group) {
        return db.createGroup(organisation_id, group);
    }

    public Group read(UUID organisation_id, String name) {
        return db.readGroup(organisation_id, name);
    }

    public void update(UUID organisation_id, Group group) {
        db.updateGroup(organisation_id, group);
    }

    public void delete(UUID organisation_id, String name) {
        db.deleteGroup(organisation_id, name);
    }

    public List<Group> readAllGroups(UUID organisation_id) {
        return db.readAllGroups(organisation_id);
    }

}
