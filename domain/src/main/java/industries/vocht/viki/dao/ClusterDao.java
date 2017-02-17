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
import industries.vocht.viki.model.UrlValue;
import industries.vocht.viki.model.k_means.kMeansCluster;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 28/04/16.
 *
 * cluster related data access
 *
 */
public class ClusterDao {

    private IDatabase db;

    public ClusterDao(IDatabase db) {
        this.db = db;
    }


    /**
     * save a full cluster
     * @param organisation_id the organisation's id
     * @param id the id of the cluster
     * @param cluster the cluster item itself
     * @throws IOException
     */
    public void saveCluster(UUID organisation_id, int id, kMeansCluster cluster) throws IOException {
        db.saveCluster(organisation_id, id, cluster);
    }


    /**
     * load a full cluster
     * @param organisation_id the organisation's id
     * @param id the id of the cluster
     * @throws IOException
     */
    public kMeansCluster loadFullClusterItem(UUID organisation_id, int id) throws IOException {
        return db.loadFullClusterItem(organisation_id, id);
    }

    /**
     * load a summary / reduced cluster
     * @param organisation_id the organisation's id
     * @param id the id of the cluster
     * @throws IOException
     */
    public kMeansCluster loadSummaryClusterItem(UUID organisation_id, int id) throws IOException {
        return db.loadSummaryClusterItem(organisation_id, id);
    }

    // when was the system last taken through a full cluster cycle?
    public long getClusterLastClustered(UUID organisation_id) {
        return db.getClusterLastClustered(organisation_id);
    }

    public void setClusterLastClustered(UUID organisation_id, long dateTime) {
        db.setClusterLastClustered(organisation_id, dateTime);
    }

    // when was the system last taken through a full cosine cycle?
    public long getCosineLastChange(UUID organisation_id) {
        return db.getCosineLastChange(organisation_id);
    }

    public void setCosineLastChange(UUID organisation_id, long dateTime) {
        db.setCosineLastChange(organisation_id, dateTime);
    }

    // when did the system last have a change that required re-clustering of the data?
    public long getClusterLastChange(UUID organisation_id) {
        return db.getClusterLastChange(organisation_id);
    }

    public void setClusterLastChange(UUID organisation_id, long dateTime) {
        db.setClusterLastChange(organisation_id, dateTime);
    }

    // save a document's emotion status if it has exceeded a certain threshold
    public void setDocumentEmotion(UUID organisation_id, String url, int positive_sentence_id, double positive,
                                                                     int negative_sentence_id, double negative) {
        db.setDocumentEmotion(organisation_id, url, positive_sentence_id, positive, negative_sentence_id, negative);
    }

    public List<UrlValue> getDocumentEmotion(UUID organisation_id, boolean positive, int offset, int pageSize) {
        return db.getDocumentEmotion(organisation_id, positive, offset, pageSize);
    }


}



