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
 * search Module
 *
 * Description group service, group related functionality
 */
angular.module('searchApp').service('searchSvc', function ($location, $rootScope, $cookies, $http, globalSvc) {

    var service = this;

    // clear the search object
    service.clearSearchObject = function() {
        globalSvc.setObject("saved_search", null);
    };

    // calls back callback with null on failure (and sets the global error)
    service.search = function(sessionID, searchObj, page, itemsPerPage, maxDistance, callback) {
        console.log("search for " + searchObj.search_text);

        // clear the search object
        globalSvc.setObject( "saved_search", null );

        $http({
                "url": globalSvc.getNodeRR("Search") + "search/search/" + encodeURIComponent(sessionID) + "/" +
                encodeURIComponent(page) + "/" + encodeURIComponent(itemsPerPage) + "/" + encodeURIComponent(maxDistance),
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


    // get authors / experts for keywords
    service.expert_search = function(sessionID, searchObj, minPercentage, callback) {
        console.log("expert search for " + searchObj.search_text);

        $http({
                "url": globalSvc.getNodeRR("Search") + "search/topic-authors/" + encodeURIComponent(sessionID) + "/" +
                                                        encodeURIComponent(minPercentage),
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


    // calls back callback with null on failure (and sets the global error)
    service.nl_search = function(sessionID, text, page, itemsPerPage, callback) {
        console.log("searchService.nl_search for \"" + text + "\"");

        $http({
                "url": globalSvc.getNodeRR("Search") + "search/tuple-search/" + encodeURIComponent(sessionID) + "/" +
                encodeURIComponent(text) + "/" + encodeURIComponent(page) + "/" + encodeURIComponent(itemsPerPage),
                method: 'PUT',
                contentType: 'application/json',
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


    service.view_entities = function( sessionID, searchObj, callback ) {
        if ( sessionID && searchObj && searchObj.url_list && searchObj.search_text ) {
            console.log("view entities \"" + searchObj.search_text + "\"");
            $http({
                    "url": globalSvc.getNodeRR("Search") + "search/view-entity/" + encodeURIComponent(sessionID),
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
                    if (callback) {
                        callback(null);
                    }
                });
        }
    };





});

