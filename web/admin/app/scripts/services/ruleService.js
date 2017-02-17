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
 * Description rule service, rule related functionality
 */
angular.module('webApp').service('ruleSvc', function ($location, $rootScope, $cookies, $http, globalSvc) {

    var service = this;
    service.rule = null; // temp storage for edit

    // create a new rule
    service.createRule = function (sessionID, obj, successCallback) {
        if (obj != null && obj.rule_name != null ) {
            $http({
                    "url": globalSvc.getNodeRR("Rule") + "rules/rule/" + encodeURIComponent(sessionID),
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
            globalSvc.error("save: rule missing name");
        }
    };


    // update an existing rule
    service.updateRule = function (sessionID, obj, successCallback) {
        if (obj != null && obj.rule_name != null ) {
            $http({
                    "url": globalSvc.getNodeRR("Rule") + "rules/rule/" + encodeURIComponent(sessionID),
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
            globalSvc.error("update: rule missing name");
        }
    };


    // update an existing rule
    service.deleteRule = function (sessionID, rule_name, successCallback) {
        if (rule_name != null ) {
            $http({
                    "url": globalSvc.getNodeRR("Rule") + "rules/rule/" + encodeURIComponent(sessionID) + "/" + encodeURIComponent(rule_name),
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
            globalSvc.error("delete: rule missing name");
        }
    };


    // calls back callback with null on failure (and sets the global error)
    service.getRuleList = function(sessionID, prevRuleName, itemsPerPage, callback) {
        console.log("get rule list");
        if ( prevRuleName == undefined || prevRuleName == '' ) {
            prevRuleName = 'null';
        }
        // rule-list/{sessionID}/{prevRuleName}/{itemsPerPage}
        $http.get(globalSvc.getNodeRR("Rule") + "rules/rule-list/" + encodeURIComponent(sessionID) + "/" +
            encodeURIComponent(prevRuleName) + "/" + encodeURIComponent(itemsPerPage) ).then(
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


    // update an existing rule
    service.execute = function (sessionID, rule_name, successCallback) {
        if (rule_name != null ) {
            $http({
                    "url": globalSvc.getNodeRR("Rule") + "rules/exec/" + encodeURIComponent(sessionID) + "/" + encodeURIComponent(rule_name),
                    method: 'PUT',
                    contentType: 'application/json',
                    dataType: 'json'
                }
            ).then(
                function success(response) {
                    if (response && response.data) {
                        if (successCallback) {
                            successCallback(response.data);
                        }
                    }
                }, function error(response) {
                    globalSvc.error(response);
                    if (successCallback) {
                        successCallback(null);
                    }
                });
        } else {
            globalSvc.error("execute: rule missing name");
        }
    };



});


