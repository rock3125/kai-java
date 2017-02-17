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

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 12/05/16.
 *
 * a rule to be executed by the system
 *
 */
public class RuleItem {

    private UUID organisation_id;
    private String rule_name;
    private String creator;

    private List<RuleItemBase> event_list;
    private List<RuleItemBase> condition_list;
    private List<RuleItemBase> action_list;

    public RuleItem() {
        setEvent_list(new ArrayList<>());
        setCondition_list(new ArrayList<>());
        setAction_list(new ArrayList<>());
    }

    @JsonIgnore
    public RuleItemBase getEvent() {
        if ( event_list.size() == 1 ) {
            return event_list.get(0);
        }
        return null;
    }

    public String getRule_name() {
        return rule_name;
    }

    public void setRule_name(String rule_name) {
        this.rule_name = rule_name;
    }

    public List<RuleItemBase> getEvent_list() {
        return event_list;
    }

    public void setEvent_list(List<RuleItemBase> event_list) {
        this.event_list = event_list;
    }

    public List<RuleItemBase> getCondition_list() {
        return condition_list;
    }

    public void setCondition_list(List<RuleItemBase> condition_list) {
        this.condition_list = condition_list;
    }

    public List<RuleItemBase> getAction_list() {
        return action_list;
    }

    public void setAction_list(List<RuleItemBase> action_list) {
        this.action_list = action_list;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public UUID getOrganisation_id() {
        return organisation_id;
    }

    public void setOrganisation_id(UUID organisation_id) {
        this.organisation_id = organisation_id;
    }
}


