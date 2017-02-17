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
 * webApp Module
 *
 * Description cluster service, k-means cluster related functionality
 */
angular.module('webApp').service('clusterSvc', function ($location, $rootScope, $cookies, $http, globalSvc) {

    // entity assist functions

    var service = this;

    // calls back callback with null on failure (and sets the global error)
    service.getClusterSet = function(sessionID, callback) {
        $http.get(globalSvc.getNodeRR("Clustering") + "cluster/k-means/" + encodeURIComponent(sessionID)  ).then(
            function success(response) {
                if ( response && response.data ) {
                    callback(response.data);
                }
            }, function error(response) {
                globalSvc.error(response);
                callback(null);
            }
        );
    };

    // calls back callback with null on failure (and sets the global error)
    service.getClusterByID = function(sessionID, cluster_id, callback) {
        $http.get(globalSvc.getNodeRR("Clustering") + "cluster/k-means/" + encodeURIComponent(sessionID) + "/" +
                                               encodeURIComponent(cluster_id) ).then(
            function success(response) {
                if ( response && response.data ) {
                    callback(response.data);
                }
            }, function error(response) {
                globalSvc.error(response);
                callback(null);
            }
        );
    };


    // calls back callback with null on failure (and sets the global error)
    service.getClusterAnomalies = function(sessionID, prevUrl, pageSize, callback) {
        if ( !prevUrl ) {
            prevUrl = "null";
        }
        $http.get(globalSvc.getNodeRR("Clustering") + "cluster/k-means-anomalies/" + encodeURIComponent(sessionID) + "/" +
                encodeURIComponent(prevUrl) + "/" + encodeURIComponent(pageSize) ).then(
            function success(response) {
                if ( response && response.data ) {
                    callback(response.data);
                }
            }, function error(response) {
                globalSvc.error(response);
                callback(null);
            }
        );
    };


});

