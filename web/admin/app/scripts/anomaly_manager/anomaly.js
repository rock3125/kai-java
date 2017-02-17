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
 * @name webApp.controller:AnomalyManagerController
 * @description
 * # AnomalyManagerController
 * Controller of the webApp
 */
angular.module('webApp')
    .controller('AnomalyManagerController', function ($scope, $http, $cookies, globalSvc, anomalySvc, documentViewerSvc) {

        var session = null;
        var positiveFirst = true;
        $scope.result_list = [];
        $scope.fragment_size = [];
        $scope.fragment_index = [];
        $scope.url_list = [];

        ///////////////////////////////////////////////////////////////

        var paginator = new DocumentPagination('anomaly', 'pagination', 'anomalyManagerController');

        $scope.next = function() {
            if (paginator.next()) {
                anomalySvc.getPaginatedAnomalies(session, paginator.page, paginator.itemsPerPage,
                    positiveFirst, $scope.getAnomalyListDone);
            }
        };

        $scope.prev = function() {
            if (paginator.prev()) {
                anomalySvc.getPaginatedAnomalies(session, paginator.page, paginator.itemsPerPage,
                    positiveFirst, $scope.getAnomalyListDone);
            }
        };

        ///////////////////////////////////////////////////////////////
        // document details / popup

        // get the details for a url item
        $scope.details = function(url) {
            if ( url && session ) {
                documentViewerSvc.show(session, url);
            }
        };

        ///////////////////////////////////////////////////////////////

        $scope.getAnomalyListDone = function(data) {
            if ( data && data.search_result_list ) {

                // get results and reset indexes and sizes for the fragments
                $scope.result_list = data.search_result_list;
                $scope.fragment_size = [];
                $scope.fragment_index = [];
                $scope.url_list = [];

                if ( $scope.result_list.length > 0 ) {

                    $.each($scope.result_list, function(i, searchResult) {
                        var new_list = [];
                        $.each(searchResult.text_list, function (j, item) {
                            new_list.push(item);
                        });
                        $scope.url_list.push(searchResult.url);
                        searchResult.index = i; // set an index on the result for processing
                        searchResult.result_list = new_list; // set the list of pretty html strings
                        searchResult.percentage = parseInt(searchResult.score * 10000.0) / 100.0;
                        // scroll through the fragment setup
                        $scope.fragment_size.push( new_list.length ); // length for boundary checking
                        $scope.fragment_index.push( 0 ); // the current index for the fragment
                    });

                    // set the url list for other pages to read
                    globalSvc.setObject("url_list", $scope.url_list);
                    paginator.setup($scope.result_list);
                } else {
                    paginator.setup([]);
                }
            } else {
                paginator.setup([]);
                console.log('anomaly get failed');
            }
        };

        $scope.positive = function () {
            if ( !positiveFirst ) {
                paginator.page = 0;
                positiveFirst = true;
                anomalySvc.getPaginatedAnomalies(session, paginator.page, paginator.itemsPerPage,
                    positiveFirst, $scope.getAnomalyListDone);
            }
        };

        $scope.negative = function () {
            if ( positiveFirst ) {
                paginator.page = 0;
                positiveFirst = false;
                anomalySvc.getPaginatedAnomalies(session, paginator.page, paginator.itemsPerPage,
                    positiveFirst, $scope.getAnomalyListDone);
            }
        };

        // signed in?
        globalSvc.getSession( function(pSession) {

            if ( !pSession ) {

                globalSvc.goHome();

            } else {
                session = pSession;
                anomalySvc.getPaginatedAnomalies(session, paginator.page, paginator.itemsPerPage,
                                                 positiveFirst, $scope.getAnomalyListDone);
            }
        });



    });

