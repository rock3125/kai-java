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

import java.util.*;

/**
 * Created by peter on 24/04/16.
 *
 * backup the current hazelcast map to CQL
 *
 */
@Component
public class HazelcastWordCountMapStore implements MapStore<String, Long> {

    @Autowired
    private IDao dao;

    public HazelcastWordCountMapStore() {
    }

    @Override
    public Long load(String key) {
        return dao.getStatisticsDao().hazelcastWordMapLoad(key);
    }

    @Override
    public Map<String, Long> loadAll(Collection<String> keys) {
        return dao.getStatisticsDao().hazelcastWordMapLoadAll(keys);
    }

    @Override
    public Iterable<String> loadAllKeys() {
        return dao.getStatisticsDao().hazelcastWordMapLoadAllKeys();
    }


    @Override
    public void store(String key, Long value) {
        dao.getStatisticsDao().hazelcastWordMapStore(key, value);
    }

    @Override
    public void storeAll(Map<String, Long> map) {
        dao.getStatisticsDao().hazelcastWordMapStoreAll(map);
    }

    @Override
    public void delete(String key) {
        dao.getStatisticsDao().hazelcastWordMapDelete(key);
    }

    @Override
    public void deleteAll(Collection<String> keys) {
        dao.getStatisticsDao().hazelcastWordMapDeleteAll(keys);
    }


}


