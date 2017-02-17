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
 * @ngdoc function
 * @name webApp.controller:DocumentManagerController
 * @description
 * # DocumentManagerController
 * Controller of the webApp
 */
angular.module('webApp')
    .controller('DocumentManagerController', function ($scope, $http, $cookies, globalSvc, documentSvc, documentViewerSvc) {

    var session = null;
    var email = '';

    $scope.filter = '';
    $scope.url = '';
    $scope.documentList = [];
    $scope.myFile = null;
    $scope.acl_list = '';
    $scope.origin = '';

    var paginator = new DocumentPagination('document-manager', 'pagination', 'documentService');

    $scope.next = function() {
        if (paginator.next()) {
            paginator.setupFilter($scope.filter);
            documentSvc.getDocumentList(session, paginator.prevUrl, paginator.page, paginator.itemsPerPage,
                                        $scope.filter, $scope.getDocumentListDone);
        }
    };

    $scope.prev = function() {
        if (paginator.prev()) {
            paginator.setupFilter($scope.filter);
            documentSvc.getDocumentList(session, paginator.prevUrl, paginator.page, paginator.itemsPerPage,
                                        $scope.filter, $scope.getDocumentListDone);
        }
    };

    ///////////////////////////////////////////////////////////////

    // dialog: show the details for a url item
    $scope.details = function(url) {
        if ( url && session ) {
            documentViewerSvc.show(session, url);
        }
    };

    ///////////////////////////////////////////////////////////////

    $scope.addGreen = function(str, type) {
        return str + "<img src='images/green-button.png' title='" + type + "' />&nbsp;"
    };

    $scope.addRed = function(str, type) {
        return str + "<img src='images/red-button.png' title='" + type + "' />&nbsp;"
    };

        // callback after list load done
    $scope.getDocumentListDone = function(documentList) {
        if ( documentList && documentList.document_list ) {
            console.log("document list gotten");
            $scope.documentList = documentList.document_list;

            // setup status of the parser, vectorizer etc
            $.each($scope.documentList, function(i, document) {
                var str = "";
                if ( document.ts_converted > 0 ) { str = $scope.addGreen(str,'text extracted'); }
                else { str = $scope.addRed(str,'text not yet extracted'); }
                if ( document.ts_converted > 0&& document.ts_parsed >= document.ts_converted )
                { str = $scope.addGreen(str,'parsed'); }
                else { str = $scope.addRed(str,'not yet parsed'); }
                if ( document.ts_converted > 0 && document.ts_vectorised >= document.ts_parsed && document.ts_vectorised >= document.ts_converted )
                { str = $scope.addGreen(str,'vectorised'); }
                else { str = $scope.addRed(str,'not vectorised'); }
                if ( document.ts_converted > 0 && document.ts_summarised >= document.ts_parsed && document.ts_vectorised >= document.ts_converted )
                { str = $scope.addGreen(str,'summarised'); }
                else { str = $scope.addRed(str,'not summarised'); }
                if ( document.ts_converted > 0 && document.ts_indexed >= document.ts_parsed && document.ts_vectorised >= document.ts_converted )
                { str = $scope.addGreen(str,'searchable'); }
                else { str = $scope.addRed(str,'not searchable'); }
                if ( document.ts_converted > 0 && document.ts_emotion_analysed >= document.ts_parsed && document.ts_vectorised >= document.ts_converted )
                { str = $scope.addGreen(str,'anomalies analysed'); }
                else { str = $scope.addRed(str,'anomalies not yet analysed'); }
                // if ( document.ts_converted > 0 && document.ts_knowledge_analysed >= document.ts_parsed && document.ts_vectorised >= document.ts_converted )
                // { str = $scope.addGreen(str,'knowlegde analysed'); }
                // else { str = $scope.addRed(str,'knowledge not yet analysed'); }
                // if ( document.ts_converted > 0 && document.ts_clustered >= document.ts_parsed && document.ts_vectorised >= document.ts_converted )
                // { str = $scope.addGreen(str,'clustered'); }
                // else { str = $scope.addRed(str,'not clustered'); }
                document.status = str;
            });

            paginator.setup($scope.documentList);
        } else {
            paginator.setup([]);
            $scope.documentList = [];
        }
    };

    $scope.newDocument = function() {
        console.log("new document");
        $("#modalTitle").html("new document");
        $scope.url = '';
        $scope.origin = '';
        $scope.acl_list = email;
        $scope.myFile = null;
        $('#addDocument').modal('show');
    };

    $scope.redo = function(url) {
       if ( url ) {
           documentSvc.redo(session, url);
       }
    };

    $scope.save = function() {
        if ( $scope.myFile && $scope.url.length > 0 && $scope.acl_list.length > 0 ) {
            var aclList = $scope.acl_list.split(",");
            documentSvc.upload(session, $scope.url, aclList, $scope.origin, $scope.myFile, $scope.saveCallback);
        } else {
            globalSvc.error("please complete all form values");
        }
    };

    // saved?
    $scope.saveCallback = function(success) {
        if ( success ) {
            globalSvc.info("document saved");
            $('#addDocument').modal('hide');
            //reset
            $scope.url = '';
            $scope.acl_list = '';
            $scope.myFile = null;
            // re-get the document list
            paginator.setupFilter($scope.filter);
            documentSvc.getDocumentList(session, paginator.prevUrl, paginator.page, paginator.itemsPerPage,
                                        $scope.filter, $scope.getDocumentListDone);
        }
    };

    $scope.edit = function(url) {
        var document = globalSvc.getObjectFromListByID($scope.documentList,'url',url);
        if ( document ) {
            $("#modalTitle").html("update document");
            $scope.url = document.url;
            $scope.origin = document.origin;
            var str = '';
            $.each(document.acl_set, function(i,acl) {
               if ( str.length > 0 ) {
                   str = str + ",";
               }
               str = str + acl.user_group;
            });
            $scope.acl_list = str;
            $('#addDocument').modal('show');
        }
    };

    $scope.delete = function(url) {
        var document = globalSvc.getObjectFromListByID($scope.documentList,'url',url);
        if ( document ) {
            bootbox.confirm("Are you sure you want to delete " + document.url + "?",
                function(result) {
                    if ( result ) {
                        documentSvc.deleteDocument(session, document.url, $scope.deleteResult);
                    }
                });
        }
    };

    $scope.view = function(url) {
        var document = globalSvc.getObjectFromListByID($scope.documentList,'url',url);
        if ( document ) {
            console.log("view " + document.url);
            documentSvc.viewDocument(session, document.url);
        }
    };

    // deleted?
    $scope.deleteResult = function (success) {
        if ( success ) {
            globalSvc.info("document deleted");
            // re-get the entity list
            paginator.setupFilter($scope.filter);
            documentSvc.getDocumentList(session, paginator.prevUrl, paginator.page, paginator.itemsPerPage,
                                        $scope.filter, $scope.getDocumentListDone);
        }
    };

    $scope.search = function() {
        console.log("apply filter:" + $scope.filter);
        paginator.setupFilter($scope.filter);
        documentSvc.getDocumentList(session, paginator.prevUrl, paginator.page, paginator.itemsPerPage,
                                    $scope.filter, $scope.getDocumentListDone)
    };



    // signed in?
    globalSvc.getSession( function(pSession) {
        if ( !pSession ) {
            globalSvc.goHome();
        } else {
            session = pSession;
            // get the entity list
            paginator.setupFilter($scope.filter);
            documentSvc.getDocumentList(session, paginator.prevUrl, paginator.page, paginator.itemsPerPage,
                                        $scope.filter, $scope.getDocumentListDone)
        }
    });


    // on enter in password field - try and login
    $("#search").keypress(function(e) {
        if(e.which == 13) {
            if ( !$scope.disabled ) {
                $scope.search();
            }
        }
    });

});
