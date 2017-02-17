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

package industries.vocht.viki.hazelcast;

import com.hazelcast.core.MapStore;
import industries.vocht.viki.IDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by peter on 9/05/16.
 *
 */
@Component
public class HazelcastHashAclMapStore implements MapStore<Integer, List<String>>  {

    @Autowired
    private IDao dao;

    public HazelcastHashAclMapStore() {
    }

    @Override
    public List<String> load(Integer key) {
        return dao.getStatisticsDao().hazelcastAclMapLoad(key);
    }

    @Override
    public Map<Integer, List<String>> loadAll(Collection<Integer> keys) {
        return dao.getStatisticsDao().hazelcastAclMapLoadAll(keys);
    }

    @Override
    public Iterable<Integer> loadAllKeys() {
        return dao.getStatisticsDao().hazelcastAclMapLoadAllKeys();
    }


    @Override
    public void store(Integer key, List<String> value) {
        dao.getStatisticsDao().hazelcastAclMapStore(key, value);
    }

    @Override
    public void storeAll(Map<Integer, List<String>> map) {
        dao.getStatisticsDao().hazelcastAclMapStoreAll(map);
    }

    @Override
    public void delete(Integer key) {
        dao.getStatisticsDao().hazelcastAclMapDelete(key);
    }

    @Override
    public void deleteAll(Collection<Integer> keys) {
        dao.getStatisticsDao().hazelcastAclMapDeleteAll(keys);
    }

}

