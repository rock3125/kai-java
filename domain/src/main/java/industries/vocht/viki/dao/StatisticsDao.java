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
import industries.vocht.viki.datastructures.IntList;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by peter on 23/04/16.
 *
 * statistics processing / gathering
 *
 */
public class StatisticsDao {

    private IDatabase db;

    public StatisticsDao(IDatabase db) {
        this.db = db;
    }

    /////////////////////////////////////////////////////////////////////////

    public Long hazelcastWordMapLoad(String key) {
        return db.hazelcastWordMapLoad(key);
    }

    public List<String> hazelcastWordMapLoadAllKeys() {
        return db.hazelcastWordMapLoadAllKeys();
    }

    public Map<String, Long> hazelcastWordMapLoadAll(Collection<String> keys) {
        return db.hazelcastWordMapLoadAll(keys);
    }

    public void hazelcastWordMapStore(String key, Long value) {
        db.hazelcastWordMapStore(key, value);
    }

    public void hazelcastWordMapStoreAll(Map<String, Long> map) {
        db.hazelcastWordMapStoreAll(map);
    }

    public void hazelcastWordMapDelete(String key) {
        db.hazelcastWordMapDelete(key);
    }

    public void hazelcastWordMapDeleteAll(Collection<String> keys) {
        db.hazelcastWordMapDeleteAll(keys);
    }

    /////////////////////////////////////////////////////////////////////////

    public List<String> hazelcastAclMapLoad(Integer key) {
        return db.hazelcastAclMapLoad(key);
    }

    public IntList hazelcastAclMapLoadAllKeys() {
        return db.hazelcastAclMapLoadAllKeys();
    }

    public Map<Integer, List<String>> hazelcastAclMapLoadAll(Collection<Integer> keys) {
        return db.hazelcastAclMapLoadAll(keys);
    }

    public void hazelcastAclMapStore(Integer key, List<String> value) {
        db.hazelcastAclMapStore(key, value);
    }

    public void hazelcastAclMapStoreAll(Map<Integer, List<String>> map) {
        db.hazelcastAclMapStoreAll(map);
    }

    public void hazelcastAclMapDelete(Integer key) {
        db.hazelcastAclMapDelete(key);
    }

    public void hazelcastAclMapDeleteAll(Collection<Integer> keys) {
        db.hazelcastAclMapDeleteAll(keys);
    }

    public List<String> getDocumentAnomaliesPaginated( UUID organisation_id, String prevUrl, int pageSize ) {
        return db.getDocumentAnomaliesPaginated(organisation_id, prevUrl, pageSize );
    }

    public void saveDocumentAnomalies( UUID organisation_id, List<String> urlList ) {
        db.saveDocumentAnomalies( organisation_id, urlList );
    }

    //////////////////////////////////////////////////////////////////////////
    // document index counting

    public void setDocumentIndexCount(UUID organisation_id, String url, long index_count) {
        db.setDocumentIndexCount(organisation_id, url, index_count);
    }

    public long getDocumentIndexCount(UUID organisation_id, String url) {
        return db.getDocumentIndexCount(organisation_id, url);
    }

    public void deleteDocumentIndexCount(UUID organisation_id, String url) {
        db.deleteDocumentIndexCount(organisation_id, url);
    }


}


