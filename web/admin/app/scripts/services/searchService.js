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
angular.module('webApp').service('searchSvc', function ($location, $rootScope, $cookies, $http, globalSvc) {

    var service = this;

    // clear the search object
    service.clearSearchObject = function() {
        globalSvc.setObject("saved_search", null);
    };

    // calls back callback with null on failure (and sets the global error)
    service.search = function(sessionID, searchObj, page, itemsPerPage, callback) {
        console.log("search");

        // save a search object for repeat business
        globalSvc.setObject( "saved_search", {
            "sessionID": sessionID,
            "searchObj": searchObj,
            "page": page,
            "itemsPerPage": itemsPerPage
        });

        $http({
                "url": globalSvc.getNodeRR("Search") + "search/search/" + encodeURIComponent(sessionID) + "/" + encodeURIComponent(page) + "/" + encodeURIComponent(itemsPerPage),
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
    service.superSearch = function(sessionID, searchExpression, page, itemsPerPage, callback) {
        console.log("super-search");

        // clear the search object
        globalSvc.setObject( "saved_search", null );

        $http({
                "url": globalSvc.getNodeRR("Search") + "search/super-search/" + encodeURIComponent(sessionID) + "/" + encodeURIComponent(page) + "/" + encodeURIComponent(itemsPerPage),
                method: 'PUT',
                contentType: 'application/json',
                data: JSON.stringify({'search_text': searchExpression}),
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

