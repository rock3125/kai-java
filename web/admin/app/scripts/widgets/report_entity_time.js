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
.controller('ReportEntityTime', function($scope, reportEntityTimeSvc) {

    $scope.protocol = '';
    $scope.url = '';
    $scope.path = '';
    $scope.username = '';
    $scope.password = '';
    $scope.domain = '';

    $scope.wordList = [];
    $scope.currentType = 'any';
    $scope.metadata = '';
    $scope.word = '';
    $scope.exact = false;

    ////////////////////////////////////////////////////////////////////////////////

    $scope.selectType = function(typeStr) {
        console.log("type set to " + typeStr);
        if ( typeStr ) {
            $scope.currentType = typeStr;
        }
    };

    $scope.remove = function(list, wordStr) {
        var newList = [];
        $.each(list, function(i, item) {
            if ( item && item.word && item.word != wordStr ) {
                newList.push(item);
            }
        });
        return newList;
    };

    $scope.contains = function(list, wordStr) {
        var found = false;
        $.each(list, function(i, item) {
            if ( item && item.word && item.word == wordStr ) {
                found = true;
            }
        });
        return found;
    };

    $scope.removeWord = function (wordStr) {
        $scope.wordList = $scope.remove($scope.wordList, wordStr);
    };

    $scope.prettyPrint = function() {
        var str = "";
        if ( wordBuilderSvc.showMetadata ) {
            str = str + $scope.metadata + " contains(";
        }
        var size = $scope.wordList.length;
        $.each($scope.wordList, function(i, item) {
            if ( item.word ) {
                if ( str.length > 0 ) {
                    str = str + " ";
                }
                str = str + item.word;
                if ( item.type && item.type != 'any' ) {
                    str = str + ":" + item.type;
                }
                if ( item.exact ) {
                    str = str + ":exact";
                }
                if ( i + 1 < size ) {
                    str = str + " " + item.logic;
                }
            }
        });
        if ( wordBuilderSvc.showMetadata ) {
            str = str + ")";
        }
        return str;
    };

    $scope.add = function(logicStr) {
        if ( $scope.word.length > 1 && !$scope.contains($scope.wordList, $scope.word) ) {
            $("#txtWord").val('');
            $scope.wordList.push({'logic': logicStr, 'word': $scope.word, 'exact': $scope.exact, 'type': $scope.currentType});
        }
    };

    
    ////////////////////////////////////////////////////////////////////////////////

    $scope.showModal = function() {
        $scope.protocol = '';
        $scope.url = '';
        $scope.path = '';
        $scope.username = '';
        $scope.password = '';
        $scope.domain = '';

        $("#ReportEntityTimeDialog").modal('show');
    };

    $scope.selectProto = function(protocol) {
        if ( protocol ) {
            $scope.protocol = protocol;
        }
    };

    $scope.closeModal = function () {
        $("#ReportEntityTimeDialog").modal('hide');
    };

    $scope.modalOK = function () {
        if ( $scope.protocol.length > 0 && $scope.url.length > 0 ) {

            reportEntityTimeSvc.result = {};
            reportEntityTimeSvc.result.data = {};
            reportEntityTimeSvc.result.data.protocol = $scope.protocol;
            reportEntityTimeSvc.result.data.url = $scope.url;
            reportEntityTimeSvc.result.data.path = $scope.path;
            reportEntityTimeSvc.result.data.username = $scope.username;
            reportEntityTimeSvc.result.data.password = $scope.password;
            reportEntityTimeSvc.result.data.domain = $scope.domain;
            reportEntityTimeSvc.result.description = reportEntityTimeSvc.prettyPrint();

            $("#ReportEntityTimeDialog").modal('hide');
            if (reportEntityTimeSvc.callback) {
                reportEntityTimeSvc.callback();
            }
        }
    };

    if ( reportEntityTimeSvc ) {
        // setup callbacks
        reportEntityTimeSvc.showModal = $scope.showModal;
        reportEntityTimeSvc.closeModal = $scope.closeModal;
    }

})
.directive('reportEntityTime', function() {
    return {
        templateUrl: 'views/widgets/report_entity_time.html'
    };
});

