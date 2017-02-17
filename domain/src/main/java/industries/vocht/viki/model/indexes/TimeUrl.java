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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 20/04/16.
 *
 * time and url
 *
 */
public class TimeUrl implements Comparable<TimeUrl> {

    // the object
    private List<String> urlList;
    // the owner of this object
    private String entity_name;

    private long dateTime;

    private int year;
    private int month;
    private int day;
    private int hour;
    private int minute;
    private int second;

    public TimeUrl() {
        this.urlList = new ArrayList<>();
    }

    public TimeUrl( String url, String entity_name,
                    long dateTime, int year, int month, int day, int hour, int minute, int second ) {
        this.urlList = new ArrayList<>();
        this.urlList.add(url);
        this.entity_name = entity_name;
        this.dateTime = dateTime;
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.second = second;
    }

    public List<String> getUrlList() {
        return urlList;
    }

    public void setUrlList(List<String> urlList) {
        this.urlList = urlList;
    }

    public long getDateTime() {
        return dateTime;
    }

    public void setDateTime(long dateTime) {
        this.dateTime = dateTime;
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

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public int getSecond() {
        return second;
    }

    public void setSecond(int second) {
        this.second = second;
    }

    public String getEntity_name() {
        return entity_name;
    }

    public void setEntity_name(String entity_name) {
        this.entity_name = entity_name;
    }

    @Override
    public int compareTo(TimeUrl other) {
        if ( dateTime < other.dateTime ) return 1;
        if ( dateTime > other.dateTime ) return -1;
        return 0;
    }

}

