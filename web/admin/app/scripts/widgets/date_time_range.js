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

angular.module('webApp')
.controller('DateTimeRange', function($scope, dateTimeRangeSvc) {

    $scope.t1 = '';
    $scope.t2 = '';
    $scope.dateTimeList = [];
    $scope.dateRangeType = 'exact';
    $scope.dateLogic = 'and';

    $scope.showModal = function() {
        if ( dateTimeRangeSvc.title ) {
            $("#DateTimeRangeDialogTitle").html(dateTimeRangeSvc.title);
        }
        $("#DateTimeRangeDialog").modal('show');
    };

    $scope.selectLogic = function(str) {
        $scope.dateLogic = str;
    };

    // tweak ui accordingly
    $scope.selectDateRange = function(str) {
        $scope.dateRangeType = str;
        var hide = (str == "exact" || str == "before" || str == "after");
        if ( hide ) {
            $("#divEndDate1").hide();
            $("#divEndDate2").hide();
            $("#divEndTime1").hide();
            $("#divEndTime2").hide();
        } else {
            $("#divEndDate1").show();
            $("#divEndDate2").show();
            $("#divEndTime1").show();
            $("#divEndTime2").show();
        }
    };

    $scope.closeModal = function () {
        dateTimeRangeSvc.timeSet = [];
        $("#DateTimeRangeDialog").modal('hide');
    };

    $scope.modalOK = function () {
        if ( $scope.dateTimeList.length > 0 ) {
            var list_csv = "";
            $.each($scope.dateTimeList, function(i, item) {
                if ( list_csv.length > 0 ) {
                    list_csv = list_csv + "|";
                }
                list_csv = list_csv + item.logicStr;
                delete item.logicStr;
                delete item.prettyStr;
            });
            // set the result object
            dateTimeRangeSvc.result = {};
            dateTimeRangeSvc.result.data = {};
            dateTimeRangeSvc.result.data.time_csv = list_csv;
            dateTimeRangeSvc.result.description = $scope.prettyPrint();
            dateTimeRangeSvc.result.type = dateTimeRangeSvc.type;

            $("#DateTimeRangeDialog").modal('hide');
            if (dateTimeRangeSvc.callback) {
                dateTimeRangeSvc.callback();
            }
        }
    };

    $scope.contains = function(d1,d2) {
        var found = false;
        $.each($scope.dateTimeList, function (i, item) {
            if ( d1 && item.date1 && d1 == item.date1 ) {
                if ( d2 && item.date2 && d2 == item.date2 ) {
                    found = true;
                } else if ( !d2 ) {
                    found = true;
                }
            }
        });
        return found;
    };

    // add a result after its been parsed
    $scope.addTimeResult = function(type, logic, d1, d2) {
        if ( type && !$scope.contains(d1,d2) ) {
            var logicStr = '';
            var prettyStr = '';
            if ( d2 ) {
                prettyStr = type + ' ' + d1 + ' and ' + d2;
                logicStr = type + ',' + d1 + ',' + d2;
            } else {
                prettyStr = type + ' ' + d1;
                logicStr = type + ',' + d1 + ',';
            }
            if ( logic ) {
                prettyStr = logic + ' ' + prettyStr;
                logicStr = logic  + ',' + logicStr;
            }
            $scope.dateTimeList.push( {'logicStr': logicStr, 'prettyStr': prettyStr, 'logic': logic, 'date1': d1, 'date2': d2, 'type': type} );
        }
    };

    $scope.clearTime = function() {
        $scope.dateTimeList = [];
        $("#divLogic1").hide();
        $("#divLogic2").hide();
    };

    // add the selected time
    $scope.addTime = function() {
        var d1 = $('#date1').val();
        var d2 = $('#date2').val();
        var t1 = $("#time1").val();
        var t2 = $("#time2").val();

        $("#divLogic1").show();
        $("#divLogic2").show();

        var type = $scope.dateRangeType;
        var logic = $scope.dateLogic;
        if ( $scope.dateTimeList.length == 0 ) {
            logic = null;
        }
        if ( type == 'exact' || type == 'before' || type == 'after' ) {
            if ( t1 != '' ) {
                $scope.addTimeResult(type, logic, d1 + ' ' + t1, null);
            } else {
                $scope.addTimeResult(type, logic, d1, null);
            }
        } else {
            if (t1 != '' && t2 != '') {
                $scope.addTimeResult(type, logic, d1 + ' ' + t1, d2 + ' ' + t2);
            } else {
                $scope.addTimeResult(type, logic, d1, d2);
            }
        }
    };

    $scope.prettyPrint = function() {
        var description = '';
        $.each($scope.dateTimeList, function(i, item) {
            var prettyStr = '';
            if ( item.date2 ) {
                prettyStr = item.type + ' ' + item.date1 + ' and ' + item.date2;
            } else {
                prettyStr = item.type + ' ' + item.date1;
            }
            if ( item.logic ) {
                prettyStr = ' (' + item.logic + ' ' + prettyStr + ')';
            }
            description = description + prettyStr;
        });
        return description;
    };

    ////////////////////////////////////////////////////////////////////////////////

    if ( dateTimeRangeSvc ) {
        // setup callbacks
        dateTimeRangeSvc.showModal = $scope.showModal;
        dateTimeRangeSvc.closeModal = $scope.closeModal;
    }

    // setup controls
    $('#datetimepicker1').datetimepicker({viewMode:'years', format: 'YYYY/MM/DD'});
    $('#datetimepicker2').datetimepicker({viewMode:'years', format: 'YYYY/MM/DD'});

})
.directive('dateTime', function() {
    return {
        templateUrl: 'views/widgets/date_time_range.html'
    };
});

