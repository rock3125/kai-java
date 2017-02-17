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
 * Description summary service, entity related functionality
 */
angular.module('webApp').service('statsSvc', function ($location, $rootScope, $cookies, $http, globalSvc) {

    var service = this;

    service.getGeneralStatistics = function (sessionID, callback) {
        var url = globalSvc.getNodeRR("Statistics") + "statistics/general/" + encodeURIComponent(sessionID);
        $http({
                "url": url,
                method: 'GET',
                contentType: 'application/json',
                dataType: 'json'
            }
        ).then(
            function success(response) {
                if (response && response.data) {
                    callback(response.data);
                }
            }, function error(response) {
                globalSvc.error(response);
                if (callback) {
                    callback(null);
                }
            });

    };


    service.getIndexStatistics = function (sessionID, count, callback) {
        var url = globalSvc.getNodeRR("Statistics") + "statistics/index/" + encodeURIComponent(sessionID) + "/" + encodeURIComponent(count);
        $http({
                "url": url,
                method: 'GET',
                contentType: 'application/json',
                dataType: 'json'
            }
        ).then(
            function success(response) {
                if (response && response.data) {
                    callback(response.data);
                }
            }, function error(response) {
                globalSvc.error(response);
                if (callback) {
                    callback(null);
                }
            });

    };


    service.getDocumentStatistics = function (sessionID, url, callback) {
        console.log("document stats for url: " + url);
        var url = globalSvc.getNodeRR("Statistics") + "statistics/document/" + encodeURIComponent(sessionID) + "/" + encodeURIComponent(url);
        $http({
                "url": url,
                method: 'GET',
                contentType: 'application/json',
                dataType: 'json'
            }
        ).then(
            function success(response) {
                if (response && response.data) {
                    callback(response.data);
                }
            }, function error(response) {
                globalSvc.error(response);
                if (callback) {
                    callback(null);
                }
            });

    };


});


