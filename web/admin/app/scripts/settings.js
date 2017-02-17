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
 * @name webApp.controller:SettingsController
 * @description
 * # SettingsController
 * Controller of the webApp
 */
angular.module('webApp')
.controller('SettingsController', function ($scope, $http, $location, globalSvc, viewLogSvc, logViewerWidgetSvc) {

    var session = null;
    $scope.server_list_by_type = {};
    $scope.server_list = [];
    $scope.config_list = [];
    $scope.log_url = null;
    $scope.table_visibility = {};

    // how many lines to display of the log
    $scope.number_of_lines_of_log = 30;

    // make a call to the echo service - this can be optimized later
    // realising that a lot of these nodes run on the same server/port
    $scope.testService = function( id, type, url, session, callback ) {
        $http({
                "url": url + "echo/test/" + encodeURIComponent(session),
                method: 'GET',
                contentType: 'application/json',
                dataType: 'json'
            }
        ).then(
            function success(response) {
                callback({ "type": type, "id": id, "url": url, "image": 'images/green-button.png', "tag": type + ' ' + id + ' active' });
            }, function error(response) {
                callback({ "type": type, "id": id, "url": url, "image": 'images/red-button.png', "tag": type + ' ' + id + ' inactive' });
            });
    };

    $scope.toggle = function(table_name) {
        if ($scope.table_visibility[table_name] == undefined) {
            $scope.table_visibility[table_name] = true;
        }
        if (!$scope.table_visibility[table_name]) {
            $("#" + table_name).show();
            $scope.table_visibility[table_name] = true;
        } else {
            $("#" + table_name).hide();
            $scope.table_visibility[table_name] = false;
        }
    };

    // request to view a log at url
    $scope.viewLog = function( url ) {
        if ( url ) {
            $scope.log_url = url;
            logViewerWidgetSvc.getDataFn = $scope.getLogData; // setup data callback
            viewLogSvc.viewLog(session, url, $scope.number_of_lines_of_log, $scope.viewLogCallback);
        }
    };

    $scope.getLogData = function() {
        if ( $scope.log_url ) {
            viewLogSvc.viewLog(session, $scope.log_url, $scope.number_of_lines_of_log, $scope.getLogDataCallback);
        }
    };

    $scope.getLogDataCallback = function(data) {
        if ( data && data.string_list ) {
            logViewerWidgetSvc.update(data.string_list);
        } else {
            console.log('no data');
        }
    };

    // callback - view log request done
    $scope.viewLogCallback = function(data) {
        if ( data && data.string_list ) {
            logViewerWidgetSvc.setup(data.string_list);
        } else {
            console.log('no data');
        }
    };

    $scope.set_config = function( itemStr ) {
        if ( itemStr == null ) {
            $("#divStatus").show();
            $("#divConfig").hide();
        } else {
            $("#divStatus").hide();
            $("#divConfig").show();

            // setup the right config panel
            $scope.config_list = [];
            $.each( configuration, function(i, item) {
                if ( item && item.description == itemStr && item.items ) {
                    var list = [];
                    $.each( item.items, function(j, part) {
                        var parts = part.split(":");
                        if ( parts && parts.length >= 2 ) {
                            if ( parts[1] == "not-set" ) {
                                parts[1] = "***";
                            }
                            var str = "";
                            $.each(parts, function(k,value) {
                                if ( k > 0 ) {
                                    str = str + value + ":";
                                }
                            });
                            if ( str.length > 0 ) {
                                str = str.substr(0, str.length - 1);
                            }
                            list.push({"name": parts[0], "value": str});
                        }
                    });
                    $scope.config_list = list;
                }
            });
        }
    };

    // get the response { "type": type, "id": id, "url": url, "image": 'images/green-button.png', "tag": type + ' ' + id + ' active' }
    $scope.response = function(record) {
        if ( record ) {
            $scope.server_list.push(record);

            if (!$scope.server_list_by_type[record.type]) {
                $scope.server_list_by_type[record.type] = [];
            }
            $scope.server_list_by_type[record.type].push(record);

            $scope.server_list.sort( function(r1, r2) {
                var v1 = r1.type + ":" + r1.id;
                var v2 = r2.type + ":" + r2.id;
                if ( v1 < v2 ) return -1;
                if ( v1 > v2 ) return 1;
                return 0;
            });
        }
    };

    // perform the test
    $scope.refresh = function () {
        $scope.server_list = []; // reset
        $scope.server_list_by_type = {};
        $.each( globalSvc.getInfrastructureMap(), function( type, itemList ) {
            $.each( itemList.list, function(i, url) {
                $scope.testService(i+1, type, url, session, $scope.response);
            });
        });
    };


    ///////////////////////////////////////////////////////////////

    // signed in?
    globalSvc.getSession( function(pSession) {
        if ( !pSession ) {
            globalSvc.goHome();
        } else {
            session = pSession;
            $scope.refresh();
        }
    });


});
