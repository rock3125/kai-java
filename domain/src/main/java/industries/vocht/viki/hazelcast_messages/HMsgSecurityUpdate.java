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
 * Created by peter on 13/06/16.
 *
 * a message that the security model has changed
 *
 */
public class HMsgSecurityUpdate extends IHazelcastMessage implements Serializable {

    private UUID organisation_id;

    public HMsgSecurityUpdate() {
    }

    public HMsgSecurityUpdate(UUID organisation_id) {
        this.organisation_id = organisation_id;
    }

    public UUID getOrganisation_id() {
        return organisation_id;
    }

    public void setOrganisation_id(UUID organisation_id) {
        this.organisation_id = organisation_id;
    }

}

