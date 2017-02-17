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
 * Description user service, user related functionality
 */
angular.module('webApp').service('userSvc', function ($location, $rootScope, $cookies, $http, globalSvc) {

    // user assist functions

    var service = this;

    // calls back callback with the session id on success
    // calls back callback with null on failure (and sets the global error)
    service.login = function(username, password, callback) {
        console.log("login viki user");
        $http.get(securityServiceEntry + "security/token/" + encodeURIComponent(username) + "/" + encodeURIComponent(password)).then(
            function success(response) {
                if ( response && response.data.sessionID ) {
                    globalSvc.setSession(response.data.sessionID);
                    globalSvc.setObject('user', response.data);
                    if ( callback ) {
                        callback(response.data.sessionID);
                    }
                }
            }, function error(response) {
                globalSvc.error(response);
                if ( callback ) {
                    callback(null);
                }
            }
        );
    };

    // create a new user/organisation for login purposes
    service.createUserOrganisation = function (sessionID, organisationName, obj, successCallback) {
        if (obj != null && obj.email != null ) {
            $http({
                    "url": globalSvc.getNodeRR("Security") + "security/user-organisation/" + encodeURIComponent(organisationName),
                    method: 'POST',
                    contentType: 'application/json',
                    data: JSON.stringify(obj),
                    dataType: 'json'
                }
            ).then(
                function success(response) {
                    if (response && response.data) {
                        if (successCallback) {
                            successCallback(false);
                        }
                    }
                }, function error(response) {
                    globalSvc.error(response);
                    if (successCallback) {
                        successCallback(true);
                    }
                });
        } else {
            globalSvc.error("object missing email address");
        }
    };

    // create a new user object
    service.createUser = function (sessionID, obj, successCallback) {
        if (obj != null && obj.email != null ) {
            $http({
                    "url": globalSvc.getNodeRR("Security") + "security/user/" + encodeURIComponent(sessionID),
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
            globalSvc.error("object missing email address");
        }
    };

    // update an existing user object
    service.updateUser = function (sessionID, obj, successCallback) {
        if (obj != null && obj.email != null ) {
            $http({
                    "url": globalSvc.getNodeRR("Security") + "security/user/" + encodeURIComponent(sessionID),
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
            globalSvc.error("object missing email address");
        }
    };

    // delete an existing user object
    service.deleteUser = function (sessionID, email, successCallback) {
        if ( sessionID != null && email != null ) {
            $http({
                    "url": globalSvc.getNodeRR("Security") + "security/user/" + encodeURIComponent(sessionID) + "/" + encodeURIComponent(email),
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
            globalSvc.error("object missing email address");
        }
    };

    // calls back callback with null on failure (and sets the global error)
    service.getUserList = function(sessionID, page, itemsPerPage, filter, callback) {
        console.log("get user list");
        if ( filter == null || filter == '' ) {
            filter = 'null';
        }
        $http.get(globalSvc.getNodeRR("Security") + "security/user-list/" + encodeURIComponent(sessionID) + "/" +
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


    // calls back callback with null on failure (and sets the global error)
    service.getSessionList = function(sessionID, page, itemsPerPage, callback) {
        console.log("get session list");
        $http.get(globalSvc.getNodeRR("Security") + "security/session-list/" + encodeURIComponent(sessionID) + "/" +
            encodeURIComponent(page) + "/" + encodeURIComponent(itemsPerPage) ).then(
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

