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

package industries.vocht.viki.rules_engine;

import industries.vocht.viki.ApplicationException;
import industries.vocht.viki.rules_engine.condition.*;
import industries.vocht.viki.rules_engine.model.RuleConversionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Created by peter on 28/05/16.
 *
 *
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test-context.xml"})
public class RuleProcessorTest {

    @Test
    public void conditionTest1() throws RuleConversionException {
        ConditionDateRange c1 = new ConditionDateRange("metadata", "between,2010,2015,or|before,2005," );
        Assert.isTrue( c1.getDate_range_list() != null && c1.getDate_range_list().size() == 2 );
        SingleDateRange d1 = c1.getDate_range_list().get(0);
        SingleDateRange d2 = c1.getDate_range_list().get(1);
        Assert.isTrue( d1 != null && d1.getTime_1().equals("2010") && d1.getTime_2().equals("2015") && d1.getOperation().equals("between") );
        Assert.isTrue( d2 != null && d2.getTime_1().equals("2005") && d2.getTime_2() == null && d2.getOperation().equals("before") );
        String str1 = c1.getLogic();
        Assert.isTrue( str1 != null&& str1.equals("(between 2010) or (before 2005)") );

        ConditionDateRange c2 = new ConditionDateRange("metadata", "between,2010,2015,or|before,2005,,and not|after,2015," );
        Assert.isTrue( c2.getDate_range_list() != null && c2.getDate_range_list().size() == 3 );
        d1 = c2.getDate_range_list().get(0);
        d2 = c2.getDate_range_list().get(1);
        SingleDateRange d3 = c2.getDate_range_list().get(2);
        Assert.isTrue( d1 != null && d1.getTime_1().equals("2010") && d1.getTime_2().equals("2015") && d1.getOperation().equals("between") );
        Assert.isTrue( d2 != null && d2.getTime_1().equals("2005") && d2.getTime_2() == null && d2.getOperation().equals("before") && d2.getLogic().equals("and not"));
        Assert.isTrue( d3 != null && d3.getTime_1().equals("2015") && d3.getTime_2() == null && d3.getOperation().equals("after") && d3.getLogic() == null);
        String str2 = c2.getLogic();
        Assert.isTrue( str2 != null && str2.equals("(between 2010) or (before 2005) and not (after 2015)"));
    }

    @Test
    public void conditionTest2() throws RuleConversionException {
        ConditionMetadataNameContainsWord c1 = new ConditionMetadataNameContainsWord("body", "word1,nnp,,and|word2,,exact,eol" );
        Assert.isTrue( c1.getSingle_word_list() != null && c1.getSingle_word_list().size() == 2 );
        SingleWordCondition d1 = c1.getSingle_word_list().get(0);
        Assert.isTrue( d1 != null && d1.getWord().equals("word1") && d1.getFilter().equals("nnp") && d1.getLogic().equals("and") && !d1.isExact() );
        SingleWordCondition d2 = c1.getSingle_word_list().get(1);
        Assert.isTrue( d2 != null && d2.getWord().equals("word2") && d2.getFilter() == null && d2.getLogic() == null && d2.isExact() );
        String str1 = c1.getLogic();
        Assert.isTrue( str1 != null&& str1.equals("body(word1,nnp) and exact body(word2)") );
    }

}

