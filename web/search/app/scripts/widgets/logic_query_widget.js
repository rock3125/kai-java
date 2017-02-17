
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

.service('logicQuerySvc', function() {

    var service = this;

    // whoever wants a callback better put their name here
    // two parameters - 1: super query string to use, 2: callback for internal setup
    service.doSearchCallback = null;

    service.setup = function() {
    };


})

.controller('LogicQueryController', function($scope, globalSvc, logicQuerySvc) {

    $scope.searchType = 'person';
    $scope.searchTypeText = 'a single person\'s name';
    $scope.text = '';
    $scope.time1 = '';
    $scope.time2 = '';
    $scope.idCounter = 1;

    // the holder of all the data structures for the logic array
    $scope.grid_list = [];

    // the item currently being edited
    $scope.editing = null;

    // score display
    $scope.hits = 0;
    $scope.documents = 0;

    $scope.setData = function() {
    };

    // change the type selector
    $scope.selectSearchTypeUI = function(typeStr, description) {
        $scope.editModeOff();
        $scope.selectSearchType( typeStr, description );
    };

    // change the type selector
    $scope.selectSearchType = function(typeStr, description) {
        if ( typeStr ) {
            $scope.text = '';
            $scope.searchType = typeStr;
            $scope.searchTypeText = description;
            $("#tdTextInput").show();
            $("#tdDate1Input").hide();
            $("#tdTime1Input").hide();
            $("#tdDate2Input").hide();
            $("#tdTime2Input").hide();
            $("#tdTo").hide();
            if ( typeStr == 'time before' || typeStr == 'time after' || typeStr == 'exact time' ) {
                $("#tdTextInput").hide();
                $("#tdDate1Input").show();
                $("#tdTime1Input").show();
            } else if ( typeStr == 'time range' ) {
                $("#tdTextInput").hide();
                $("#tdDate1Input").show();
                $("#tdTime1Input").show();
                $("#tdDate2Input").show();
                $("#tdTime2Input").show();
                $("#tdTo").show();
            } else {

            }
        }
    };

    // conver the pull down list types to their super search logic string equivalents
    $scope.typeStrToLogic = function(typeStr) {
        if ( typeStr == 'person' || typeStr == 'location' || typeStr == 'title' || typeStr == 'summary' || typeStr == 'url' ) {
            return typeStr;
        } else if ( typeStr == 'exact person' || typeStr == 'exact location' || typeStr == 'exact title' || typeStr == 'exact summary' ) {
            return typeStr;
        } else if ( typeStr == 'body text' ) {
            return "body";
        } else if ( typeStr == 'exact body text' ) {
            return "exact body";
        } else if ( typeStr == 'time before' ) {
            return "date before";
        } else if ( typeStr == 'time after' ) {
            return "date after";
        } else if ( typeStr == 'exact time' ) {
            return "date exact";
        } else if ( typeStr == 'time range' ) {
            return "date between";
        }
    };

    // ui call
    $scope.addComponent = function() {
        var typeStr = $scope.searchType;
        var dateStr1 = $("#date1").val();
        var dateStr2 = $("#date2").val();
        $scope.addComponentWithType(typeStr, $scope.text, dateStr1, $scope.time1, dateStr2, $scope.time2);
    };

    // add a new item to the grid
    $scope.addComponentWithType = function(typeStr, text, dateStr1, time1, dateStr2, time2) {
        if ( typeStr == 'person' || typeStr == 'location' || typeStr == 'title' ||
            typeStr == 'exact person' || typeStr == 'exact location' || typeStr == 'exact title' ||
            typeStr == 'exact summary' || typeStr == 'exact body text' ||
            typeStr == 'summary' || typeStr == 'url' || typeStr == 'body text' ) {
            if ( text && text == '' ) {
                globalSvc.error_message("'" + typeStr + "' cannot be empty");
            } else {
                $scope.addComponentType(typeStr, $scope.typeStrToLogic(typeStr) + '(' + text + ')', $scope.typeToIcon(typeStr), text,
                    { 'text': text } );
            }
        } else if ( typeStr == 'time before' || typeStr == 'time after' || typeStr == 'exact time' ) {
            if ( !dateStr1 || dateStr1 == '' ) {
                globalSvc.error_message("'" + typeStr + "' must have a the date field set");
            } else {
                if ( time1 && time1 != '' ) {
                    dateStr1 = dateStr1 + " " + time1;
                }
                $scope.addComponentType(typeStr, $scope.typeStrToLogic(typeStr) + ' ' + dateStr1, $scope.typeToIcon('time'),
                    typeStr + ' ' + dateStr1, { 'date1': dateStr1, 'time1': time1 } );
            }
        } else if ( typeStr == 'time range' ) {
            if ( (!dateStr1 || dateStr1 == '') || (!dateStr2 || dateStr2 == '') ) {
                globalSvc.error_message("'" + typeStr + "' must have a the date fields set");
            } else {
                if ( time1 && time2 && time1 != '' && time2 != '') {
                    dateStr1 = dateStr1 + " " + time1;
                    dateStr2 = dateStr2 + " " + time2;
                }
                $scope.addComponentType(typeStr, 'date between ' + dateStr1 + ' and ' + dateStr2, $scope.typeToIcon('time'),
                    typeStr + ' ' + dateStr1 + ' - ' + dateStr2, { 'date1': dateStr1, 'time1': time1, 'date2': dateStr2, 'time2': time2 } );
            }
        } else {
            globalSvc.error_message("'" + typeStr + "' unknown");
        }
    };

    $scope.typeToIcon = function(str) {
        if ( str == 'time' ) {
            return "images/clock.png";
        } else if ( str == 'person' || str == 'exact person' ) {
            return "images/person.png";
        } else if ( str == 'location' || str == 'exact location' ) {
            return "images/location.png";
        } else  {
            return "images/book.png";
        }
    };

    $scope.logicToIcon = function(str) {
        if ( str == 'or' ) {
            return "images/or.png";
        } else if ( str == 'and not' ) {
            return "images/and-not.png";
        } else  {
            return "images/and.png";
        }
    };

    $scope.typeToClass = function(str, isEdit) {
        if ( str == 'time' || str == 'time range' ) {
            if ( isEdit ) {
                return "viki-icon-time-edit";
            } else {
                return "viki-icon-time";
            }
        } else if ( str == 'person' ) {
            if ( isEdit ) {
                return "viki-icon-person-edit";
            } else {
                return "viki-icon-person";
            }
        } else if ( str == 'exact person' ) {
            if ( isEdit ) {
                return "viki-icon-person-edit";
            } else {
                return "viki-icon-exact-person";
            }
        } else if ( str == 'location' ) {
            if ( isEdit ) {
                return "viki-icon-location-edit";
            } else {
                return "viki-icon-location";
            }
        } else if ( str == 'exact location' ) {
            if ( isEdit ) {
                return "viki-icon-location-edit";
            } else {
                return "viki-icon-exact-location";
            }
        } else  {
            if ( isEdit ) {
                return "viki-icon-book-edit";
            } else if ( str.indexOf('exact') >= 0 ) {
                return "viki-icon-exact-book";
            } else {
                return "viki-icon-book";
            }
        }
    };

    $scope.contains = function( typeStr, text ) {
        var c = false;
        $.each( $scope.grid_list, function(i, item) {
            if ( item.type == typeStr && item.text == text ) {
                c = true;
            }
        });
        return c;
    };

    // add a component to the grid
    $scope.addComponentType = function( typeStr, text, icon, description, originalData ) {
        if ( $scope.editing != null ) {
            $scope.editing.text = text;
            $scope.editing.width = (description.length * 8) + 72;
            $scope.editing.description = description;
            $scope.editing.data = originalData;
            console.log(originalData);
            $scope.editModeOff();
        } else if ( !$scope.contains(typeStr, text) ) {
            var id = 'id' + $scope.idCounter;
            $scope.idCounter = $scope.idCounter + 1;
            var list = $scope.grid_list;
            for ( var i = 0; i < list.length; i++ ) {
                list[i].display = '';
            }
            $scope.grid_list.push({ 'id': id, 'type': typeStr, 'text': text, 'icon': icon, 'logic': 'and', 'display': 'display: none;',
                'class': $scope.typeToClass(typeStr, false), 'description': description, 'width': (description.length * 8) + 72,
                'data': originalData });
        }
    };

    $scope.editItem = function(id) {
        if ( id ) {
            $scope.editing = null;
            $.each($scope.grid_list, function(i, item) {
                item.class = $scope.typeToClass(item.type, item.id == id);
                if ( item.id == id ) {
                    $scope.editing = item;
                }
            });
            if ( $scope.editing ) {
                $scope.selectSearchType($scope.editing.type, '');
                if ( $scope.editing.data.text ) {
                    $scope.text = $scope.editing.data.text;
                } else if ( $scope.editing.data.editing ) {
                    $("#date1").val($scope.editing.data.date1);
                    $scope.time1 = $scope.editing.data.time1;
                    if ( $scope.editing.data.date2 ) {
                        $("#date2").val($scope.editing.data.date2);
                        $scope.time2 = $scope.editing.data.time2;
                    }
                }
            }
        }
    };

    // out of edit mode
    $scope.editModeOff = function() {
        $scope.editing = null;
        $.each($scope.grid_list, function(i, item) {
            item.class = $scope.typeToClass(item.type, false);
        });
    };

    $scope.removeItem = function(id) {
        if ( id ) {
            var list = [];
            $.each($scope.grid_list, function(i, item) {
                if ( item.id != id ) {
                    list.push(item);
                }
            });
            if ( list.length > 0 ) {
                for ( var i = 0; i < list.length - 1; i++ ) {
                    list[i].display = '';
                }
                list[list.length - 1].display = 'display: none;';
            }
            $scope.grid_list = list;
        }
    };

    $scope.changeLogic = function(id) {
        if ( id ) {
            $.each($scope.grid_list, function(i, item) {
                if ( item.id == id ) {
                    var logic = item.logic;
                    if ( logic == 'and' ) {
                        logic = 'or';
                    } else if ( logic == 'or' ) {
                        logic = 'and not'
                    } else {
                        logic = 'and';
                    }
                    item.logic = logic;
                }
            });
        }
    };

    // access the created data of this widget
    $scope.getSuperQueryString = function() {
        if ( $scope.grid_list.length == 0 ) {
            return "";
        } else {
            var superSearchStr = "";
            for ( var i = 0; i < $scope.grid_list.length; i++ ) {
                var item = $scope.grid_list[i];
                superSearchStr = superSearchStr + " (" + item.text + ")";
                if ( i + 1 < $scope.grid_list.length ) {
                    superSearchStr = superSearchStr + " " + item.logic;
                }
            }
            return "(" + superSearchStr + ")";
        }
    };

    // ui click search
    $scope.search = function() {
        if ( logicQuerySvc.doSearchCallback ) {
            logicQuerySvc.doSearchCallback( $scope.getSuperQueryString(), $scope.searchDone );
        }
    };

    // callback
    $scope.searchDone = function(data) {
        if ( data ) {
            $scope.hits = data.total_num_results;
            $scope.documents = data.total_num_documents;
            $("#tdTotals").show();
        } else {
            $scope.hits = 0;
            $scope.documents = 0;
        }
    };

    // setup date controls
    $('#datetimepicker1').datetimepicker({viewMode:'years', format: 'YYYY/MM/DD'});
    $('#datetimepicker2').datetimepicker({viewMode:'years', format: 'YYYY/MM/DD'});

})

.directive('logicQuery', function() {
    return {
        templateUrl: 'views/widgets/logic_query_widget.html'
    };
});

