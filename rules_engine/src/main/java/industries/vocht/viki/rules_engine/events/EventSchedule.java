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

/**
 * Created by peter on 15/05/16.
 *
 */
public class EventSchedule implements IEvent {

    private String time_csv;

    public EventSchedule() {
    }

    public EventSchedule(String time_csv) {
        this.time_csv = time_csv;
    }

    public String getTime_csv() {
        return time_csv;
    }

    public void setTime_csv(String time_csv) {
        this.time_csv = time_csv;
    }
}

