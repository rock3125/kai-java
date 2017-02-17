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
import industries.vocht.viki.model.semantics.Tuple;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 9/06/16.
 *
 * case tuple semantic system
 *
 */
public class TupleDao {

    private IDatabase db;

    public TupleDao(IDatabase db) {
        this.db = db;
    }

    // read a set of tuples from the database for a given id list, returns null if none exists
    public List<Tuple> readTuples(UUID organisation_id, List<UUID> idArray) throws IOException {
        return db.readTuples(organisation_id, idArray);
    }

    // read a set of tuples from the database for a given url, returns null if dne
    public List<Tuple> readTuplesForDocument(UUID organisation_id, String url) throws IOException {
        return db.readTuplesForDocument(organisation_id, url);
    }

    // write a set of case tuples to the database
    public void writeTuple( UUID organisation_id, Tuple tuple) throws IOException {
        db.writeTuple(organisation_id, tuple);
    }

}

