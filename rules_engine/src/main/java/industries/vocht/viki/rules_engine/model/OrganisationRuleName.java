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

package industries.vocht.viki.rules_engine.model;

import java.util.UUID;

/**
 * Created by peter on 15/05/16.
 *
 * grouping of an organisation and the name of a rule
 *
 */
public class OrganisationRuleName {

    private UUID organisation_id;
    private String rule_name;
    // the optional url of the object
    private String url;
    // the user wishing to execute the query
    private String email;

    public OrganisationRuleName() {
    }

    public OrganisationRuleName( UUID organisation_id, String rule_name, String email ) {
        this.organisation_id = organisation_id;
        this.email = email;
        this.rule_name = rule_name;
    }


    public OrganisationRuleName( UUID organisation_id, String rule_name, String url, String email ) {
        this.organisation_id = organisation_id;
        this.rule_name = rule_name;
        this.email = email;
        this.setUrl(url);
    }

    /**
     * return a pretty print of the contents of this object, used for detecting identical
     * rules executing
     * @return the nicely formatted string that is this object
     */
    public String toString() {
        if ( organisation_id != null && rule_name != null ) {
            String str = rule_name + "," + organisation_id.toString();
            if (url != null) {
                str = str + "," + url;
            }
            return str;
        } else {
            return "empty";
        }
    }

    public UUID getOrganisation_id() {
        return organisation_id;
    }

    public void setOrganisation_id(UUID organisation_id) {
        this.organisation_id = organisation_id;
    }

    public String getRule_name() {
        return rule_name;
    }

    public void setRule_name(String rule_name) {
        this.rule_name = rule_name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
