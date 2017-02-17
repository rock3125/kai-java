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

package industries.vocht.viki.datastructures;

import industries.vocht.viki.utility.StringUtility;
import org.junit.Test;
import org.springframework.util.Assert;

/**
 * Created by peter on 1/11/15.
 *
 * test string utilities
 *
 */
public class StringUtilityTest {

    @Test
    public void checkValue() {
        Assert.isTrue( new StringUtility().prettyCommaPrint(1234L).equals("1,234") );
        Assert.isTrue( new StringUtility().prettyCommaPrint(123).equals("123") );
        Assert.isTrue( new StringUtility().prettyCommaPrint(1).equals("1") );
        Assert.isTrue( new StringUtility().prettyCommaPrint(123456L).equals("123,456") );
        Assert.isTrue( new StringUtility().prettyCommaPrint(11123456L).equals("11,123,456") );
    }

    @Test
    public void testStringCopy() {
        String str1 = "Some test StRinG!";
        String str2 = new StringUtility().copy(str1);
        Assert.isTrue( str2.equals(str1) ); // content equals
        Assert.isTrue( str2 != str1 ); // pointers differ
    }


}

