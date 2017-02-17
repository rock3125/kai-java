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

import org.joda.time.DateTime;

import java.util.*;

/**
 * Created by peter on 20/04/16.
 *
 * a list of time/url items for a time-line
 *
 */
public class TimeUrlSet {

    private UUID organisation_id;
    private List<TimeUrl> timeUrlList;

    public TimeUrlSet() {
        this.timeUrlList = new ArrayList<>();
    }

    public void add( String ownerName, TimeIndex index ) {
        if ( index != null ) {
            long dateTime = index.getDate_time();
            DateTime dateTime1 = new DateTime(dateTime);
            timeUrlList.add( new TimeUrl(index.getUrl(), ownerName, dateTime,
                    dateTime1.getYear(), dateTime1.getMonthOfYear(), dateTime1.getDayOfMonth(),
                    dateTime1.getHourOfDay(), dateTime1.getMinuteOfHour(), dateTime1.getSecondOfMinute() ) );
        }
    }

    /**
     * sort the time url list by... time
     */
    public void sort() {
        Collections.sort(timeUrlList);
    }

    /**
     * paginate the time url list set
     * @param page the offset page
     * @param pageSize the number of items per page
     */
    public void paginate( int page, int pageSize ) {
        int startOffset = page * pageSize;
        int endOffset = startOffset + pageSize;
        List<TimeUrl> tempList = new ArrayList<>();
        for ( int i = startOffset; i < endOffset; i++ ) {
            if ( i < timeUrlList.size() ) {
                tempList.add( timeUrlList.get(i));
            }
        }
        timeUrlList = tempList;
    }

    /**
     * group all time entities by time and url
     */
    public void consolidateUrls(TimeGroupingEnum grouping) {
        Map<String, TimeUrl> groupSet = new HashMap<>();
        for ( TimeUrl timeUrl : timeUrlList ) {

            String pk = timeUrl.getEntity_name();

            int year = timeUrl.getYear();
            int month = timeUrl.getMonth();
            int day = timeUrl.getDay();
            int hour = timeUrl.getHour();

            switch (grouping) {
                case byMonth: {
                    pk = pk + ":" + year + ":" + month;
                    break;
                }
                case byDay: {
                    if ( day <= 0 ) day = 1;
                    pk = pk + ":" + year + ":" + month + ":" + day;
                    break;
                }
                default: {
                    if ( day <= 0 ) day = 1;
                    if ( hour < 0 ) hour = 0;
                    pk = pk + ":" + year + ":" + month + ":" + day + ":" + hour;
                    break;
                }
            }

            TimeUrl existing = groupSet.get(pk);
            if ( existing == null ) {
                groupSet.put( pk, timeUrl );
            } else {
                existing.getUrlList().addAll( timeUrl.getUrlList() );
            }

        }
        // add new grouptd items to this list
        timeUrlList.clear();
        timeUrlList.addAll( groupSet.values() );
    }


    public TimeUrlSet( UUID organisation_id ) {
        this.organisation_id = organisation_id;
        this.timeUrlList = new ArrayList<>();
    }

    public UUID getOrganisation_id() {
        return organisation_id;
    }

    public void setOrganisation_id(UUID organisation_id) {
        this.organisation_id = organisation_id;
    }

    public List<TimeUrl> getTimeUrlList() {
        return timeUrlList;
    }

    public void setTimeUrlList(List<TimeUrl> timeUrlList) {
        this.timeUrlList = timeUrlList;
    }

}
