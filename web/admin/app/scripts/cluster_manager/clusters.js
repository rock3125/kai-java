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
 * @name webApp.controller:ClusterManagerController
 * @description
 * # ClusterManagerController
 * Controller of the webApp
 */
angular.module('webApp')
    .controller('ClusterManagerController', function ($scope, $cookies, $location, globalSvc, clusterSvc, documentSvc) {

        var session = null;

        $scope.cluster_set = [];


        $scope.clusterDoneCallback = function(data) {
            if ( data && data.kMeansClusterList ) {
                $scope.cluster_set = data.kMeansClusterList;
            } else {
                $scope.cluster_set = [];
            }
        };

        // go to detail-view for a specific cluster
        $scope.details = function(cluster_id) {
            if ( cluster_id >= 0 ) {
                globalSvc.setObject("cluster_id", {"cluster_id": cluster_id});
                $location.path("/cluster-details");
            }
        };

        ///////////////////////////////////////////////////////////////

        // signed in?
        globalSvc.getSession( function(pSession) {
            if ( !pSession ) {
                globalSvc.goHome();
            } else {
                session = pSession;
                clusterSvc.getClusterSet(session, $scope.clusterDoneCallback);
            }
        });



    });

