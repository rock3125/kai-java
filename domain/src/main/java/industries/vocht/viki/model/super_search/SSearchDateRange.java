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

package industries.vocht.viki.model.super_search;

import industries.vocht.viki.model.indexes.IIndex;

import java.util.List;

/**
 * Created by peter on 25/04/16.
 *
 * set of indexes for a date-range
 *
 */
public class SSearchDateRange implements ISSearchItem {

    private List<IIndex> timeIndexList;

    private SSearchDateRangeType operation; // { before, after, between, exact }

    private int year1;
    private int month1;
    private int day1;
    private int hour1;
    private int min1;

    private int year2;
    private int month2;
    private int day2;
    private int hour2;
    private int min2;

    public SSearchDateRange() {
    }

    public SSearchDateRange( SSearchDateRangeType operation, int year1, int month1, int day1, int hour1, int min1,
                             int year2, int month2, int day2, int hour2, int min2) {
        this.operation = operation;
        this.year1 = year1;
        this.month1 = month1;
        this.day1 = day1;
        this.hour1 = hour1;
        this.min1 = min1;
        this.year2 = year2;
        this.month2 = month2;
        this.day2 = day2;
        this.hour2 = hour2;
        this.min2 = min2;
    }

    public void getSearchTerms(List<SSearchWord> inList) {
    }

    public List<IIndex> getTimeIndexList() {
        return timeIndexList;
    }

    public void setTimeIndexList(List<IIndex> timeIndexList) {
        this.timeIndexList = timeIndexList;
    }

    public int getYear1() {
        return year1;
    }

    public void setYear1(int year1) {
        this.year1 = year1;
    }

    public int getMonth1() {
        return month1;
    }

    public void setMonth1(int month1) {
        this.month1 = month1;
    }

    public int getDay1() {
        return day1;
    }

    public void setDay1(int day1) {
        this.day1 = day1;
    }

    public int getHour1() {
        return hour1;
    }

    public void setHour1(int hour1) {
        this.hour1 = hour1;
    }

    public int getYear2() {
        return year2;
    }

    public void setYear2(int year2) {
        this.year2 = year2;
    }

    public int getMonth2() {
        return month2;
    }

    public void setMonth2(int month2) {
        this.month2 = month2;
    }

    public int getDay2() {
        return day2;
    }

    public void setDay2(int day2) {
        this.day2 = day2;
    }

    public int getHour2() {
        return hour2;
    }

    public void setHour2(int hour2) {
        this.hour2 = hour2;
    }

    public int getMin1() {
        return min1;
    }

    public void setMin1(int min1) {
        this.min1 = min1;
    }

    public int getMin2() {
        return min2;
    }

    public void setMin2(int min2) {
        this.min2 = min2;
    }

    public SSearchDateRangeType getOperation() {
        return operation;
    }

    public void setOperation(SSearchDateRangeType operation) {
        this.operation = operation;
    }
}






