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

package industries.vocht.viki.model.group;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 4/03/16.
 *
 */
public class Group {
    private String name;
    private UUID organisation_id;
    private List<String> user_list;

    public Group() {
        user_list = new ArrayList<>();
    }

    public Group(UUID organisation_id, String name, String... userList) {
        this.organisation_id = organisation_id;
        this.name = name;
        this.user_list = new ArrayList<>();
        this.user_list.addAll(Arrays.asList(userList));
    }

    public Group addUser( String user ) {
        if ( user != null && user.length() > 0 ) {
            user_list.add(user);
        }
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getOrganisation_id() {
        return organisation_id;
    }

    public void setOrganisation_id(UUID organisation_id) {
        this.organisation_id = organisation_id;
    }

    public List<String> getUser_list() {
        return user_list;
    }

    public void setUser_list(List<String> user_list) {
        this.user_list = user_list;
    }
}
