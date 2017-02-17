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
 * @name webApp.controller:SimilarManagerController
 * @description
 * # SimilarManagerController
 * Controller of the webApp
 */
angular.module('webApp')
    .controller('SimilarManagerController', function ($scope, $cookies, $location, globalSvc, searchSvc,
                                                   documentSvc, summarySvc, documentViewerSvc) {

        var session = null;
        $scope.similar_list = [];

        ///////////////////////////////////////////////////////////////

        var paginator = new DocumentPagination('similar-manager', 'pagination', 'similarManagerService');

        $scope.next = function() {
            if (paginator.next()) {
                summarySvc.getSimilarSet(session, paginator.page, paginator.itemsPerPage, $scope.similarGetDone);
            }
        };

        $scope.prev = function() {
            if (paginator.prev()) {
                summarySvc.getSimilarSet(session, paginator.page, paginator.itemsPerPage, $scope.similarGetDone);
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

        // callback after done
        $scope.similarGetDone = function (data) {
            if ( data && data.similarDocumentSetList ) {
                $scope.similar_list = data.similarDocumentSetList;

                // setup %
                $.each($scope.similar_list, function(i, item) {
                    $.each(item.similarDocumentList, function(j, item2) {
                        item2.percentage = parseInt((100.0 - (100 * item2.similarity)) * 100.0) * 0.01;
                    });
                });

                paginator.setup($scope.similar_list);

            } else {
                $scope.similar_list = [];
                paginator.setup([]);
            }
        };

        ///////////////////////////////////////////////////////////////

        // signed in?
        globalSvc.getSession( function(pSession) {
            if ( !pSession ) {
                globalSvc.goHome();
            } else {
                session = pSession;
                summarySvc.getSimilarSet(session, paginator.page, paginator.itemsPerPage, $scope.similarGetDone);
            }
        });



    });

