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

package industries.vocht.viki.model;

import java.util.UUID;

/**
 * Created by peter on 4/03/16.
 *
 */
public class Organisation {
    private UUID id;
    private String name;
    private UUID primary_user;

    public Organisation() {
    }

    public Organisation(String name) {
        this.name = name;
    }

    public Organisation(UUID id, String name, UUID primary_user) {
        this.id = id;
        this.name = name;
        this.primary_user = primary_user;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getPrimary_user() {
        return primary_user;
    }

    public void setPrimary_user(UUID primary_user) {
        this.primary_user = primary_user;
    }
}

