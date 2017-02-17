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
 * @name webApp.controller:DocumentAnomalyManagerController
 * @description
 * # DocumentAnomalyManagerController
 * Controller of the webApp
 */
angular.module('webApp')
    .controller('DocumentAnomalyManagerController', function ($scope, $http, $cookies, globalSvc, clusterSvc,
                                                           documentSvc, documentViewerSvc) {

    var session = null;
    $scope.anomaly_list = [];


    $scope.clusterAnomalyCallback = function(data) {
        if ( data && data.document_list ) {
            $scope.anomaly_list = data.document_list;
            paginator.setup($scope.anomaly_list);
        } else {
            $scope.anomaly_list = [];
            paginator.setup([]);
        }
    };
        
    ///////////////////////////////////////////////////////////////

    var paginator = new DocumentPagination('document-anomaly', 'pagination', 'documentAnomalyManagerController');

    $scope.next = function() {
        if (paginator.next()) {
            clusterSvc.getClusterAnomalies(session, paginator.prevUrl, paginator.itemsPerPage, $scope.clusterAnomalyCallback);
        }
    };

    $scope.prev = function() {
        if (paginator.prev()) {
            clusterSvc.getClusterAnomalies(session, paginator.prevUrl, paginator.itemsPerPage, $scope.clusterAnomalyCallback);
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

    $scope.view = function(url) {
        if ( url ) {
            console.log("view " + document.url);
            documentSvc.viewDocument(session, document.url);
        }
    };

    // signed in?
    globalSvc.getSession( function(pSession) {

        if ( !pSession ) {

            globalSvc.goHome();

        } else {
            session = pSession;
            clusterSvc.getClusterAnomalies(session, paginator.prevUrl, paginator.itemsPerPage, $scope.clusterAnomalyCallback);
        }
    });



});

