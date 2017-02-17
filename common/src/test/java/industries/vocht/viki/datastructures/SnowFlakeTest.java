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

import industries.vocht.viki.utility.SnowFlake;
import org.junit.Assert;
import org.junit.Test;

/*
 * Created by peter on 5/02/15.
 *
 * test Twitter's snowflake 64 bit id generator
 *
 */
public class SnowFlakeTest {

    @Test
    public void checkIds() {
        SnowFlake snowFlake = new SnowFlake(1,1);
        long prevID = 0L;
        for ( long i = 0L; i < 100_000; i++ ) {
            long id = snowFlake.nextID();
            Assert.assertTrue( prevID != id);
            prevID = id;
        }
    }

    @Test
    public void checkIdsWithDifferentSnowflakes() {
        SnowFlake snowFlake1 = new SnowFlake(1,1);
        SnowFlake snowFlake2 = new SnowFlake(1,2);
        for ( long i = 0L; i < 100_000; i++ ) {
            long id1 = snowFlake1.nextID();
            long id2 = snowFlake2.nextID();
            Assert.assertTrue( id1 != id2 );
        }
    }


}


