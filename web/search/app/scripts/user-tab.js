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
.controller('UserTabController', function( $scope, $location, globalSvc, ambiguousWordsSvc,
                                           keywordSearchSvc, entitySvc ) {

    var session = null;
    var distanceValue = 50;  // some distance value for searching

    // tab controls / information
    $scope.user_tab_list = [];
    $scope.field_list = [];
    $scope.tab_name = null;
    $scope.html_template = null;
    $scope.type = null;
    $scope.selectedField = '';

    // search
    $scope.prevNextText = '';
    $scope.result_list = [];


    $scope.selectField = function(field) {
        $scope.selectedField = field;
    };

    /////////////////////////////////////////////////////////////////////////////////////////////

    // page url, ul-name, div page name
    var paginator = new EntityPagination('user-tab', 'pagination', 'UserTabController');

    $scope.next = function() {
        if (paginator.next()) {
            entitySvc.getEntityList(session, $scope.type, paginator.entityUrl, paginator.items_per_page,
                $scope.selectedField, $scope.prevNextText, $scope.searchDone);
        }
    };

    $scope.prev = function() {
        if (paginator.prev()) {
            entitySvc.getEntityList(session, $scope.type, paginator.entityUrl, paginator.items_per_page,
                $scope.selectedField, $scope.prevNextText, $scope.searchDone);
        }
    };

    $scope.gotoPage = function(num) {
        if (paginator.gotoPage(num)) {
            entitySvc.getEntityList(session, $scope.type, paginator.entityUrl, paginator.items_per_page,
                $scope.selectedField, $scope.prevNextText, $scope.searchDone);
        }
    };

    ///////////////////////////////////////////////////////////////////////////////////////////////


    // setup search result list when results come back formatted accordingly
    $scope.searchDone = function(result) {
        if (result && result.list) {
            var result_list = [];
            $.each(result.list, function(i, item) {
                // parse the json
                if (item && item.json_data) {
                    var content_object = JSON.parse(item.json_data);
                    if (content_object) {
                        // create html?  or template provided?
                        if ($scope.html_template) {
                            content_object.html = $scope.html_template;
                            // replace all fields
                            $.each($scope.field_list, function(j,field) {
                                if (content_object[field]) {
                                    content_object.html = content_object.html.replace('<' + field + '>', content_object[field]);
                                } else {
                                    content_object.html = content_object.html.replace('<' + field + '>', '');
                                }
                            });
                        } else {
                            var str = "";
                            $.each($scope.field_list, function(j,field) {
                                if (content_object[field]) {
                                    if (str.length > 0) {
                                        str += ", ";
                                    }
                                    str += content_object[field];
                                }
                            });
                            content_object.html = str;
                        }
                        content_object.id = item.id;
                        result_list.push(content_object);
                    }
                }
            });
            paginator.setup(result_list);
            $scope.result_list = result_list;
        } else {
            paginator.setup([]);
            $scope.result_list = [];
        }
    };

    // perform a search
    $scope.search = function(text) {
        console.log(text);
        if ( text ) { // super search?
            $scope.prevNextText = text;
            // reset pagination of searching through this function
            var searchObj = { 'search_text': text, 'synset_set_list': ambiguousWordsSvc.getSelectedSynsets() };
            entitySvc.getEntityList(session, $scope.type, paginator.entityUrl, paginator.items_per_page,
                $scope.selectedField, text, $scope.searchDone);
        } else {
            globalSvc.error('search term(s) empty');
        }
    };

    /////////////////////////////////////////////////////////////////////
    // setup controls

    $scope.signout = function() {
        globalSvc.setSession(null);
        $location.path("/");
    };

    // click the user tab
    $scope.user_tab = function(name, type) {
        globalSvc.setObject("kb_tab", {"tab_name": name, "type": type});
        $location.path("/user-tab")
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

            keywordSearchSvc.doSearchCallback = $scope.search;
            keywordSearchSvc.session = session;

            var kb_tab = globalSvc.getObject("kb_tab"); //, {"tab_name": name, "type": type});
            if (kb_tab && kb_tab.field_list && kb_tab.field_list.length > 0) {
                $scope.html_template = kb_tab.html_template;
                $scope.tab_name = kb_tab.tab_name;
                $scope.type = kb_tab.type;
                $scope.field_list = kb_tab.field_list;
                $scope.selectedField = $scope.field_list[0];
            } else {
                console.log("kb_tab not set or empty field list");
            }
        }
    }, $scope.userCallback);


});

