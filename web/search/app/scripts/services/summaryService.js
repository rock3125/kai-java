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
angular.module('searchApp').service('summarySvc', function ($location, $rootScope, $cookies, $http, globalSvc) {
    
    var service = this;

    service.getSummarySet= function(sessionID, url_list, callback) {
        var url = globalSvc.getNodeRR("Summary") + "summarization/retrieve/" + encodeURIComponent(sessionID);
        console.log("summary post urls: " + url_list);
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


    service.getSimilarSet= function(sessionID, page, pageSize, callback) {
        var url = globalSvc.getNodeRR("Summary") + "summarization/retrieve-similar/" + encodeURIComponent(sessionID) +
                                               "/" + encodeURIComponent(page) + "/" + encodeURIComponent(pageSize);
        console.log("summary/retrieve-similar get");
        $http({
                "url": url,
                method: 'GET',
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



    // calls back callback with null on failure (and sets the global error)
    service.search = function(sessionID, searchObj, page, itemsPerPage, callback) {
        console.log("summary search");

        // save a search object for repeat business
        globalSvc.setObject( "saved_search", {
            "sessionID": sessionID,
            "searchObj": searchObj,
            "page": page,
            "itemsPerPage": itemsPerPage
        });

        $http({
                "url": globalSvc.getNodeRR("Summary") + "summarization/search/" + encodeURIComponent(sessionID) + "/" + encodeURIComponent(page) + "/" + encodeURIComponent(itemsPerPage),
                method: 'PUT',
                contentType: 'application/json',
                data: JSON.stringify(searchObj),
                dataType: 'json'
            }
        ).then(
            function success(response) {
                if (response && response.data) {
                    if (callback) {
                        callback(response.data);
                    }
                }
            }, function error(response) {
                globalSvc.error(response);
                if (callback) {
                    callback(null);
                }
            });
    };



});

