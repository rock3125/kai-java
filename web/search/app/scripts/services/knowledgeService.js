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
angular.module('searchApp').service('knowledgeSvc', function ($location, $rootScope, $cookies, $http, globalSvc) {

    var service = this;

    // calls back callback with null on failure (and sets the global error)
    service.getTuplesForDocument = function(sessionID, url, page, itemsPerPage, callback) {
        console.log("knowledge for " + url);

        $http({
                "url": globalSvc.getNodeRR("Knowledge") + "knowledge/tuples/" + encodeURIComponent(sessionID) + "/" +
                encodeURIComponent(url) + "/" + encodeURIComponent(page) + "/" + encodeURIComponent(itemsPerPage),
                method: 'GET',
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


});

