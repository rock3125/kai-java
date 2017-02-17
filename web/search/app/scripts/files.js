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
 * @name searchApp.controller:FilesController
 * @description
 * # FilesController
 * Controller of the searchApp
 */
angular.module('searchApp')
.controller('FilesController', function( $scope, $location, globalSvc, documentSvc, documentViewerSvc, fileUploadSvc ) {

    var session = null;

    $scope.myFile = null;
    $scope.documentList = [];

    $scope.user_tab_list = []; // user tabs

    /////////////////////////////////////////////////////////////////////

    $scope.compareClick = function() {
        console.log( 'file ' + $scope.myFile );
        if ( $scope.myFile ) {
            console.log('compare upload');
            documentSvc.documentCompare(session, $("#documentUpload").val(), 0.5, 0, $scope.myFile, $scope.compareCallback);
        }
    };

    // saved?
    $scope.compareCallback = function(data) {
        console.log('done');
        if ( data && data.kMeansValueList ) {
            $scope.documentList = data.kMeansValueList;
            $.each($scope.documentList, function(i, doc) {
                doc.percent = (parseInt(doc.distance * 10000.0) / 100.0) + "%";
            });
            // documentSvc.getDocumentList(session, paginator.prevUrl, paginator.page, paginator.itemsPerPage,
            //     $scope.filter, $scope.getDocumentListDone);
        }
    };

    // get the details for a url item
    $scope.details = function(url) {
        if ( url && session ) {
            documentViewerSvc.show(session, url);
        }
    };

    /////////////////////////////////////////////////////////////////////


    // setup controls
    // sliderControl = $("#searchDistance").slider();
    // sliderControl.slider('setValue', 0);


    $scope.signout = function() {
        globalSvc.setSession(null);
        $location.path("/");
    };

    $scope.get_tab_object = function(name, type) {
        var obj = null;
        $.each($scope.user_tab_list, function(i, tab) {
            if (tab.tab_name == name && tab.type == type) {
                obj = tab;
            }
        });
        return obj;
    };

    // click the user tab
    $scope.user_tab = function(name, type) {
        var obj = $scope.get_tab_object(name, type);
        if ( obj ) {
            globalSvc.setObject("kb_tab", {"tab_name": name,
                "type": type, "field_list": obj.field_list,
                "html_template": obj.html_template});
            $location.path("/user-tab")
        }
    };

    $scope.userCallback = function(user) {
        $scope.user_tab_list = user.user_tab_list;
        if ($scope.user_tab_list == null) {
            $scope.user_tab_list = [];
        }
    };

    // signed in?
    globalSvc.getSession( function(pSession) {
        if ( !pSession ) {
            console.log("logout");
            $scope.signout();
        } else {
            session = pSession;
        }
    }, $scope.userCallback);


})

.service('fileUploadSvc', function() {

    var service = this;
    service.filename = null;
})


.directive("fileread", [function () {
    return {
        scope: {
            fileread: "="
        },
        link: function (scope, element, attributes) {
            element.bind("change", function (changeEvent) {
                var reader = new FileReader();
                reader.onload = function (loadEvent) {
                    scope.$apply(function () {
                        scope.fileread = loadEvent.target.result;
                    });
                };
                reader.readAsDataURL(changeEvent.target.files[0]);
            });
        }
    }
}]);


