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
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 4/03/16.
 *
 */
public class GroupList {

    private UUID organisation_id;
    private List<Group> group_list;
    private int page;
    private int items_per_page;
    private int total_group_count;

    public GroupList() {
        group_list = new ArrayList<>();
    }

    public UUID getOrganisation_id() {
        return organisation_id;
    }

    public void setOrganisation_id(UUID organisation_id) {
        this.organisation_id = organisation_id;
    }

    public List<Group> getGroup_list() {
        return group_list;
    }

    public void setGroup_list(List<Group> group_list) {
        this.group_list = group_list;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getItems_per_page() {
        return items_per_page;
    }

    public void setItems_per_page(int items_per_page) {
        this.items_per_page = items_per_page;
    }

    public int getTotal_group_count() {
        return total_group_count;
    }

    public void setTotal_group_count(int total_group_count) {
        this.total_group_count = total_group_count;
    }
}
