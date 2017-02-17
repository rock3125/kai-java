
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

angular.module('searchApp')


.service('advancedSearchSvc', function() {

    var service = this;

    // whoever wants a callback better put their name here
    // two parameters - 1: super query string to use, 2: callback for internal setup
    service.doSearchCallback = null;

    service.setup = function() {
    };

})

.controller('AdvancedSearchController', function($scope, advancedSearchSvc) {

    $scope.dateRangeType = 'exact';

    $scope.who = '';
    $scope.exact_who = false;
    $scope.where = '';
    $scope.exact_where = false;

    $scope.time1 = '';
    $scope.time2 = '';

    $scope.title = '';
    $scope.exact_title = false;
    $scope.summary = '';
    $scope.exact_summary = false;
    $scope.body = '';
    $scope.exact_body = false;
    $scope.url = '';

    // tweak ui accordingly
    $scope.selectDateRange = function(str) {
        $scope.dateRangeType = str;
        var hide = (str == "exact" || str == "before" || str == "after");
        if ( hide ) {
            $("#divAnd").hide();
            $("#divEndDate1").hide();
            $("#divEndTime1").hide();
        } else {
            $("#divAnd").show();
            $("#divEndDate1").show();
            $("#divEndTime1").show();
        }
    };

    $scope.getSuperSearchQuery = function() {
        var d1 = $('#date1').val();
        var d2 = $('#date2').val();

        var superSearchStr = '';

        if ( d1 != '' && $scope.time1 != '' ) {
            if ( d2 != '' && $scope.time2 != '' && $scope.dateRangeType == 'between' ) {
                superSearchStr = '(date between ' + d1 + ' ' + $scope.time1 + ' and ' + d2 + ' ' + $scope.time2 + ')';
            } else {
                superSearchStr = '(date ' + $scope.dateRangeType + ' ' + d1 + ' ' + $scope.time1 + ')';
            }
        } else if ( d1 != '' ) {
            if ( d2 != '' && $scope.dateRangeType == 'between' ) {
                superSearchStr = '(date between ' + d1 + ' and ' + d2 + ')';
            } else {
                superSearchStr = '(date ' + $scope.dateRangeType + ' ' + d1 + ')';
            }
        }

        if ( $scope.who != '' ) {
            if ( superSearchStr.length > 0 ) superSearchStr = superSearchStr + ' and ';
            superSearchStr = superSearchStr + '(';
            if ( $scope.exact_who ) superSearchStr = superSearchStr + 'exact ';
            superSearchStr = superSearchStr + 'person(' + $scope.who + '))';
        }
        if ( $scope.where != '' ) {
            if ( superSearchStr.length > 0 ) superSearchStr = superSearchStr + ' and ';
            superSearchStr = superSearchStr + '(';
            if ( $scope.exact_where ) superSearchStr = superSearchStr + 'exact ';
            superSearchStr = superSearchStr + 'location(' + $scope.where + '))';
        }
        if ( $scope.title != '' ) {
            if ( superSearchStr.length > 0 ) superSearchStr = superSearchStr + ' and ';
            superSearchStr = superSearchStr + '(';
            if ( $scope.exact_title ) superSearchStr = superSearchStr + 'exact ';
            superSearchStr = superSearchStr + 'title(' + $scope.title + '))';
        }
        if ( $scope.summary != '' ) {
            if ( superSearchStr.length > 0 ) superSearchStr = superSearchStr + ' and ';
            superSearchStr = superSearchStr + '(';
            if ( $scope.exact_summary ) superSearchStr = superSearchStr + 'exact ';
            superSearchStr = superSearchStr + 'summary(' + $scope.summary + '))';
        }
        if ( $scope.body != '' ) {
            if ( superSearchStr.length > 0 ) superSearchStr = superSearchStr + ' and ';
            superSearchStr = superSearchStr + '(';
            if ( $scope.exact_body ) superSearchStr = superSearchStr + 'exact ';
            superSearchStr = superSearchStr + 'body(' + $scope.body + '))';
        }
        if ( $scope.url != '' ) {
            if ( superSearchStr.length > 0 ) superSearchStr = superSearchStr + ' and ';
            superSearchStr = superSearchStr + '(';
            superSearchStr = superSearchStr + 'url(' + $scope.url + '))';
        }
        if ( superSearchStr.length > 0 ) {
            return "(" + superSearchStr + ")";
        }
        return null;
    };


    // ui click search
    $scope.search = function() {
        var queryStr = $scope.getSuperSearchQuery();
        if ( queryStr && advancedSearchSvc.doSearchCallback ) {
            advancedSearchSvc.doSearchCallback( queryStr, $scope.searchDone );
        }
    };

    // callback
    $scope.searchDone = function(data) {
        if ( data ) {
        }
    };

    // setup controls
    $('#datetimepicker1').datetimepicker({viewMode:'years', format: 'YYYY/MM/DD'});
    $('#datetimepicker2').datetimepicker({viewMode:'years', format: 'YYYY/MM/DD'});


})

.directive('advancedSearch', function() {
    return {
        templateUrl: 'views/widgets/advanced_search_widget.html'
    };
});

