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

'use strict';

/**
 * @ngdoc function
 * @name webApp.controller:RuleEditorController
 * @description
 * # RuleEditorController
 * Controller of the webApp
 */
angular.module('webApp')
.controller('RuleEditorController', function ($scope, $cookies, $timeout, $location, globalSvc, documentEventSvc, timeIntervalSvc, timeTableSvc,
                                           dateTimeRangeSvc, wordBuilderSvc, wordSetSvc, addMetadataSvc, addFieldSvc,
                                           emailDocumentSvc, exportDocumentSvc, ruleSvc ) {

    var session = null;
    var email = null;

    $scope.rule_name = '';

    // save button text
    $scope.buttonText = "save";
    $scope.headerText = "create rule";
    $scope.tabText = "create rule";

    $scope.event_list = [];
    $scope.condition_list = [];
    $scope.action_list = [];

    var counter = 1; // id system

    /////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////
    // select events

    // the pretty name of the event
    $scope.event = '';
    // the data for an event
    $scope.eventData = null;

    $scope.selectEvent = function( eventStr, type ) {
      if ( eventStr ) {
          $scope.event_list = [];
          $scope.event_list.push( { 'description': eventStr, 'type': type} );
      }
    };

    $scope.newDocumentEvent = function() {
        documentEventSvc.show('new-document', $scope.newDocumentEventDone);
    };

    $scope.manual = function () {
        $scope.event_list = [];
        var obj = {};
        obj['type'] = 'manual';
        obj['description'] = "a manual / user activated event";
        $scope.event_list.push( obj );
    };

    $scope.newDocumentEventDone = function () {
        if ( documentEventSvc.result ) {
            $scope.event_list = [];
            var obj = documentEventSvc.result;
            obj['type'] = 'new-document';
            obj['description'] = documentEventSvc.prettyPrint();
            $scope.event_list.push( obj );
        }
    };

    $scope.removeEvent = function () {
        $scope.event_list = [];
    };

    $scope.selectInterval = function() {
        timeIntervalSvc.show($scope.selectIntervalDone);
    };

    $scope.selectIntervalDone = function() {
        if ( timeIntervalSvc.result && timeIntervalSvc.result.data ) {
            $scope.selectEvent( timeIntervalSvc.result.description, 'interval');
        }
    };

    $scope.selectTimeSchedule = function() {
        timeTableSvc.show($scope.selectTimeScheduleDone);
    };

    $scope.selectTimeScheduleDone = function() {
        if ( timeTableSvc.result && timeTableSvc.result.data ) {
            $scope.selectEvent(timeTableSvc.result.description, 'schedule');
        }
    };

    /////////////////////////////////////////////////////////////

    $scope.remove = function(list, id) {
        var newList = [];
        $.each(list, function(i, item) {
            if ( item && item.id && item.id != id ) {
                newList.push(item);
            }
        });
        return newList;
    };

    $scope.removeCondition = function(id) {
        $scope.condition_list = $scope.remove($scope.condition_list, id);
        if ( $scope.condition_list.length > 0 ) {
            var id = $scope.condition_list[$scope.condition_list.length - 1].id;
            $("#logic_" + id).hide();
        }
    };

    /////////////////////////////////////////////////////////////
    // conditions

    $scope.dateTimeRange = function (title, type) {
        if ( title && type ) {
            dateTimeRangeSvc.show(title, type, $scope.dateTimeRangeDone);
        }
    };

    $scope.dateTimeRangeDone = function () {
        if ( dateTimeRangeSvc.result ) {
            var obj = dateTimeRangeSvc.result;
            obj.id = counter;
            obj.logic = "and";
            counter = counter + 1;
            $scope.condition_list.push(obj);

            if ( $scope.condition_list.length > 1 ) {
                var id = $scope.condition_list[$scope.condition_list.length - 2].id;
                $("#logic_" + id).show();
            }

        }
    };

    $scope.wordBuilder = function(title, type, showMetadata) {
        wordBuilderSvc.show(title, type, showMetadata, $scope.wordBuilderDone);
    };

    $scope.wordBuilderDone = function() {
        if ( wordBuilderSvc.result ) {
            var obj = wordBuilderSvc.result;
            obj.id = counter;
            obj.logic = "and";
            counter = counter + 1;
            $scope.condition_list.push(obj);

            if ( $scope.condition_list.length > 1 ) {
                var id = $scope.condition_list[$scope.condition_list.length - 2].id;
                $("#logic_" + id).show();
            }
        }
    };

    $scope.wordSet = function () {
        wordSetSvc.show($scope.wordSetCallback);
    };

    $scope.wordSetCallback = function () {
        if ( wordSetSvc.result ) {
            wordSetSvc.result.id = counter;
            wordSetSvc.result.type = 'word-statistics';
            wordSetSvc.logic = "and";
            counter = counter + 1;
            $scope.condition_list.push(wordSetSvc.result);

            if ( $scope.condition_list.length > 1 ) {
                var id = $scope.condition_list[$scope.condition_list.length - 2].id;
                $("#logic_" + id).show();
            }
        }
    };

    $scope.addCondition = function(description, type) {
        if ( description && type ) {
            $scope.condition_list.push( { 'id': counter, 'description': description, 'type': type } );
            counter = counter + 1;

            if ( $scope.condition_list.length > 1 ) {
                var id = $scope.condition_list[$scope.condition_list.length - 2].id;
                $("#logic_" + id).show();
            }
        }
    };

    /////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////
    // actions

    $scope.removeAction = function(id) {
        $scope.action_list = $scope.remove($scope.action_list, id);
    };

    $scope.addAction = function(description, type) {
        if ( description && type ) {
            $scope.action_list.push( { 'id': counter, 'description': description, 'type': type } );
            counter = counter + 1;
        }
    };

    $scope.addField = function( description, type, label, placeholder ) {
        if ( description && type && label && placeholder ) {
            addFieldSvc.show(description, type, label, placeholder, $scope.addFieldComplete);
        }
    };

    $scope.addFieldComplete = function() {
        if ( addFieldSvc.result && addFieldSvc.type ) {
            var obj = addFieldSvc.result;
            obj.id = counter;
            $scope.action_list.push( obj );
            counter = counter + 1;
        }
    };

    $scope.addMetadata  = function() {
        addMetadataSvc.show('add-metadata', $scope.addMetadataComplete);
    };

    $scope.addMetadataComplete = function() {
        if ( addMetadataSvc.result ) {
            var obj = addMetadataSvc.result;
            obj.id = counter;
            $scope.action_list.push( obj );
            counter = counter + 1;
        }
    };

    $scope.removeMetadata  = function() {
        addMetadataSvc.show('remove-metadata', $scope.removeMetadata_Complete);
    };

    $scope.removeMetadata_Complete = function() {
        if ( addMetadataSvc.result ) {
            var obj = addMetadataSvc.result;
            obj.id = counter;
            $scope.action_list.push( obj );
            counter = counter + 1;
        }
    };

    $scope.emailDocument = function() {
        emailDocumentSvc.show($scope.emailDocumentComplete);
    };

    $scope.emailDocumentComplete = function() {
        if ( emailDocumentSvc.result ) {
            var obj = emailDocumentSvc.result;
            obj.type = 'email';
            obj.id = counter;
            $scope.action_list.push( obj );
            counter = counter + 1;
        }
    };

    $scope.exportDocument = function() {
        exportDocumentSvc.show($scope.exportDocumentComplete);
    };

    $scope.exportDocumentComplete = function() {
        if ( exportDocumentSvc.result ) {
            var obj = exportDocumentSvc.result;
            obj.type = 'export';
            obj.id = counter;
            $scope.action_list.push( obj );
            counter = counter + 1;
        }
    };

    /////////////////////////////////////////////////////////////

    $scope.cleanList = function( list ) {
        if ( list ) {
            $.each(list, function(i, value) {
                delete value['$$hashKey'];
            });
        }
        return list;
    };

    $scope.save = function() {
        if ( $scope.rule_name.length > 0 && $scope.event_list.length > 0 &&
             $scope.condition_list.length > 0 && $scope.action_list.length > 0 ) {

            console.log("saving rule \"" + $scope.rule_name + "\"");

            var obj = {};
            obj.rule_name = $scope.rule_name;
            obj.creator = email;
            obj.event_list = $scope.cleanList($scope.event_list);
            obj.condition_list = $scope.cleanList($scope.condition_list);
            obj.action_list = $scope.cleanList($scope.action_list);

            if ( $scope.buttonText == "save" ) {
                ruleSvc.createRule(session, obj, $scope.saveDone);
            } else {
                ruleSvc.updateRule(session, obj, $scope.saveDone);
            }
        } else {
            globalSvc.error_message("not all fields are complete");
        }
    };

    $scope.saveDone = function() {
        $location.path("/rules");
    };

    $scope.cancel = function () {
        ruleSvc.rule = null;
        $location.path("/rules");
    };

    // set id's logic to ... logic
    $scope.setLogic = function(id, logic) {
        var obj = globalSvc.getObjectFromListByID($scope.condition_list, 'id', id);
        if ( obj ) {
            obj.logic = logic;
        }
    };

    // signed in?
    globalSvc.getSession( function(pSession) {
        if ( !pSession ) {
            globalSvc.goHome();
        } else {
            session = pSession;
        } },
        function(user) {
            if ( user ) {
                email = user.email;
            }
        }
    );

    // is this an edit or a create?
    if ( ruleSvc.rule != null ) {
        // setup for edit
        $scope.rule_name = ruleSvc.rule.rule_name;
        $scope.event_list = ruleSvc.rule.event_list;
        $scope.condition_list = ruleSvc.rule.condition_list;
        $.each($scope.condition_list, function(i, item) {
            item.id = counter;
            counter = counter + 1;
        });
        $scope.action_list = ruleSvc.rule.action_list;
        $.each($scope.action_list, function(i, item) {
            item.id = counter;
            counter = counter + 1;
        });
        $scope.buttonText = "update";
        $scope.headerText = "update existing rule";
        $scope.tabText = "update rule";
        $("#btnCancel").show();
        $("#txtRuleName")
            .attr("readonly", "true")
            .attr("disabled", "true");

        $timeout(function () {
            var size = $scope.condition_list.length;
            $.each($scope.condition_list, function (i, item) {
                var ctrl = "logic_" + item.id;
                if (i + 1 < size) {
                    $("#" + ctrl).show();
                }
                if ( item.logic == 'and' ) {
                    $("input:radio[name=" + ctrl + "]:nth(0)").attr('checked',true);
                } else if ( item.logic == 'or' ) {
                    $("input:radio[name=" + ctrl + "]:nth(1)").attr('checked',true);
                } else if ( item.logic == 'and not' ) {
                    $("input:radio[name=" + ctrl + "]:nth(2)").attr('checked',true);
                }
            });
        });

    } else {
        $scope.buttonText = "save";
        $scope.headerText = "create rule";
        $scope.tabText = "create rule";
        $("#btnCancel").hide();
        $("#txtRuleName")
            .removeAttr("readonly")
            .removeAttr("disabled");
    }


});

