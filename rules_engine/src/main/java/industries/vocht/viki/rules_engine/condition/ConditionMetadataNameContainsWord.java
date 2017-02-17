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

import industries.vocht.viki.rules_engine.model.RuleConversionException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 15/05/16.
 *
 */
public class ConditionMetadataNameContainsWord implements ICondition {

    private String metadata_name;
    private List<SingleWordCondition> single_word_list;

    public ConditionMetadataNameContainsWord() {
        this.single_word_list = new ArrayList<>();
    }

    public ConditionMetadataNameContainsWord( String metadata_name, String word_csv ) throws RuleConversionException {
        this.metadata_name = metadata_name;
        this.single_word_list = new ArrayList<>();
        String[] items = word_csv.split("\\|");
        for ( String item : items ) {
            this.single_word_list.add( new SingleWordCondition(item) );
        }
    }

    public String getLogic() {
        String str = "";
        for ( SingleWordCondition sword : single_word_list ) {
            str = str + " ";
            if ( sword.isExact() ) {
                str = str + "exact ";
            }
            str = str + metadata_name + "(";
            str = str + sword.getWord();
            if ( sword.getFilter() != null ) {
                str = str + "," + sword.getFilter();
            }
            str = str + ")";
            if ( sword.getLogic() != null ) {
                str = str + " " + sword.getLogic();
            }
        }
        return str.trim();
    }


    public String getMetadata_name() {
        return metadata_name;
    }

    public void setMetadata_name(String metadata_name) {
        this.metadata_name = metadata_name;
    }

    public List<SingleWordCondition> getSingle_word_list() {
        return single_word_list;
    }

    public void setSingle_word_list(List<SingleWordCondition> single_word_list) {
        this.single_word_list = single_word_list;
    }


}



