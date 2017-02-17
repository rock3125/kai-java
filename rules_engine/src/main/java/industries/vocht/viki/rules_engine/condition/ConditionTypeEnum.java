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
public enum ConditionTypeEnum {

    DateRangeContent("date-time-content"),
    DateRangeCreated("date-time-created"),
    ContentContainsWord("content-contains"),
    TitleContainsWord("title-contains"),
    MetadataNameContainsWord("metadata-contains"),
    AuthorContainsWord("author"),
    SummaryContainsWord("summary"),
    WordStatsForSet("word-statistics"),
    NegativeContent("negative-content"),
    PositiveContent("positive-content"),
    SexualContent("sexual-content"),
    CloseDuplicate("duplicate-content");

    private ConditionTypeEnum( String value ) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    private String value;


}

