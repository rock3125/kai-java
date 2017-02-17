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
.controller('TimeTable', function($scope, timeTableSvc) {

    $scope.select = function( time ) {
        var str = time.split("-");
        if ( str.length == 2 ) {
            $scope.flipCell(time);
        } else {
            if ( str == 'mon' || str == 'tue' || str == 'wed' || str == 'thu' || str == 'fri' || str == 'sat' || str == 'sun' ) {
                for ( var i = 0; i < 24; i++ ) {
                    $scope.flipCell(str + "-" + i);
                }
            } else {
                $scope.flipCell('mon-' + time);
                $scope.flipCell('tue-' + time);
                $scope.flipCell('wed-' + time);
                $scope.flipCell('thu-' + time);
                $scope.flipCell('fri-' + time);
                $scope.flipCell('sat-' + time);
                $scope.flipCell('sun-' + time);
            }
        }
    };

    $scope.flipCell = function(time) {
        if ( !timeTableSvc.timeGrid[time] ) {
            timeTableSvc.timeGrid[time] = true;
            $("#" + time).css('background-color', 'red');
        } else {
            timeTableSvc.timeGrid[time] = null;
            $("#" + time).css('background-color', 'transparent');
        }
    };

    $scope.setCell = function(time) {
        $("#" + time).css('background-color', 'red');
    };

    $scope.showModal = function() {
        $("#TimeTableDialog").modal('show');
    };


    $scope.closeModal = function () {
        timeTableSvc.result = null;
        $("#TimeTableDialog").modal('hide');
    };

    // return the size of the timeTableSvc.timeGrid
    $scope.size = function() {
        var size = 0;
        $.each(timeTableSvc.timeGrid, function(name,value) {
            if ( value ) {
                size = size + 1;
            }
        });
        return size;
    };

    $scope.modalOK = function () {
        if ( timeTableSvc.timeGrid && $scope.size() > 0 ) {

            timeTableSvc.result = {};
            timeTableSvc.result.data = {};
            timeTableSvc.result.data.time_csv = timeTableSvc.getDataAsString();
            timeTableSvc.result.description = timeTableSvc.prettyPrint();

            $("#TimeTableDialog").modal('hide');

            if (timeTableSvc.callback) {
                timeTableSvc.callback();
            }
        }
    };

    // setup callbacks
    timeTableSvc.showModal = $scope.showModal;
    timeTableSvc.closeModal = $scope.closeModal;

    if ( timeTableSvc && timeTableSvc.timeGrid ) {
        $.each( timeTableSvc.timeGrid, function( name, value ) {
            $scope.setCell(name);
        });
    }

})
.directive('timeTable', function() {
    return {
        templateUrl: 'views/widgets/time_table.html'
    };
});

