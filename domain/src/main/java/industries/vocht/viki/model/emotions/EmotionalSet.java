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

package industries.vocht.viki.model.emotions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 10/04/16.
 *
 * set of emotional values for inspection of a document
 *
 */
public class EmotionalSet {

    private UUID organisation_id;
    private String url;
    private List<EmotionalItem> emotional_list;

    public EmotionalSet( UUID organisation_id, String url ) {
        this.organisation_id = organisation_id;
        this.url = url;
        this.emotional_list = new ArrayList<>();
    }

    public EmotionalSet() {
        this.emotional_list = new ArrayList<>();
    }

    public List<EmotionalItem> getEmotional_list() {
        return emotional_list;
    }

    public void setEmotional_list(List<EmotionalItem> emotional_list) {
        this.emotional_list = emotional_list;
    }

    public UUID getOrganisation_id() {
        return organisation_id;
    }

    public void setOrganisation_id(UUID organisation_id) {
        this.organisation_id = organisation_id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}


