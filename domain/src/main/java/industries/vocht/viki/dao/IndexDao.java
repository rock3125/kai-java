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

package industries.vocht.viki.dao;

import industries.vocht.viki.IDatabase;
import industries.vocht.viki.model.emotions.EmotionalSet;
import industries.vocht.viki.model.indexes.Index;
import industries.vocht.viki.model.indexes.TimeIndex;
import industries.vocht.viki.model.indexes.TimeIndexSelector;
import industries.vocht.viki.model.indexes.TimeSelectorSetWithBoundaries;
import org.joda.time.DateTime;

import java.util.*;

/**
 * Created by peter on 6/03/16.
 *
 */
@SuppressWarnings("ConstantConditions")
public class IndexDao {

    private IDatabase db;

    public IndexDao(IDatabase db) {
        this.db = db;
    }

    /**
     * add an index for an organisation - needs flush to complete
     * @param organisation_id the organisation concerned
     * @param index the index to add
     */
    public void addIndex(UUID organisation_id, Index index) {
        db.addIndex(organisation_id, index);
    }

    /**
     * flush the indexes after a few adds - this ensures that the auto-batch system in the background
     * of the index system actually flushes the last remaining indexes to the database engine
     */
    public void flushIndexes() {
        db.flushIndexes();
    }

    /**
     * remove an index from the system, remove all indexes belonging to a single url
     * @param organisation_id the organisation in question
     * @param url the url owner of the indexes to remove
     * @param metadata the metadata field of the index
     */
    public void removeIndex(UUID organisation_id, String url, String metadata) {
        db.removeIndex(organisation_id, url, metadata);
    }

    /**
     * read an index from the system
     * @param organisation_id the organisation id
     * @param word the word in question (Case insensitive)
     * @param shard the shard to read from
     * @return a list of indexes or null if dne / none found
     */
    public List<Index> readIndex(UUID organisation_id, String word, int shard, String metadata) {
        return db.readIndex(organisation_id, word, shard, metadata);
    }

    /**
     * an emotional index is an index to stores an emotional value for a given document / sentence
     * with security
     * @param organisation_id the organisation concerned
     * @param url the document's URL
     * @param sentence_id the id of the sentence where value is concerned
     * @param value the value of the emotion from -1.0 to 1.0 (negative to positive)
     * @param acl_hash the security acl for this item
     */
    public void indexEmotion( UUID organisation_id, String url, int sentence_id, double value, int acl_hash) {
        db.indexEmotion(organisation_id, url, sentence_id, value, acl_hash);
    }

    /**
     * read a set of emotions (sentence indexes with values) for a given document
     * @param organisation_id the organisation concerned
     * @param url the url of the document concerned
     * @return a set of emotions describing the sentences of a document,
     *         return a set even if its empty for valid parameters
     */
    public EmotionalSet getEmotionSet( UUID organisation_id, String url ) {
        return db.getEmotionSet( organisation_id, url );
    }

    /**
     * add a list of time indexes to the system
     * @param organisation_id the organisation to add for
     * @param dateTimeList a list of time indexes to add
     */
    public void addTimeIndexes(UUID organisation_id, List<TimeIndex> dateTimeList) {
        db.addTimeIndexes(organisation_id, dateTimeList);
    }

    /**
     * read time indexes for different resolutions
     * @param organisation_id the organisation to read from
     * @param year the year of the indexes
     * @param month the month of the indexes
     * @return a list of indexes that satisfies that criteria
     */
    public List<TimeIndex> readTimeIndexes(UUID organisation_id, int year, int month ) {
        return db.readTimeIndexes(organisation_id, year, month);
    }

    /**
     * read time indexes for different resolutions
     * @param organisation_id the organisation to read from
     * @param year the year of the indexes
     * @param month the month of the indexes
     * @param day the day of the indexes
     * @return a list of indexes that satisfies that criteria
     */
    public List<TimeIndex> readTimeIndexes(UUID organisation_id, int year, int month, int day ) {
        return db.readTimeIndexes(organisation_id, year, month, day);
    }

    /**
     * read time indexes for different resolutions
     * @param organisation_id the organisation to read from
     * @param year the year of the indexes
     * @param month the month of the indexes
     * @param day the day of the indexes
     * @param hour the hour of the indexes
     * @return a list of indexes that satisfies that criteria
     */
    public List<TimeIndex> readTimeIndexes(UUID organisation_id, int year, int month, int day, int hour ) {
        return db.readTimeIndexes(organisation_id, year, month, day, hour);
    }

    /**
     * given a set of time ranges select all indexes in these sets
     * @param organisation_id the organsiation for the selection
     * @param set the set of ranges to select
     * @return the indexes for the selected ranges or null
     */
    public List<TimeIndex> getIndexListForRange( UUID organisation_id, TimeSelectorSetWithBoundaries set ) {

        if ( set != null && set.getTimeIndexSelectorList().size() > 0 ) {

            List<TimeIndex> indexList = new ArrayList<>();
            for ( TimeIndexSelector timeIndexSelector : set.getTimeIndexSelectorList() ) {

                if ( timeIndexSelector.timeSelectorForYearMonth() ) {
                    List<TimeIndex> indexList1 = readTimeIndexes(organisation_id, timeIndexSelector.getYear(), timeIndexSelector.getMonth() );
                    if ( indexList1 != null ) {
                        indexList.addAll(indexList1);
                    }
                }

                else if ( timeIndexSelector.timeSelectorForDays() ) {
                    List<TimeIndex> indexList1 = readTimeIndexes(organisation_id, timeIndexSelector.getYear(), timeIndexSelector.getMonth(), timeIndexSelector.getDay() );
                    if ( indexList1 != null ) {
                        indexList.addAll(indexList1);
                    }
                }


                else if ( timeIndexSelector.timeSelectorForHours() ) {
                    List<TimeIndex> indexList1 = readTimeIndexes(organisation_id, timeIndexSelector.getYear(),
                            timeIndexSelector.getMonth(), timeIndexSelector.getDay(), timeIndexSelector.getHour() );
                    if ( indexList1 != null ) {
                        indexList.addAll(indexList1);
                    }
                }

            }

            // filter by time - to singe
            if ( indexList.size() > 0 ) {
                // filter by url and time - no duplicates
                Map<String, TimeIndex> timeFiltered = new HashMap<>();
                for ( TimeIndex index : indexList ) {
                    // filter by outlier times too
                    long indexTime = index.getDate_time();
                    if ( set.getTime1() <= indexTime && indexTime <= set.getTime2() ) {
                        timeFiltered.put(index.getUrl() + ":" + index.getDate_time(), index);
                    }
                }
                indexList.clear();
                indexList.addAll( timeFiltered.values() );
                return indexList;
            }
        }
        return null;
    }

    /**
     * get all previous items up to a number of years into the past relative to the given date/time
     * @param year2 the year to end at
     * @param month2 the month to end at
     * @param day2 the day to end at
     * @param hour2 the hour to end at
     * @param min2 the minute to end at
     * @param yearsIntoThePast the number of years to go into the past (always starts in the past at yyyy/01/1 00:00
     * @return the range of times that covers it
     */
    public TimeSelectorSetWithBoundaries getTimeSelectorsBefore( int year2, int month2, int day2, int hour2, int min2, int yearsIntoThePast ) {

        int year1 = year2 - yearsIntoThePast; // up to x years into the past
        int month1 = 1;
        int day1 = 1;
        int hour1 = 0;
        int min1 = 0;

        if ( month2 == -1 ) {
            month2 = 1;
        }
        long time1 = new DateTime(year1, month1, day1, hour1, min1).toDate().getTime();

        long time2;
        if ( day2 == -1 && hour2 == -1 ) {
            time2 = new DateTime(year2, month2, 1, 0, 0).toDate().getTime();
            day1 = -1;
            hour1 = -1;
        } else if ( hour2 == -1 ){
            time2 = new DateTime(year2, month2, day2, 0, 0).toDate().getTime();
            hour1 = -1;
        } else {
            time2 = new DateTime(year2, month2, day2, hour2, min2).toDate().getTime();
        }

        List<TimeIndexSelector> totalSet = new ArrayList<>();
        for ( int year = year1; year <= year2; year++ ) {

            int startMonth = 1;
            if ( year == year1) {
                startMonth = month1;
            }
            int endMonth = 12;
            if ( year == year2) {
                endMonth = month2;
            }

            for ( int month = startMonth; month <= endMonth; month++ ) {

                if ( year == year1 && month == month1 ) {
                    if ( day1 == -1 ) {
                        // whole month?
                        totalSet.add( new TimeIndexSelector(year, month) );
                    } else if ( hour1 == -1 ) {
                        // all days of the month from day1
                        int endDay = daysForMonth(year1, month1);
                        if ( year == year2 && month == month2 && day2 != -1) {
                            endDay = day2;
                        }
                        for ( int day = day1; day <= endDay; day++ ) {
                            totalSet.add( new TimeIndexSelector(year, month, day) );
                        }
                    } else {
                        int endDay = daysForMonth(year1, month1);
                        if ( year == year2 && month == month2 && day2 != -1) {
                            endDay = day2;
                        }
                        for ( int day = day1; day <= endDay; day++ ) {
                            // first day?
                            if ( day == day1 ) {
                                int endHour = 23;
                                if ( year == year2 && month == month2 && day2 == day && hour2 != -1 ) {
                                    endHour = hour2;
                                }
                                if ( hour1 == 0 && endHour == 23 ) {
                                    totalSet.add(new TimeIndexSelector(year, month, day, -1));
                                } else {
                                    for (int hour = hour1; hour <= endHour; hour++) {
                                        totalSet.add(new TimeIndexSelector(year, month, day, hour));
                                    }
                                }
                            } else {
                                totalSet.add( new TimeIndexSelector(year, month, day) );
                            }
                        }
                    }
                }

                else if ( year == year2 && month == month2 ) {
                    if ( day2 == -1 ) {
                        // whole month?
                        totalSet.add( new TimeIndexSelector(year, month) );
                    } else if ( hour2 == -1 ) {
                        // all days of the month from day1
                        int startDay = 1;
                        if ( year == year1 && month == month1 && day1 != -1) {
                            startDay = day1;
                        }
                        for ( int day = startDay; day <= day2; day++ ) {
                            totalSet.add( new TimeIndexSelector(year, month, day) );
                        }
                    } else {
                        int startDay = 1;
                        if ( year == year1 && month == month1 && day1 != -1) {
                            startDay = day1;
                        }
                        for ( int day = startDay; day <= day2; day++ ) {
                            // last day?
                            if ( day == day2 ) {
                                int startHour = 0;
                                if ( year == year1 && month == month1 && day1 == day && hour1 != -1 ) {
                                    startHour = hour1;
                                }
                                if ( startHour == 0 && hour2 == 23 ) {
                                    totalSet.add(new TimeIndexSelector(year, month, day, -1));
                                } else {
                                    for (int hour = startHour; hour <= hour2; hour++) {
                                        totalSet.add(new TimeIndexSelector(year, month, day, hour));
                                    }
                                }
                            } else {
                                totalSet.add( new TimeIndexSelector(year, month, day) );
                            }
                        }
                    }
                }

                else {
                    // whole month?
                    totalSet.add( new TimeIndexSelector(year, month) );
                }
            }
        }
        return new TimeSelectorSetWithBoundaries(time1, time2, totalSet);
    }


    /**
     * get all previous items up to a number of years into the future relative to the given date/time
     * @param year1 the year to start at
     * @param month1 the month to start at
     * @param day1 the day to start at
     * @param hour1 the hour to start at
     * @param min1 the minute to start at
     * @param yearsIntoTheFuture the number of years to go into the future, always ending at yyyy/12/31 23:59
     * @return the range of times that covers it
     */
    public TimeSelectorSetWithBoundaries getTimeSelectorsAfter( int year1, int month1, int day1, int hour1, int min1, int yearsIntoTheFuture ) {
        int year2 = year1 + yearsIntoTheFuture; // up to x years into the future
        int month2 = 12;
        int day2 = 31;
        int hour2 = 23;
        int min2 = 59;

        if ( month1 == -1 ) {
            month1 = 1;
        }
        long time2 = new DateTime(year2, month2, day2, hour2, min2).toDate().getTime();

        long time1;
        if ( day1 == -1 && hour1 == -1 ) {
            time1 = new DateTime(year1, month1, 1, 0, 0).toDate().getTime();
        } else if ( hour1 == -1 ){
            time1 = new DateTime(year1, month1, day1, 0, 0).toDate().getTime();
        } else {
            time1 = new DateTime(year1, month1, day1, hour1, min1).toDate().getTime();
        }

        List<TimeIndexSelector> totalSet = new ArrayList<>();
        for ( int year = year1; year <= year2; year++ ) {

            int startMonth = 1;
            if ( year == year1) {
                startMonth = month1;
            }
            int endMonth = 12;
            if ( year == year2) {
                endMonth = month2;
            }

            for ( int month = startMonth; month <= endMonth; month++ ) {

                if ( year == year1 && month == month1 ) {
                    if ( day1 == -1 ) {
                        // whole month?
                        totalSet.add( new TimeIndexSelector(year, month) );
                    } else if ( hour1 == -1 ) {
                        // all days of the month from day1
                        int endDay = daysForMonth(year1, month1);
                        if ( year == year2 && month == month2 && day2 != -1) {
                            endDay = day2;
                        }
                        for ( int day = day1; day <= endDay; day++ ) {
                            totalSet.add( new TimeIndexSelector(year, month, day) );
                        }
                    } else {
                        int endDay = daysForMonth(year1, month1);
                        if ( year == year2 && month == month2 && day2 != -1) {
                            endDay = day2;
                        }
                        for ( int day = day1; day <= endDay; day++ ) {
                            // first day?
                            if ( day == day1 ) {
                                int endHour = 23;
                                if ( year == year2 && month == month2 && day2 == day && hour2 != -1 ) {
                                    endHour = hour2;
                                }
                                if ( hour1 == 0 && endHour == 23 ) {
                                    totalSet.add(new TimeIndexSelector(year, month, day, -1));
                                } else {
                                    for (int hour = hour1; hour <= endHour; hour++) {
                                        totalSet.add(new TimeIndexSelector(year, month, day, hour));
                                    }
                                }
                            } else {
                                totalSet.add( new TimeIndexSelector(year, month, day) );
                            }
                        }
                    }
                }

                else if ( year == year2 && month == month2 ) {
                    if ( day2 == -1 ) {
                        // whole month?
                        totalSet.add( new TimeIndexSelector(year, month) );
                    } else if ( hour2 == -1 ) {
                        // all days of the month from day1
                        int startDay = 1;
                        if ( year == year1 && month == month1 && day1 != -1) {
                            startDay = day1;
                        }
                        for ( int day = startDay; day <= day2; day++ ) {
                            totalSet.add( new TimeIndexSelector(year, month, day) );
                        }
                    } else {
                        int startDay = 1;
                        if ( year == year1 && month == month1 && day1 != -1) {
                            startDay = day1;
                        }
                        for ( int day = startDay; day <= day2; day++ ) {
                            // last day?
                            if ( day == day2 ) {
                                int startHour = 0;
                                if ( year == year1 && month == month1 && day1 == day && hour1 != -1 ) {
                                    startHour = hour1;
                                }
                                if ( startHour == 0 && hour2 == 23 ) {
                                    totalSet.add(new TimeIndexSelector(year, month, day, -1));
                                } else {
                                    for (int hour = startHour; hour <= hour2; hour++) {
                                        totalSet.add(new TimeIndexSelector(year, month, day, hour));
                                    }
                                }
                            } else {
                                totalSet.add( new TimeIndexSelector(year, month, day) );
                            }
                        }
                    }
                }

                else {
                    // whole month?
                    totalSet.add( new TimeIndexSelector(year, month) );
                }
            }
        }
        return new TimeSelectorSetWithBoundaries(time1, time2, totalSet);
    }


    /**
     * read time indexes for a range at year / month / day level granularity
     * @param year1 the start year
     * @param month1 the start month
     * @param day1 the start day
     * @param hour1 the day's start hour
     * @param year2 the end year
     * @param month2 the end month
     * @param day2 the end day
     * @param hour2 the day's last hour
     * @return a list of index selectors for the period
     */
    public TimeSelectorSetWithBoundaries getTimeSelectorsForRange( int year1, int month1, int day1, int hour1, int min1,
                                                                   int year2, int month2, int day2, int hour2, int min2 ) {

        // months must have valid selector values
        if ( month1 == -1 ) {
            month1 = 1;
        }
        if ( month2 == -1 ) {
            month2 = 12;
        }

        long time1;
        if ( day1 == -1 && hour1 == -1 ) {
            time1 = new DateTime(year1, month1, 1, 0, 0).toDate().getTime();
        } else if ( hour1 == -1 ){
            time1 = new DateTime(year1, month1, day1, 0, 0).toDate().getTime();
        } else {
            time1 = new DateTime(year1, month1, day1, hour1, min1).toDate().getTime();
        }

        long time2;
        if ( day2 == -1 && hour2 == -1 ) {
            time2 = new DateTime(year2, month2, daysForMonth(year2, month2), 23, 59, 59).toDate().getTime();
        } else if ( hour2 == -1 ){
            time2 = new DateTime(year2, month2, day2, 23, 59, 59).toDate().getTime();
        } else {
            time2 = new DateTime(year2, month2, day2, hour2, min2, 59).toDate().getTime();
        }

        List<TimeIndexSelector> totalSet = new ArrayList<>();
        for ( int year = year1; year <= year2; year++ ) {

            int startMonth = 1;
            if ( year == year1) {
                startMonth = month1;
            }
            int endMonth = 12;
            if ( year == year2) {
                endMonth = month2;
            }

            for ( int month = startMonth; month <= endMonth; month++ ) {

                if ( year == year1 && month == month1 ) {
                    if ( day1 == -1 ) {
                        // whole month?
                        totalSet.add( new TimeIndexSelector(year, month) );
                    } else if ( hour1 == -1 ) {
                        // all days of the month from day1
                        int endDay = daysForMonth(year1, month1);
                        if ( year == year2 && month == month2 && day2 != -1) {
                            endDay = day2;
                        }
                        for ( int day = day1; day <= endDay; day++ ) {
                            totalSet.add( new TimeIndexSelector(year, month, day) );
                        }
                    } else {
                        int endDay = daysForMonth(year1, month1);
                        if ( year == year2 && month == month2 && day2 != -1) {
                            endDay = day2;
                        }
                        for ( int day = day1; day <= endDay; day++ ) {
                            // first day?
                            if ( day == day1 ) {
                                int endHour = 23;
                                if ( year == year2 && month == month2 && day2 == day && hour2 != -1 ) {
                                    endHour = hour2;
                                }
                                if ( hour1 == 0 && endHour == 23 ) {
                                    totalSet.add(new TimeIndexSelector(year, month, day, -1));
                                } else {
                                    for (int hour = hour1; hour <= endHour; hour++) {
                                        totalSet.add(new TimeIndexSelector(year, month, day, hour));
                                    }
                                }
                            } else {
                                totalSet.add( new TimeIndexSelector(year, month, day) );
                            }
                        }
                    }
                }

                else if ( year == year2 && month == month2 ) {
                    if ( day2 == -1 ) {
                        // whole month?
                        totalSet.add( new TimeIndexSelector(year, month) );
                    } else if ( hour2 == -1 ) {
                        // all days of the month from day1
                        int startDay = 1;
                        if ( year == year1 && month == month1 && day1 != -1) {
                            startDay = day1;
                        }
                        for ( int day = startDay; day <= day2; day++ ) {
                            totalSet.add( new TimeIndexSelector(year, month, day) );
                        }
                    } else {
                        int startDay = 1;
                        if ( year == year1 && month == month1 && day1 != -1) {
                            startDay = day1;
                        }
                        for ( int day = startDay; day <= day2; day++ ) {
                            // last day?
                            if ( day == day2 ) {
                                int startHour = 0;
                                if ( year == year1 && month == month1 && day1 == day && hour1 != -1 ) {
                                    startHour = hour1;
                                }
                                if ( startHour == 0 && hour2 == 23 ) {
                                    totalSet.add(new TimeIndexSelector(year, month, day, -1));
                                } else {
                                    for (int hour = startHour; hour <= hour2; hour++) {
                                        totalSet.add(new TimeIndexSelector(year, month, day, hour));
                                    }
                                }
                            } else {
                                totalSet.add( new TimeIndexSelector(year, month, day) );
                            }
                        }
                    }
                }

                else {
                    // whole month?
                    totalSet.add( new TimeIndexSelector(year, month) );
                }
            }
        }
        return new TimeSelectorSetWithBoundaries(time1, time2, totalSet);
    }

    /**
     * return the number of days for a month
     * @param year the year
     * @param month the month
     * @return the number of days
     */
    private int daysForMonth(int year, int month) {
        switch (month) {
            case 2:
                if ( year % 4 == 0 )
                    return 29;
                return 28;
            case 1:
            case 3:
            case 5:
            case 7:
            case 8:
            case 10:
            case 12:
                return 31;
            default:
                return 30;
        }
    }

}

