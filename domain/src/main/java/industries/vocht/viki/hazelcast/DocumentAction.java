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

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by peter on 2/04/16.
 *
 * carry a document work item
 *
 */
public class DocumentAction implements Serializable {

    private String url;                     // the document's uid
    private UUID organisation_id;           // owner org

    public DocumentAction() {
    }

    public DocumentAction(UUID organisation_id, String url ) {
        this.organisation_id = organisation_id;
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public UUID getOrganisation_id() {
        return organisation_id;
    }

    public void setOrganisation_id(UUID organisation_id) {
        this.organisation_id = organisation_id;
    }

}

