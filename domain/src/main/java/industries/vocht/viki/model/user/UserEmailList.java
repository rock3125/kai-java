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

package industries.vocht.viki.model.user;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 4/03/16.
 *
 */
public class UserEmailList {

    private UUID organisation_id;
    private List<String> user_list;

    public UserEmailList() {
        user_list = new ArrayList<>();
    }

    public UserEmailList(UUID organisation_id, List<String> user_list) {
        this.organisation_id = organisation_id;
        this.user_list = user_list;
    }

    // remove an email address from the list
    public void remove( String email ) {
        user_list.remove(email);
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

