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
 * Description entity service, entity related functionality
 */
angular.module('searchApp').service('entitySvc', function ($location, $rootScope, $cookies, $http, globalSvc) {

    // entity assist functions

    var service = this;

    // save a new/existing kb-entity object
    service.saveEntity = function (sessionID, obj, successCallback) {
        if (obj != null && obj.type != null && obj.json_data != null) {
            if ( obj.id ) {
                console.log("update kb-entry " + obj.id);
            } else {
                console.log("save new kb-entry " + obj.json_data);
            }
            $http({
                    "url": globalSvc.getNodeRR("KBEntry") + "kb/entry/" + encodeURIComponent(sessionID),
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
            globalSvc.error("kb-entry object missing type or json_data");
        }
    };

    // calls back callback with null on failure (and sets the global error)
    service.getEntityList = function(sessionID, type, prevEntityId, itemsPerPage, field_name, filter, callback) {
        console.log("get kb-entry list");
        if ( filter == '' || field_name == null || filter == null || field_name == '') {
            filter = 'null';
            field_name = 'null';
        }
        if ( prevEntityId == undefined || prevEntityId == '' ) {
            prevEntityId = 'null';
        }
        // entity-list/{sessionID}/{type}/{previousEntityId}/{itemsPerPage}/{filter}
        $http.get(globalSvc.getNodeRR("KBEntry") + "kb/list-entities/" + encodeURIComponent(sessionID) + "/" +
            encodeURIComponent(type) + "/" +
            encodeURIComponent(prevEntityId) + "/" + encodeURIComponent(itemsPerPage) + "/" +
            encodeURIComponent(field_name) + "/" +
            encodeURIComponent(filter) ).then(

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

    // delete an existing entity object
    service.deleteEntity = function (sessionID, type, id, successCallback) {
        if ( sessionID != null && id != null ) {
            $http({
                    "url": globalSvc.getNodeRR("KBEntry") + "kb/entry/" + encodeURIComponent(sessionID) + "/" +
                            encodeURIComponent(type) + "/" +
                            encodeURIComponent(id),
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
            globalSvc.error("kb-entry object missing id");
        }
    };

    // upload a json document
    service.upload = function(sessionID, type, filename, file, successCallback) {
        console.log('uploading json data');
        var url = globalSvc.getNodeRR("KBEntry") + 'kb/upload-for-type/' + encodeURIComponent(sessionID) + '/' + encodeURIComponent(type) +
            '/' + encodeURIComponent(filename);
        var fd = new FormData();
        fd.append('file', file);
        $http.post(url, fd, {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        }).success(function(response){
                console.log('json upload successful');
                if ( successCallback ) {
                    successCallback(true);
                }
        }).error(function(response){
            globalSvc.error(response);
            if ( successCallback ) {
                successCallback(false);
            }
        });
    };

});

