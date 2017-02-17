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

package industries.vocht.viki.model.indexes;

import java.util.List;

/**
 * Created by peter on 26/04/16.
 *
 * a list of time selectors and the boundary calculated cases
 *
 */
public class TimeSelectorSetWithBoundaries {

    private long time1;
    private long time2;
    private List<TimeIndexSelector> timeIndexSelectorList;

    public TimeSelectorSetWithBoundaries(long time1, long time2, List<TimeIndexSelector> timeIndexSelectorList) {
        this.time1 = time1;
        this.time2 = time2;
        this.timeIndexSelectorList = timeIndexSelectorList;
    }


    public long getTime1() {
        return time1;
    }

    public void setTime1(long time1) {
        this.time1 = time1;
    }

    public long getTime2() {
        return time2;
    }

    public void setTime2(long time2) {
        this.time2 = time2;
    }

    public List<TimeIndexSelector> getTimeIndexSelectorList() {
        return timeIndexSelectorList;
    }

    public void setTimeIndexSelectorList(List<TimeIndexSelector> timeIndexSelectorList) {
        this.timeIndexSelectorList = timeIndexSelectorList;
    }


}



