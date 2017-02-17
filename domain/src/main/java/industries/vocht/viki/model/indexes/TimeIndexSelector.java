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

/**
 * Created by peter on 20/04/16.
 *
 * year/month/day/hour selector for searching through the indexes
 *
 */
public class TimeIndexSelector {

    private int year;
    private int month;
    private int day;
    private int hour;

    public TimeIndexSelector() {
    }

    public TimeIndexSelector(int year, int month ) {
        this.year = year;
        this.month = month;
        this.day = -1;
        this.hour = -1;
    }

    public TimeIndexSelector(int year, int month, int day ) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = -1;
    }

    public TimeIndexSelector(int year, int month, int day, int hour) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
    }

    public boolean timeSelectorForYearMonth() {
        return day == -1 && hour == -1;
    }

    public boolean timeSelectorForDays() {
        return day >= 1 && hour == -1;
    }

    public boolean timeSelectorForHours() {
        return day >= 1 && hour >= 0;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }


}


