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

/**
 * Created by peter on 12/05/16.
 *
 * the base item of a rule item list
 *
 */
public class RuleItemBase {

    private Integer id;
    private String description;
    private String type;

    // if applicable - how to join this statement and the next
    private String logic;

    private RuleItemData data;

    public RuleItemBase() {
        this.data = new RuleItemData();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public RuleItemData getData() {
        return data;
    }

    public void setData(RuleItemData data) {
        this.data = data;
    }

    public String getLogic() {
        return logic;
    }

    public void setLogic(String logic) {
        this.logic = logic;
    }
}

