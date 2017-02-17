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

package industries.vocht.viki.model;

import industries.vocht.viki.document.Document;
import industries.vocht.viki.model.super_search.*;
import org.junit.Test;
import org.springframework.util.Assert;

/**
 * Created by peter on 25/04/16.
 *
 * test the super search parser parses as we might expect
 * according to its grammar
 *
 */
@SuppressWarnings("ConstantConditions")
public class SSearchParserTest {

    @Test
    public void testSSPWord1() throws Exception {
        SSearchParser parser = new SSearchParser();
        ISSearchItem item = parser.parse("body(hello there!)");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchWord);
        SSearchWord conv = (SSearchWord)item;
        Assert.isTrue(conv.getWord() != null && conv.getWord().equals("hello there!"));
        Assert.isTrue(!conv.isExact());
        Assert.isTrue(conv.getMetadata() != null && conv.getMetadata().equals(Document.META_BODY));
    }

    @Test
    public void testSSPWord2() throws Exception {
        SSearchParser parser = new SSearchParser();
        ISSearchItem item = parser.parse("body(Peter,nnp)");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchWord);
        SSearchWord conv = (SSearchWord)item;
        Assert.isTrue(conv.getWord() != null && conv.getWord().equals("Peter"));
        Assert.isTrue(conv.getTag() != null && conv.getTag().equals("NNP"));
        Assert.isTrue(!conv.isExact());
        Assert.isTrue(conv.getMetadata() != null && conv.getMetadata().equals(Document.META_BODY));
    }

    @Test
    public void testSSPPerson1() throws Exception {
        SSearchParser parser = new SSearchParser();
        ISSearchItem item = parser.parse("person(Peter)");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchWord);
        SSearchWord conv = (SSearchWord)item;
        Assert.isTrue(conv.getWord() != null && conv.getWord().equals("Peter"));
        Assert.isTrue(conv.getTag() != null && conv.getTag().equals("NNP"));
        Assert.isTrue(!conv.isExact());
        Assert.isTrue(conv.getMetadata() != null && conv.getMetadata().equals(Document.META_BODY));
    }

    @Test
    public void testSSPWord3() throws Exception {
        SSearchParser parser = new SSearchParser();
        ISSearchItem item = parser.parse("exact body(Peter,nnp)");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchWord);
        SSearchWord conv = (SSearchWord)item;
        Assert.isTrue(conv.getWord() != null && conv.getWord().equals("Peter"));
        Assert.isTrue(conv.getTag() != null && conv.getTag().equals("NNP"));
        Assert.isTrue(conv.isExact());
        Assert.isTrue(conv.getMetadata() != null && conv.getMetadata().equals(Document.META_BODY));
    }

    @Test
    public void testSSPWord4() throws Exception {
        SSearchParser parser = new SSearchParser();
        ISSearchItem item = parser.parse("author(Peter)");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchWord);
        SSearchWord conv = (SSearchWord)item;
        Assert.isTrue(conv.getWord() != null && conv.getWord().equals("Peter"));
        Assert.isTrue(conv.getTag() != null && conv.getTag().equals("NNP"));
        Assert.isTrue(!conv.isExact());
        Assert.isTrue(conv.getMetadata() != null && conv.getMetadata().equals(Document.META_AUTHOR));
    }

    @Test
    public void testSSPWord5() throws Exception {
        SSearchParser parser = new SSearchParser();
        ISSearchItem item = parser.parse("title(Peter)");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchWord);
        SSearchWord conv = (SSearchWord)item;
        Assert.isTrue(conv.getWord() != null && conv.getWord().equals("Peter"));
        Assert.isNull(conv.getTag());
        Assert.isTrue(!conv.isExact());
        Assert.isTrue(conv.getMetadata() != null && conv.getMetadata().equals(Document.META_TITLE));
    }

    @Test
    public void testSSPWord6() throws Exception {
        SSearchParser parser = new SSearchParser();
        ISSearchItem item = parser.parse("exact summary(Peter and Sherry)");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchWord);
        SSearchWord conv = (SSearchWord)item;
        Assert.isTrue(conv.getWord() != null && conv.getWord().equals("Peter and Sherry"));
        Assert.isNull(conv.getTag());
        Assert.isTrue(conv.isExact());
        Assert.isTrue(conv.getMetadata() != null && conv.getMetadata().equals(Document.META_SUMMARIZATION));
    }

    @Test
    public void testSSPDate1() throws Exception {
        SSearchParser parser = new SSearchParser();
        ISSearchItem item = parser.parse("  date  before  2014  ");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchDateRange);
        SSearchDateRange conv = (SSearchDateRange)item;
        Assert.isTrue(conv.getOperation() != null && conv.getOperation() == SSearchDateRangeType.Before);
        Assert.isTrue(conv.getYear1() == 2014);
        Assert.isTrue(conv.getMonth1() == -1);
        Assert.isTrue(conv.getDay1() == -1);
        Assert.isTrue(conv.getHour1() == -1);
    }

    @Test
    public void testSSPDate2() throws Exception {
        SSearchParser parser = new SSearchParser();
        ISSearchItem item = parser.parse("  date after  2014  -  12 ");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchDateRange);
        SSearchDateRange conv = (SSearchDateRange)item;
        Assert.isTrue(conv.getOperation() != null && conv.getOperation() == SSearchDateRangeType.After );
        Assert.isTrue(conv.getYear1() == 2014);
        Assert.isTrue(conv.getMonth1() == 12);
        Assert.isTrue(conv.getDay1() == -1);
        Assert.isTrue(conv.getHour1() == -1);
    }

    @Test
    public void testSSPDate3() throws Exception {
        SSearchParser parser = new SSearchParser();
        ISSearchItem item = parser.parse("  date  exact  2014  -  12  -  31  ");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchDateRange);
        SSearchDateRange conv = (SSearchDateRange)item;
        Assert.isTrue(conv.getOperation() != null && conv.getOperation() == SSearchDateRangeType.Exact);
        Assert.isTrue(conv.getYear1() == 2014);
        Assert.isTrue(conv.getMonth1() == 12);
        Assert.isTrue(conv.getDay1() == 31);
        Assert.isTrue(conv.getHour1() == -1);

        item = parser.parse("date exact 2014-12-31");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchDateRange);
        conv = (SSearchDateRange)item;
        Assert.isTrue(conv.getOperation() != null && conv.getOperation() == SSearchDateRangeType.Exact);
        Assert.isTrue(conv.getYear1() == 2014);
        Assert.isTrue(conv.getMonth1() == 12);
        Assert.isTrue(conv.getDay1() == 31);
        Assert.isTrue(conv.getHour1() == -1);
    }

    @Test
    public void testSSPDate4() throws Exception {
        SSearchParser parser = new SSearchParser();
        ISSearchItem item = parser.parse("date after 2014-12-31 15:00");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchDateRange);
        SSearchDateRange conv = (SSearchDateRange)item;
        Assert.isTrue(conv.getOperation() != null && conv.getOperation() == SSearchDateRangeType.After);
        Assert.isTrue(conv.getYear1() == 2014);
        Assert.isTrue(conv.getMonth1() == 12);
        Assert.isTrue(conv.getDay1() == 31);
        Assert.isTrue(conv.getHour1() == 15);
    }


    @Test
    public void testSSPDate5() throws Exception {
        SSearchParser parser = new SSearchParser();
        ISSearchItem item = parser.parse("  date  between  2014  and  2015  ");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchDateRange);
        SSearchDateRange conv = (SSearchDateRange)item;
        Assert.isTrue(conv.getOperation() != null && conv.getOperation() == SSearchDateRangeType.Between);
        Assert.isTrue(conv.getYear1() == 2014);
        Assert.isTrue(conv.getMonth1() == -1);
        Assert.isTrue(conv.getDay1() == -1);
        Assert.isTrue(conv.getHour1() == -1);
        Assert.isTrue(conv.getYear2() == 2015);
        Assert.isTrue(conv.getMonth2() == -1);
        Assert.isTrue(conv.getDay2() == -1);
        Assert.isTrue(conv.getHour2() == -1);
    }

    @Test
    public void testSSPDate6() throws Exception {
        SSearchParser parser = new SSearchParser();
        ISSearchItem item = parser.parse("date between 2014-12 and 2015-01");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchDateRange);
        SSearchDateRange conv = (SSearchDateRange)item;
        Assert.isTrue(conv.getOperation() != null && conv.getOperation() == SSearchDateRangeType.Between);
        Assert.isTrue(conv.getYear1() == 2014);
        Assert.isTrue(conv.getMonth1() == 12);
        Assert.isTrue(conv.getDay1() == -1);
        Assert.isTrue(conv.getHour1() == -1);
        Assert.isTrue(conv.getYear2() == 2015);
        Assert.isTrue(conv.getMonth2() == 1);
        Assert.isTrue(conv.getDay2() == -1);
        Assert.isTrue(conv.getHour2() == -1);
    }

    @Test
    public void testSSPDate7() throws Exception {
        SSearchParser parser = new SSearchParser();
        ISSearchItem item = parser.parse("date between 2014-12-31 and 2016-2-28");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchDateRange);
        SSearchDateRange conv = (SSearchDateRange)item;
        Assert.isTrue(conv.getOperation() != null && conv.getOperation() == SSearchDateRangeType.Between);
        Assert.isTrue(conv.getYear1() == 2014);
        Assert.isTrue(conv.getMonth1() == 12);
        Assert.isTrue(conv.getDay1() == 31);
        Assert.isTrue(conv.getHour1() == -1);
        Assert.isTrue(conv.getYear2() == 2016);
        Assert.isTrue(conv.getMonth2() == 2);
        Assert.isTrue(conv.getDay2() == 28);
        Assert.isTrue(conv.getHour2() == -1);
    }

    @Test
    public void testSSPDate8() throws Exception {
        SSearchParser parser = new SSearchParser();
        ISSearchItem item = parser.parse("date between 2014-12-31 15:01 and 2016-12-01 12:42");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchDateRange);
        SSearchDateRange conv = (SSearchDateRange)item;
        Assert.isTrue(conv.getOperation() != null && conv.getOperation() == SSearchDateRangeType.Between);
        Assert.isTrue(conv.getYear1() == 2014);
        Assert.isTrue(conv.getMonth1() == 12);
        Assert.isTrue(conv.getDay1() == 31);
        Assert.isTrue(conv.getHour1() == 15);
        Assert.isTrue(conv.getMin1() == 1);
        Assert.isTrue(conv.getYear2() == 2016);
        Assert.isTrue(conv.getMonth2() == 12);
        Assert.isTrue(conv.getDay2() == 1);
        Assert.isTrue(conv.getHour2() == 12);
        Assert.isTrue(conv.getMin2() == 42);
    }



    @Test
    public void testSSPAnd1() throws Exception {
        SSearchParser parser = new SSearchParser();
        ISSearchItem item = parser.parse("body(test) and body(Peter dearest)");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchAnd);
        SSearchAnd conv = (SSearchAnd)item;
        Assert.isTrue(conv.getLeft() != null && conv.getRight() != null);
        Assert.isTrue(conv.getLeft() instanceof SSearchWord);
        Assert.isTrue(((SSearchWord)conv.getLeft()).getWord().equals("test"));
        Assert.isTrue(conv.getRight() instanceof SSearchWord);
        Assert.isTrue(((SSearchWord)conv.getRight()).getWord().equals("Peter dearest"));
    }

    @Test
    public void testSSPAnd2() throws Exception {
        SSearchParser parser = new SSearchParser();
        ISSearchItem item = parser.parse("  body(test)  and  author(Peter best)  and body(Markie Mark)");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchAnd);
        SSearchAnd conv = (SSearchAnd)item;
        Assert.isTrue(conv.getLeft() != null && conv.getRight() != null);
        Assert.isTrue(conv.getLeft() instanceof SSearchWord);
        Assert.isTrue(((SSearchWord)conv.getLeft()).getWord().equals("test"));
        Assert.isTrue(conv.getRight() instanceof SSearchAnd);

        SSearchAnd and2 = (SSearchAnd)conv.getRight();

        Assert.isTrue(((SSearchWord)and2.getLeft()).getWord().equals("Peter best"));
        Assert.isTrue(((SSearchWord)and2.getRight()).getWord().equals("Markie Mark"));
    }

    @Test
    public void testSSPOr1() throws Exception {
        SSearchParser parser = new SSearchParser();
        ISSearchItem item = parser.parse("  body(test)  or   summary(Peter)  ");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchOr);
        SSearchOr conv = (SSearchOr)item;
        Assert.isTrue(conv.getLeft() != null && conv.getRight() != null);
        Assert.isTrue(conv.getLeft() instanceof SSearchWord);
        Assert.isTrue(((SSearchWord)conv.getLeft()).getWord().equals("test"));
        Assert.isTrue(conv.getRight() instanceof SSearchWord);
        Assert.isTrue(((SSearchWord)conv.getRight()).getWord().equals("Peter"));
    }

    @Test
    public void testSSPBracket1() throws Exception {
        SSearchParser parser = new SSearchParser();
        ISSearchItem item = parser.parse("(body(hello there!))");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchWord);
        SSearchWord conv = (SSearchWord)item;
        Assert.isTrue(conv.getWord() != null && conv.getWord().equals("hello there!"));
    }


    @Test
    public void testSSBracket2() throws Exception {
        SSearchParser parser = new SSearchParser();
        ISSearchItem item = parser.parse("  (  title(test)  or   body(Peter)  )  ");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchOr);
        SSearchOr conv = (SSearchOr) item;
        Assert.isTrue(conv.getLeft() != null && conv.getRight() != null);
        Assert.isTrue(conv.getLeft() instanceof SSearchWord);
        Assert.isTrue(((SSearchWord) conv.getLeft()).getWord().equals("test"));
        Assert.isTrue(conv.getRight() instanceof SSearchWord);
        Assert.isTrue(((SSearchWord) conv.getRight()).getWord().equals("Peter"));
    }

    @Test
    public void testSSPAndNot1() throws Exception {
        SSearchParser parser = new SSearchParser();
        ISSearchItem item = parser.parse("  location(test)  and  not   title(Peter)  ");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchAndNot);
        SSearchAndNot conv = (SSearchAndNot)item;
        Assert.isTrue(conv.getLeft() != null && conv.getRight() != null);
        Assert.isTrue(conv.getLeft() instanceof SSearchWord);
        Assert.isTrue(((SSearchWord)conv.getLeft()).getWord().equals("test"));
        Assert.isTrue(conv.getRight() instanceof SSearchWord);
        Assert.isTrue(((SSearchWord)conv.getRight()).getWord().equals("Peter"));
    }


    @Test
    public void testDoubleBracketsPerson() throws Exception {
        SSearchParser parser = new SSearchParser();
        ISSearchItem item = parser.parse("(person(Peter))");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchWord);
        SSearchWord conv = (SSearchWord)item;
        Assert.isTrue(conv.getWord().equals("Peter"));
        Assert.isTrue(conv.getSemantic() != null && conv.getSemantic().equals("person"));
        Assert.isTrue(conv.getMetadata() != null && conv.getMetadata().equals(Document.META_BODY));
    }


    @Test
    public void testDoubleBracketsLocation() throws Exception {
        SSearchParser parser = new SSearchParser();
        ISSearchItem item = parser.parse("(exact location(Sydney Harbour bridge))");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchWord);
        SSearchWord conv = (SSearchWord)item;
        Assert.isTrue(conv.getWord().equals("Sydney Harbour bridge"));
        Assert.isTrue(conv.isExact());
        Assert.isTrue(conv.getSemantic() != null && conv.getSemantic().equals("location"));
        Assert.isTrue(conv.getMetadata() != null && conv.getMetadata().equals(Document.META_BODY));
    }


    @Test
    public void testDoubleBracketsUrl() throws Exception {
        SSearchParser parser = new SSearchParser();
        ISSearchItem item = parser.parse("(url(http://some part of))");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchWord);
        SSearchWord conv = (SSearchWord)item;
        Assert.isTrue(conv.getWord().equals("http://some part of"));
        Assert.isTrue(conv.getMetadata() != null && conv.getMetadata().equals(Document.META_URL));
    }

    @Test
    public void testDateBefore1() throws Exception {
        SSearchParser parser = new SSearchParser();
        ISSearchItem item = parser.parse("(date before 2016/12/31)");
        Assert.notNull(item);
        Assert.isTrue(item instanceof SSearchDateRange);
        SSearchDateRange conv = (SSearchDateRange)item;
        Assert.isTrue(conv.getOperation() == SSearchDateRangeType.Before);
        Assert.isTrue(conv.getYear1() == 2016 && conv.getMonth1() == 12 && conv.getDay1() == 31 );
    }


}




