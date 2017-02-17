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
 * @name webApp.controller:SearchController
 * @description
 * # SearchController
 * Controller of the webApp
 */
angular.module('webApp')
.controller('SearchController', function ($scope, $cookies, $location, globalSvc, searchSvc, documentSvc, documentViewerSvc) {

    var session = null;

    $scope.document_type = '';
    $scope.author = '';
    $scope.url = '';
    $scope.search_text = '';

    // the search results
    $scope.result_list = [];
    $scope.fragment_size = [];
    $scope.fragment_index = [];

    $scope.url_list = [];

    ///////////////////////////////////////////////////////////////
    // document details / popup

    // get the details for a url item
    $scope.details = function(url) {
        if ( url && session ) {
            documentViewerSvc.show(session, url);
        }
    };

    ///////////////////////////////////////////////////////////////

    var paginator = new Pagination('search', 'pagination', 'searchController');

    // set only 5 items per page for the semantic search screen
    paginator.itemsPerPage = 5;

    $scope.next = function() {
        if (paginator.next()) {
            var searchObj = $scope.getSearchObject();
            if ( searchObj != null ) {
                searchSvc.search(session, searchObj, paginator.page, paginator.itemsPerPage, $scope.searchDone);
            }
        }
    };

    $scope.prev = function() {
        if (paginator.prev()) {
            var searchObj = $scope.getSearchObject();
            if ( searchObj != null ) {
                searchSvc.search(session, searchObj, paginator.page, paginator.itemsPerPage, $scope.searchDone);
            }
        }
    };

    $scope.gotoPage = function(num) {
        if (paginator.gotoPage(num)) {
            var searchObj = $scope.getSearchObject();
            if ( searchObj != null ) {
                searchSvc.search(session, searchObj, paginator.page, paginator.itemsPerPage, $scope.searchDone);
            }
        }
    };


    ///////////////////////////////////////////////////////////////

    // inter-fragment navigation
    $scope.prevf = function(id) {
        if ( id >= 0 && id < $scope.fragment_index.length ) {
            var index = $scope.fragment_index[id];
            if ( index > 0 ) {
                index = index - 1;
                $scope.fragment_index[id] = index;
            }
        }
    };

    // inter-fragment navigation
    $scope.nextf = function(id) {
        if ( id >= 0 && id < $scope.fragment_index.length ) {
            var index = $scope.fragment_index[id];
            var length = $scope.fragment_size[id];
            if ( index < length ) {
                index = index + 1;
                $scope.fragment_index[id] = index;
            }
        }
    };

    // view a document by url (i.e. download it)
    $scope.view = function (url) {
        if ( url ) {
            console.log("view " + url);
            documentSvc.viewDocument(session, url);
        }
    };

    $scope.searchDone = function(data) {
        if ( data && data.search_result_list ) {

            // get results and reset indexes and sizes for the fragments
            $scope.result_list = data.search_result_list;
            $scope.fragment_size = [];
            $scope.fragment_index = [];
            $scope.url_list = [];

            paginator.setup(data.total_document_count);

            if ( $scope.result_list.length > 0 ) {

                $.each($scope.result_list, function(i, searchResult) {
                    var new_list = [];
                    $.each(searchResult.text_list, function (j, item) {
                        new_list.push($scope.setupHighlighting(item));
                    });
                    $scope.url_list.push(searchResult.url);
                    searchResult.index = i; // set an index on the result for processing
                    searchResult.result_list = new_list; // set the list of pretty html strings
                    // scroll through the fragment setup
                    $scope.fragment_size.push( new_list.length ); // length for boundary checking
                    $scope.fragment_index.push( 0 ); // the current index for the fragment
                });

                // set the url list for other pages to read
                globalSvc.setObject("url_list", $scope.url_list);

                $("#pagination").show();

            } else {

                $("#pagination").hide();

            }
        } else {
            $("#pagination").hide();
            console.log('search failed');
        }
    };

    // replace any search-string highlighting with html equivalent tags
    $scope.setupHighlighting = function(itemStr) {
        var str = itemStr.replace(/\{hl1\:\}/g, "<div class='hl1'>");
        str = str.replace(/\{hl1\:\}/g, "<div class='hl1'>");
        str = str.replace(/\{\:hl1\}/g, "</div>");
        str = str.replace(/\{hl2\:\}/g, "<div class='hl2'>");
        str = str.replace(/\{\:hl2\}/g, "</div>");
        str = str.replace(/\{hl3\:\}/g, "<div class='hl3'>");
        str = str.replace(/\{\:hl3\}/g, "</div>");
        return str;
    };

    // construct a search object for the service layer
    $scope.getSearchObject = function () {
        var searchObj = { 'metadata': '{body}' };
        if ( $scope.search_text != '' ) {
            searchObj.search_text = $scope.search_text;
        }
        if ( $scope.author != '' ) {
            searchObj.author = $scope.author;
        }
        if ( $scope.url != '' ) {
            searchObj.url = $scope.url;
        }
        if ( $scope.document_type != '' ) {
            searchObj.document_type = $scope.document_type;
        }
        if ( searchObj.document_type || searchObj.author || searchObj.url || searchObj.search_text ) {
            return searchObj;
        }
        return null;
    };

    // set the object back into the fields
    $scope.setSearchObj = function( searchObj ) {
      if ( searchObj ) {
          if ( searchObj.search_text ) {
              $scope.search_text = searchObj.search_text;
          }
          if ( searchObj.author ) {
              $scope.author = searchObj.author;
          }
          if ( searchObj.document_type ) {
              $scope.document_type = searchObj.document_type;
          }
          if ( searchObj.url ) {
              $scope.url = searchObj.url;
          }
      }
    };

    $scope.search = function () {
        if ( $scope.search_text.startsWith("(") ) {
            // reset pagination of searching through this function
            paginator.reset();
            searchSvc.superSearch(session, $scope.search_text, paginator.page, paginator.itemsPerPage, $scope.searchDone);
        } else {
            var searchObj = $scope.getSearchObject();
            if (searchObj != null) {
                // reset pagination of searching through this function
                paginator.reset();
                searchSvc.search(session, searchObj, paginator.page, paginator.itemsPerPage, $scope.searchDone);
            } else {
                globalSvc.error('search term(s) empty');
            }
        }
    };


    ///////////////////////////////////////////////////////////////

    // signed in?
    globalSvc.getSession( function(pSession) {

        if ( !pSession ) {

            globalSvc.goHome();

        } else {

            session = pSession;

            // do we have a previous search object? - if so - repeat the last search
            var searchObj = globalSvc.getObject( "saved_search" );
            if ( searchObj != null ) {
                $scope.setSearchObj(searchObj.searchObj);
                searchSvc.search(session, searchObj.searchObj, searchObj.page, searchObj.itemsPerPage, $scope.searchDone);
            }

        }

    });


});

