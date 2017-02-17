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

package industries.vocht.viki.rules_engine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import industries.vocht.viki.rules_engine.action.IAction;
import industries.vocht.viki.rules_engine.condition.ICondition;
import industries.vocht.viki.rules_engine.events.EventInterval;
import industries.vocht.viki.rules_engine.events.EventSchedule;
import industries.vocht.viki.rules_engine.events.IEvent;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 15/05/16.
 *
 * the actual (single) rule for the rule engine
 * created from a ruleItem using ConvertFromRuleItem
 *
 */
public class ExecutableRule {

    private String rule_name;
    private UUID organisation_id;
    private String creator;

    private long timeStart; // for intervals - handy storage

    private List<IEvent> eventList;
    private List<ICondition> conditionList;
    private List<IAction> actionList;

    public ExecutableRule() {
    }

    @JsonIgnore
    public boolean isIntervalEvent() {
        if ( eventList != null && eventList.size() == 1 ) {
            IEvent event = eventList.get(0);
            return (event != null && event instanceof EventInterval);
        }
        return false;
    }

    // can this item execute now?
    public boolean canExecuteOnSchedule() {
        if ( eventList != null && eventList.size() == 1 ) {
            IEvent event = eventList.get(0);
            if ( event != null && event instanceof EventSchedule ) {
                EventSchedule schedule = (EventSchedule)event;
                DateTime date = new DateTime(new Date(System.currentTimeMillis()));
                String str = "";
                switch ( date.getDayOfWeek() ) {
                    case DateTimeConstants.MONDAY: { str = "mon-"; break; }
                    case DateTimeConstants.TUESDAY: { str = "tue-"; break; }
                    case DateTimeConstants.WEDNESDAY: { str = "wed-"; break; }
                    case DateTimeConstants.THURSDAY: { str = "thu-"; break; }
                    case DateTimeConstants.FRIDAY: { str = "fri-"; break; }
                    case DateTimeConstants.SATURDAY: { str = "sat-"; break; }
                    case DateTimeConstants.SUNDAY: { str = "sun-"; break; }
                }
                int hour = date.getHourOfDay();
                if ( hour < 10 ) {
                    str = str + "0";
                }
                str = str + hour;
                return ( schedule.getTime_csv() != null && schedule.getTime_csv().contains(str) && timeStart == 0L );
            }
        }
        return false; // not ready to execute
    }

    // can this item execute now?
    public boolean canExecuteOnInterval() {
        if ( eventList != null && eventList.size() == 1 ) {
            IEvent event = eventList.get(0);
            if ( event != null && event instanceof EventInterval ) {
                EventInterval interval = (EventInterval)event;
                long time = interval.intervalToMilliseconds();
                long now = System.currentTimeMillis();
                return ( now - timeStart ) >= time;
            }
        }
        return false; // not ready to execute
    }

    public String getRule_name() {
        return rule_name;
    }

    public void setRule_name(String rule_name) {
        this.rule_name = rule_name;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public List<IEvent> getEventList() {
        return eventList;
    }

    public void setEventList(List<IEvent> eventList) {
        this.eventList = eventList;
    }

    public List<ICondition> getConditionList() {
        return conditionList;
    }

    public void setConditionList(List<ICondition> conditionList) {
        this.conditionList = conditionList;
    }

    public List<IAction> getActionList() {
        return actionList;
    }

    public void setActionList(List<IAction> actionList) {
        this.actionList = actionList;
    }

    public long getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(long timeStart) {
        this.timeStart = timeStart;
    }

    public UUID getOrganisation_id() {
        return organisation_id;
    }

    public void setOrganisation_id(UUID organisation_id) {
        this.organisation_id = organisation_id;
    }
}

