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

package industries.vocht.viki.rules_engine;

import industries.vocht.viki.EventTypeEnum;
import industries.vocht.viki.document.Document;
import industries.vocht.viki.model.rules.RuleItem;
import industries.vocht.viki.model.rules.RuleItemBase;
import industries.vocht.viki.rules_engine.action.*;
import industries.vocht.viki.rules_engine.condition.*;
import industries.vocht.viki.rules_engine.events.*;
import industries.vocht.viki.rules_engine.model.ExecutableRule;
import industries.vocht.viki.rules_engine.model.RuleConversionException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 15/05/16.
 *
 * convert the storage only rule-item to a proper set of rule classes
 *
 */
public class ConvertFromRuleItem {

    public ConvertFromRuleItem() {
    }

    /**
     * convert a storage rule to a real executable rule with a more complex structure
     * @param item the item to convert
     * @return the converted equivalent
     */
    public ExecutableRule convert(RuleItem item ) throws RuleConversionException {
        if ( item != null ) {
            ExecutableRule executableRule = new ExecutableRule();
            executableRule.setOrganisation_id(item.getOrganisation_id());
            executableRule.setRule_name( item.getRule_name() );
            executableRule.setCreator( item.getCreator() );
            // convert the event list
            executableRule.setEventList( convertToEventList(item.getEvent_list()) );
            // convert a condition list
            executableRule.setConditionList( convertToConditionList(item.getCondition_list()) );
            // convert an action list
            executableRule.setActionList( convertToActionList(item.getAction_list()) );

            return executableRule;
        }
        return null;
    }

    /**
     * convert a list of events to proper events
     * @param ruleList the list of events to convert
     * @return a list of events
     */
    private List<IEvent> convertToEventList(List<RuleItemBase> ruleList )  throws RuleConversionException {
        if ( ruleList != null ) {
            List<IEvent> eventList = new ArrayList<>();
            for ( RuleItemBase item : ruleList ) {
                IEvent eventItem = convertToEvent( item );
                if ( eventItem != null ) {
                    eventList.add(eventItem);
                }
            }
            return eventList;
        }
        return null;
    }

    /**
     * convert a list of conditions to proper conditions
     * @param ruleList the list of conditions to convert
     * @return a list of conditions
     */
    private List<ICondition> convertToConditionList(List<RuleItemBase> ruleList )  throws RuleConversionException {
        if ( ruleList != null ) {
            List<ICondition> conditionList = new ArrayList<>();
            for ( RuleItemBase item : ruleList ) {
                ICondition condition = convertToCondition( item );
                if ( condition != null ) {
                    conditionList.add(condition);
                }
            }
            return conditionList;
        }
        return null;
    }

    /**
     * convert a list of actions to proper actions
     * @param ruleList the list of actions to convert
     * @return a list of actions
     */
    private List<IAction> convertToActionList(List<RuleItemBase> ruleList )  throws RuleConversionException {
        if ( ruleList != null ) {
            List<IAction> actionList = new ArrayList<>();
            for ( RuleItemBase item : ruleList ) {
                IAction action = convertToAction( item );
                if ( action != null ) {
                    actionList.add(action);
                }
            }
            return actionList;
        }
        return null;
    }

    /**
     * convert a single event item
     * @param item the item to convert
     * @return the converted item
     */
    private IEvent convertToEvent( RuleItemBase item )  throws RuleConversionException {
        if ( item != null && item.getType() != null ) {
            if ( item.getType().equals(EventTypeEnum.New_Document.getValue()) ) {
                return new EventNewDocument( item.getData().getOrigin_filter(), item.getData().getDocument_type_filter() );
            } else if ( item.getType().equals(EventTypeEnum.Interval.getValue()) ) {
                return new EventInterval( item.getData().getInterval(), item.getData().getInterval_unit() );
            } else if ( item.getType().equals(EventTypeEnum.Schedule.getValue()) ) {
                return new EventSchedule( item.getData().getTime_csv() );
            } else if ( item.getType().equals(EventTypeEnum.Manual.getValue()) ) {
                return new EventManual();
            } else {
                throw new RuleConversionException("unknown event type " + item.getType());
            }
        }
        return null;
    }

    /**
     * convert a single condition item
     * @param item the item to convert
     * @return the converted item
     */
    private ICondition convertToCondition( RuleItemBase item )  throws RuleConversionException {
        if ( item != null && item.getType() != null ) {
            if ( item.getType().equals(ConditionTypeEnum.DateRangeContent.getValue()) ) {
                return new ConditionDateRange(Document.META_BODY, item.getData().getTime_csv() );
            } else if ( item.getType().equals(ConditionTypeEnum.DateRangeCreated.getValue()) ) {
                return new ConditionDateRange(Document.META_CREATED_DATE_TIME, item.getData().getTime_csv() );
            } else if ( item.getType().equals(ConditionTypeEnum.ContentContainsWord.getValue()) ) {
                return new ConditionMetadataNameContainsWord( Document.META_BODY, item.getData().getWord_csv());
            } else if ( item.getType().equals(ConditionTypeEnum.TitleContainsWord.getValue()) ) {
                return new ConditionMetadataNameContainsWord( Document.META_TITLE, item.getData().getWord_csv());
            } else if ( item.getType().equals(ConditionTypeEnum.MetadataNameContainsWord.getValue()) ) {
                return new ConditionMetadataNameContainsWord( item.getData().getMetadata(), item.getData().getWord_csv());
            } else if ( item.getType().equals(ConditionTypeEnum.AuthorContainsWord.getValue()) ) {
                return new ConditionMetadataNameContainsWord( Document.META_AUTHOR, item.getData().getWord_csv());
            } else if ( item.getType().equals(ConditionTypeEnum.SummaryContainsWord.getValue()) ) {
                return new ConditionMetadataNameContainsWord( Document.META_SUMMARIZATION, item.getData().getWord_csv());
            } else if ( item.getType().equals(ConditionTypeEnum.WordStatsForSet.getValue()) ) {
                return new ConditionWordStats( item.getLogic(), ConditionWordSetType.SpecificWords, item.getData().getWord_csv() );
            } else if ( item.getType().equals(ConditionTypeEnum.NegativeContent.getValue()) ) {
                return new ConditionWordStats( item.getLogic(), ConditionWordSetType.NegativeWords, null );
            } else if ( item.getType().equals(ConditionTypeEnum.PositiveContent.getValue()) ) {
                return new ConditionWordStats( item.getLogic(), ConditionWordSetType.PositiveWords, null );
            } else if ( item.getType().equals(ConditionTypeEnum.SexualContent.getValue()) ) {
                return new ConditionWordStats( item.getLogic(), ConditionWordSetType.SexualContent, null );
            } else if ( item.getType().equals(ConditionTypeEnum.CloseDuplicate.getValue()) ) {
                return new ConditionDuplicates(item.getLogic());
            } else {
                throw new RuleConversionException("unknown condition type " + item.getType());
            }
        }
        return null;
    }

    /**
     * convert a single action item
     * @param item the item to convert
     * @return the converted item
     */
    private IAction convertToAction( RuleItemBase item )  throws RuleConversionException {
        if ( item != null && item.getType() != null ) {
            if ( item.getType().equals(ActionTypeEnum.Email.getValue()) ) {
                return new ActionEmail( item.getData().getTo(), item.getData().getSubject() );
            } else if ( item.getType().equals(ActionTypeEnum.Export.getValue()) ) {
                return new ActionExport( item.getData().getProtocol(), item.getData().getUrl(), item.getData().getPath(),
                                         item.getData().getUsername(), item.getData().getPassword(), item.getData().getDomain() );
            } else if ( item.getType().equals(ActionTypeEnum.ChangeDocumentSecurity.getValue()) ) {
                return new ActionChangeDocumentSecurity( item.getData().getValue() );
            } else if ( item.getType().equals(ActionTypeEnum.ChangeDocumentClassification.getValue()) ) {
                return new ActionChangeDocumentClassification( item.getData().getValue() );
            } else if ( item.getType().equals(ActionTypeEnum.AddMetadata.getValue()) ) {
                return new ActionPutMetadata( item.getData().getName(), item.getData().getValue() );
            } else if ( item.getType().equals(ActionTypeEnum.RemoveMetadata.getValue()) ) {
                return new ActionRemoveMetadata( item.getData().getName() );
            } else if ( item.getType().equals(ActionTypeEnum.StopProcessing.getValue()) ) {
                return new ActionStopProcessing();
            } else if ( item.getType().equals(ActionTypeEnum.RemoveDocument.getValue()) ) {
                return new ActionRemoveDocument();
            } else {
                throw new RuleConversionException("unknown action type " + item.getType());
            }
        }
        return null;
    }

}

