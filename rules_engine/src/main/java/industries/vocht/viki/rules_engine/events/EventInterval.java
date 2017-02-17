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

package industries.vocht.viki.rules_engine.events;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by peter on 15/05/16.
 *
 */
public class /*
 * Copyright (c) Peter de Vocht, 2016.
 */

EventInterval implements IEvent {

    private String interval_unit;
    private int interval;

    public EventInterval() {
    }

    public EventInterval(int interval, String interval_unit ) {
        this.interval = interval;
        this.interval_unit = interval_unit;
    }

    @JsonIgnore
    public long intervalToMilliseconds() {
        long value = interval;
        switch ( interval_unit ) {
            case "hours": {
                return value * 3600_000L;
            }
            case "days": {
                return value * 24L * 3600_000L;
            }
            case "weeks": {
                return value * 7L * 24L * 3600_000L;
            }
            case "months": {
                return value * 30L * 7L * 24L * 3600_000L;
            }
        }
        return 0L;
    }

    public String getInterval_unit() {
        return interval_unit;
    }

    public void setInterval_unit(String interval_unit) {
        this.interval_unit = interval_unit;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }
}
