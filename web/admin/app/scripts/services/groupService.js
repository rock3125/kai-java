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
angular.module('webApp').service('groupSvc', function ($location, $rootScope, $cookies, $http, globalSvc) {

    // group assist functions

    var service = this;

    // create a new group object
    service.createGroup = function (sessionID, obj, successCallback) {
        if (obj != null && obj.name != null ) {
            $http({
                    "url": globalSvc.getNodeRR("Group") + "security/group/" + encodeURIComponent(sessionID),
                    method: 'POST',
                    contentType: 'application/json',
                    data: JSON.stringify(obj),
                    dataType: 'json'
                }
            ).then(
                function success(response) {
                    if (response && response.data) {
                        if (successCallback) {
                            successCallback(true);
                        }
                    }
                }, function error(response) {
                    globalSvc.error(response);
                    if (successCallback) {
                        successCallback(false);
                    }
                });
        } else {
            globalSvc.error("group missing name");
        }
    };

    // update an existing group object
    service.updateGroup = function (sessionID, obj, successCallback) {
        if (obj != null && obj.name != null ) {
            $http({
                    "url": globalSvc.getNodeRR("Group") + "security/group/" + encodeURIComponent(sessionID),
                    method: 'PUT',
                    contentType: 'application/json',
                    data: JSON.stringify(obj),
                    dataType: 'json'
                }
            ).then(
                function success(response) {
                    if (response && response.data) {
                        if (successCallback) {
                            successCallback(true);
                        }
                    }
                }, function error(response) {
                    globalSvc.error(response);
                    if (successCallback) {
                        successCallback(false);
                    }
                });
        } else {
            globalSvc.error("group missing name");
        }
    };

    // delete existing group
    service.deleteGroup = function (sessionID, name, successCallback) {
        if ( sessionID != null && name != null ) {
            $http({
                    "url": globalSvc.getNodeRR("Group") + "security/group/" + encodeURIComponent(sessionID) + "/" + encodeURIComponent(name),
                    method: 'DELETE',
                    contentType: 'application/json',
                    dataType: 'json'
                }
            ).then(
                function success(response) {
                    if (response && response.data) {
                        if (successCallback) {
                            successCallback(true);
                        }
                    }
                }, function error(response) {
                    globalSvc.error(response);
                    if (successCallback) {
                        successCallback(false);
                    }
                });
        } else {
            globalSvc.error("group missing name");
        }
    };

    // calls back callback with null on failure (and sets the global error)
    service.getGroupList = function(sessionID, page, itemsPerPage, filter, callback) {
        console.log("get group list");
        if ( filter == null || filter == '' ) {
            filter = 'null';
        }
        $http.get(globalSvc.getNodeRR("Group") + "group/group-list/" + encodeURIComponent(sessionID) + "/" +
            encodeURIComponent(page) + "/" + encodeURIComponent(itemsPerPage) + "/" + encodeURIComponent(filter)).then(
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
