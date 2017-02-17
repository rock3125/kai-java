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

package industries.vocht.viki.rules_engine.condition;

/**
 * Created by peter on 15/05/16.
 *
 */
public class ConditionWordStats implements ICondition {

    private ConditionWordSetType type;
    private String word_csv;
    private String logic;

    public ConditionWordStats() {
    }

    public ConditionWordStats(String logic, ConditionWordSetType type, String word_csv ) {
        this.type = type;
        this.word_csv = word_csv;
    }

    public String getLogic() { return logic; }

    public ConditionWordSetType getType() {
        return type;
    }

    public void setType(ConditionWordSetType type) {
        this.type = type;
    }

    public String getWord_csv() {
        return word_csv;
    }

    public void setWord_csv(String word_csv) {
        this.word_csv = word_csv;
    }
}

