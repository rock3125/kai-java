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
 * Description entity service, entity related functionality
 */
angular.module('searchApp').service('documentSvc', function ($location, $rootScope, $cookies, $http, globalSvc) {

    // entity assist functions

    var service = this;

    // calls back callback with null on failure (and sets the global error)
    service.getDocumentList = function(sessionID, prevUrl, page, itemsPerPage, filter, callback) {
        console.log("get document list");
        if ( !filter || filter == '') {
            filter = 'null';
        }
        if ( !prevUrl || prevUrl == '') {
            prevUrl = 'null';
        }
        $http.get(globalSvc.getNodeRR("Document") + "document/document-list/" + encodeURIComponent(sessionID) + "/" +
            encodeURIComponent(prevUrl) + "/" + encodeURIComponent(page) + "/" + encodeURIComponent(itemsPerPage) + "/" +
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

    // return the sentence @ sentenceIndex for url
    service.getSentence = function(sessionID, url, sentenceIndex, callback) {
        $http.get(globalSvc.getNodeRR("Document") + "document/sentence/" + encodeURIComponent(sessionID) + "/" +
                                               encodeURIComponent(url) + "/" + encodeURIComponent(sentenceIndex) ).then(
            function success(response) {
                if ( response && response.data ) {
                    if (callback) {
                        callback(url, sentenceIndex, response.data);
                    }
                }
            }, function error(response) {
                globalSvc.error(response);
                if ( callback ) {
                    callback(url, sentenceIndex, null);
                }
            }
        );
    };


    // return the metadata items for an entire document (without its original content text)
    service.getMetadata = function(sessionID, url, callback) {
        console.log("metadata get url: " + url);
        $http.get(globalSvc.getNodeRR("Document") + "document/metadata/" + encodeURIComponent(sessionID) + "/" + encodeURIComponent(url) ).then(
            function success(response) {
                if ( response && response.data ) {
                    if (callback) {
                        callback(response.data);
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


    // calls back callback with null on failure (and sets the global error)
    service.viewDocument = function(sessionID, url) {
        console.log("view document " + url);
        var viewDocumentUrl = globalSvc.getNodeRR("Document") + "document/document/" + encodeURIComponent(sessionID) + "/" + encodeURIComponent(url);
        window.open(viewDocumentUrl, 'view_document');
    };

    // calls back callback with null on failure (and sets the global error)
    service.getDocumentImageUrl = function(sessionID, url) {
        console.log("view document image " + url);
        return globalSvc.getNodeRR("Document") + "document/document/image/" + encodeURIComponent(sessionID) + "/" + encodeURIComponent(url);
    };

    // delete an existing document
    service.deleteDocument = function (sessionID, url, successCallback) {
        if ( sessionID != null && url != null ) {
            $http({
                    "url": globalSvc.getNodeRR("Document") + "document/document/" + encodeURIComponent(sessionID) + "/" + encodeURIComponent(url),
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
            globalSvc.error("document object missing url");
        }
    };

    // create/update a document with upload
    service.upload = function(session, uploadUrl, aclList, origin, file, saveCallback) {
        // POST /viki/document/metadata/{sessionID}/{url}
        var aclSet = [];
        $.each(aclList, function(i, acl) {
            aclSet.push( { 'user_group': acl, 'has_access': true});
        });
        console.log('uploading document meta-data for ' + uploadUrl);
        var url1 = globalSvc.getNodeRR("Document") + 'document/metadata/' + encodeURIComponent(session) + '/' +
                                                encodeURIComponent(origin) + '/' + encodeURIComponent(uploadUrl);
        var obj = { 'url': uploadUrl, 'acl_set': aclSet };
        $http({
                "url": url1,
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(obj),
                dataType: 'json'
            }
        ).then(
            function success(response) {

                // POST /viki/document/document/{sessionID}/{url}
                console.log('posting document binary data for ' + uploadUrl);
                var fd = new FormData();
                var url2 = globalSvc.getNodeRR("Document") + 'document/document/' + encodeURIComponent(session) + '/' + encodeURIComponent(uploadUrl);
                fd.append('file', file);
                $http.post(url2, fd, {
                        transformRequest: angular.identity,
                        headers: {'Content-Type': undefined}
                    })
                    .success(function(){

                        console.log('document successful: ' + uploadUrl);
                        if ( saveCallback ) {
                            saveCallback(true);
                        }

                    }).error(function(){
                        if ( saveCallback ) {
                            saveCallback(false);
                        }
                    });

            }, function error(response) {
                globalSvc.error(response);
                if ( saveCallback ) {
                    saveCallback(false);
                }
            });

    };


    // compare documents with upload : compare/{sessionID}/{url}/{threshold}/{numResults}
    service.documentCompare = function(session, url, threshold, numResults, file, saveCallback) {
        var url1 = globalSvc.getNodeRR("Document") + 'document-compare/compare/' + encodeURIComponent(session) + '/' +
                encodeURIComponent(url) + '/' + encodeURIComponent(threshold) + '/' + encodeURIComponent(numResults);

        console.log('posting document binary data for ' + url);
        var fd = new FormData();
        fd.append('file', file);
        $http.post(url1, fd, {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        }).success(function(data){
            console.log('document compare: ' + url);
            if ( saveCallback ) {
                saveCallback(data);
            }
        }).error(function(){
            if ( saveCallback ) {
                saveCallback(null);
            }
        });
    };


    // send a redo it all message
    service.redo = function(session, uploadUrl, doneCallback) {
        // start the party!
        console.log('triggering document processing for ' + uploadUrl);
        var url = globalSvc.getNodeRR("Document") + 'document/start/' + encodeURIComponent(session) + '/' + encodeURIComponent(uploadUrl);
        $http({
            "url": url,
            method: 'POST',
            contentType: 'application/json',
            dataType: 'json'
        }).then(
            function success(response) {
                console.log('document successful: ' + uploadUrl);
                if ( doneCallback ) {
                    doneCallback(true);
                }
            },
            function error(response) {
                if ( doneCallback ) {
                    doneCallback(false);
                }
            });
    };


});

