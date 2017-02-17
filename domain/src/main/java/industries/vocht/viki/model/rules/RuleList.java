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

package industries.vocht.viki.model.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 12/05/16.
 *
 * a list of rule items
 *
 */
public class RuleList {

    private UUID organisation_id;
    private List<RuleItem> rule_list;
    private int items_per_page;

    public RuleList() {
        rule_list = new ArrayList<>();
    }

    public RuleList(List<RuleItem> rule_list) {
        this.rule_list = rule_list;
    }

    public UUID getOrganisation_id() {
        return organisation_id;
    }

    public void setOrganisation_id(UUID organisation_id) {
        this.organisation_id = organisation_id;
    }

    public List<RuleItem> getRule_list() {
        return rule_list;
    }

    public void setRule_list(List<RuleItem> rule_list) {
        this.rule_list = rule_list;
    }

    public int getItems_per_page() {
        return items_per_page;
    }

    public void setItems_per_page(int items_per_page) {
        this.items_per_page = items_per_page;
    }

}

