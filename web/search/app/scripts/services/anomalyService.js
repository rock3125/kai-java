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
 * Description group service, group related functionality
 */
angular.module('searchApp').service('anomalySvc', function ($location, $rootScope, $cookies, $http, globalSvc) {

    // group assist functions

    var service = this;

    // calls back callback with null on failure (and sets the global error)
    service.getAnomalySet= function(sessionID, url_list, callback) {
        var url = globalSvc.getNodeRR("Analysis") + "emotional/analysis/" + encodeURIComponent(sessionID);
        console.log("anomaly post urls: " + url_list);
        $http({
                "url": url,
                method: 'POST',
                data: JSON.stringify({ "string_list": url_list }),
                contentType: 'application/json',
                dataType: 'json'
            }
        ).then(
            function success(response) {
                if ( response && response.data ) {
                    callback(response.data);
                }
            }, function error(response) {
                globalSvc.error(response);
                if ( callback ) {
                    callback(null);
                }
            });

    };


    // get the anomalies paginated list
    service.getPaginatedAnomalies = function(sessionID, page, pageSize, asc, callback) {
        var url = globalSvc.getNodeRR("Analysis") + "emotional/positive-negative/" + encodeURIComponent(sessionID) + "/" +
                                               encodeURIComponent(page) + "/" + encodeURIComponent(pageSize) + "/";
        if ( asc ) {
            url = url + "true";
        } else {
            url = url + "false";
        }
        console.log("positive-negative post");
        $http({
                "url": url,
                method: 'POST',
                contentType: 'application/json',
                dataType: 'json'
            }
        ).then(
            function success(response) {
                if ( response && response.data ) {
                    callback(response.data);
                }
            }, function error(response) {
                globalSvc.error(response);
                if ( callback ) {
                    callback(null);
                }
            });

    };



});

