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

package industries.vocht.viki.model.nnet;

import java.util.Map;
import java.util.UUID;

/**
 * Created by peter on 27/05/16.
 *
 * simple encapsulation of a nnet training sample
 *
 */
public class NNetTrainingSample {

    private UUID id;
    private Map<String, Integer> training_data;

    public NNetTrainingSample() {
    }

    public NNetTrainingSample(UUID id, Map<String, Integer> training_data) {
        this.id = id;
        this.training_data = training_data;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Map<String, Integer> getTraining_data() {
        return training_data;
    }

    public void setTraining_data(Map<String, Integer> training_data) {
        this.training_data = training_data;
    }


}

