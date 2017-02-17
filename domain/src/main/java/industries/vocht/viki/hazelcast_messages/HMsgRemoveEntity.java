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

package industries.vocht.viki.hazelcast_messages;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by peter on 2/06/16.
 *
 * tell the system to remove an entity from its lexicon
 *
 */
public class HMsgRemoveEntity extends IHazelcastMessage implements Serializable {

    private UUID organisation_id;
    private UUID entity_id;

    public HMsgRemoveEntity() {
    }

    public HMsgRemoveEntity(UUID organisation_id, UUID entity_id ) {
        this.organisation_id = organisation_id;
        this.entity_id = entity_id;
    }


    public UUID getOrganisation_id() {
        return organisation_id;
    }

    public void setOrganisation_id(UUID organisation_id) {
        this.organisation_id = organisation_id;
    }

    public UUID getEntity_id() {
        return entity_id;
    }

    public void setEntity_id(UUID entity_id) {
        this.entity_id = entity_id;
    }

}

