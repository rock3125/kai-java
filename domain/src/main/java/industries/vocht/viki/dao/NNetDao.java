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
import industries.vocht.viki.model.nnet.NNetModelData;
import industries.vocht.viki.model.nnet.NNetTrainingSample;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 27/05/16.
 *
 * neural network dao access
 *
 */
public class NNetDao {

    private IDatabase db;

    public NNetDao(IDatabase db) {
        this.db = db;
    }


    public void addNNetTrainingSample(UUID organisation_id, String word, int synset_id,
                               NNetTrainingSample training_set ) throws IOException {
        db.addNNetTrainingSample(organisation_id, word, synset_id, training_set);
    }

    public List<NNetTrainingSample> getNNetTrainingSamples(UUID organisation_id, String word, int synset_id,
                                                    UUID prev_id, int pageSize ) throws IOException {
        return db.getNNetTrainingSamples(organisation_id, word, synset_id, prev_id, pageSize);
    }

    public void saveModel( UUID organisation_id, String word, NNetModelData model ) throws IOException {
        db.saveModel(organisation_id, word, model);
    }

    public NNetModelData loadModel( UUID organisation_id, String word ) throws IOException {
        return db.loadModel(organisation_id, word);
    }

    // return when the neural network model was last updated so that we can reload them if need be
    public long getModelLastUpdated( UUID organisation_id, String word ) throws IOException {
        return db.getModelLastUpdated(organisation_id, word);
    }


}
