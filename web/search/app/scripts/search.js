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
 * @name searchApp.controller:searchService
 * @description
 * # searchService
 * Controller of the searchApp
 */
angular.module('searchApp')
.controller('SearchController', function ($scope, $location, $interval, $timeout, globalSvc, searchSvc, documentSvc,
                                       documentViewerSvc, paginationSvc, ambiguousWordsSvc,
                                       keywordSearchSvc, advancedSearchSvc, logicQuerySvc ) {

    var session = null;

    $scope.user_tab_list = []; // list of user tab items

    // which input type to use for a search
    $scope.searchType = 'query';

    // different displays
    $scope.displayType = '5 blue lines';

    // embedded search type callback
    $scope.callback = null;

    // the text to use for previous / next buttons
    $scope.prevNextText = '';

    // the search results
    $scope.result_list = [];
    $scope.fragment_size = [];
    $scope.fragment_index = [];
    $scope.url_list = [];

    // sigma data structure
    $scope.graphDrawn = false;
    $scope.entity_to_url = {};

    // breakdown arrays for the items
    $scope.pageSize = 15;
    $scope.people = [];
    $scope.peoplePage = 0;
    $scope.places = [];
    $scope.placesPage = 0;
    $scope.time = [];
    $scope.timePage = 0;

    // graph colourings
    var linkColour = '#aaa';        // light grey
    var fadedColour = '#ccc';       // not selected colour
    var personColour = '#d00040';   // brand red
    var locationColour = '#0e264d'; // dark blue
    var timeColour = '#5E7DAF';     // light blue


    // for viewing selected entities in the people/places/time view
    $scope.entity_view = [];

    var sliderControl = null;

    ///////////////////////////////////////////////////////////////

    $scope.prevPeople = function() {
        if ( $scope.peoplePage > 0 ) $scope.peoplePage = $scope.peoplePage - 1;
    };
    $scope.nextPeople = function() {
        if ( ($scope.peoplePage+1)* $scope.pageSize < $scope.people.length ) $scope.peoplePage = $scope.peoplePage + 1;
    };
    $scope.peopleList = function() {
        var list = [];
        var index = $scope.peoplePage * $scope.pageSize;
        var last = Math.min( index + $scope.pageSize, $scope.people.length );
        for ( var i = index; i < last; i++ ) {
            list.push( $scope.people[i] );
        }
        return list;
    };
    $scope.selectPerson = function(person) {
        $scope.viewEntityOccurrences(person);
    };

    $scope.prevLocation = function() {
        if ( $scope.placesPage > 0 ) $scope.placesPage = $scope.placesPage - 1;
    };
    $scope.nextLocation = function() {
        if ( ($scope.placesPage+1) * $scope.pageSize < $scope.places.length ) $scope.placesPage = $scope.placesPage + 1;
    };
    $scope.placesList = function() {
        var list = [];
        var index = $scope.placesPage * $scope.pageSize;
        var last = Math.min( index + $scope.pageSize, $scope.places.length );
        for ( var i = index; i < last; i++ ) {
            list.push( $scope.places[i] );
        }
        return list;
    };
    $scope.selectLocation = function(location) {
        $scope.viewEntityOccurrences(location);
    };


    $scope.prevTime = function() {
        if ( $scope.timePage > 0 ) $scope.timePage = $scope.timePage - 1;
    };
    $scope.nextTime = function() {
        if ( ($scope.timePage+1) * $scope.pageSize < $scope.time.length ) $scope.timePage = $scope.timePage + 1;
    };
    $scope.timeList = function() {
        var list = [];
        var index = $scope.timePage * $scope.pageSize;
        var last = Math.min( index + $scope.pageSize, $scope.time.length );
        for ( var i = index; i < last; i++ ) {
            list.push( $scope.time[i] );
        }
        return list;
    };
    $scope.selectTime = function(time) {
        $scope.viewEntityOccurrences(time);
    };

    $scope.viewEntityOccurrences = function(obj) {
        if ( obj && obj.word && obj.url_list ) {
            searchSvc.view_entities( session, { 'search_text': obj.word, 'url_list': obj.url_list }, $scope.viewEntityCallback );
        }
    };

    $scope.viewEntityCallback = function(data) {
        $scope.entity_view = [];
        if ( data && data.search_result_list ) {
            $.each( data.search_result_list, function(i,obj) {
                if ( obj.url && obj.text_list ) {
                    for ( var i = 0; i < obj.text_list.length; i++ ) {
                        var htmlStr = $scope.setupHighlighting(obj.text_list[i]);
                        $scope.entity_view.push( { 'url': obj.url, 'html': htmlStr } );
                    }
                }
            });
        }
    };

    ///////////////////////////////////////////////////////////////
    // search type

    $scope.selectSearchTypeUI = function(str) {
        if ( str ) {
            $scope.searchType = str;
            var q1 = $("#keywordSearch");
            var q2 = $("#nlSearch");
            var q3 = $("#logicQuery");
            var q4 = $("#advancedSearch");
            if ( str == 'query' ) {
                q1.show();
                q2.hide();
                q3.hide();
                q4.hide();
            } else if ( str == 'logic builder' ) {
                q1.hide();
                q2.hide();
                q3.show();
                q4.hide();
            } else if ( str == 'advanced search' ) {
                q1.hide();
                q2.hide();
                q3.hide();
                q4.show();
            // } else if ( str == 'natural language' ) {
            //     q1.hide();
            //     q2.show();
            //     q3.hide();
            //     q4.hide();
            }
        }
    };

    ///////////////////////////////////////////////////////////////

    // change the display type
    $scope.selectDisplay = function(str) {
        if ( str ) {
            $scope.displayType = str;
            var d1 = $("#div10BlueLines");
            var d2 = $("#divGraph");
            var d3 = $("#divPPT");
            var d4 = $("#divTime");
            var d5 = $("#divVisuals");
            if ( str == "5 blue lines" ) {
                d1.show();
                d2.hide();
                d3.hide();
                d4.hide();
                d5.hide();
            } else if ( str == "relationship graph" ) {
                d1.hide();
                d2.show();
                d3.hide();
                d4.hide();
                d5.hide();
                $timeout( function() {
                    if ( !$scope.graphDrawn ) {
                        $scope.graphDrawn = true;
                        $scope.setupSigma(null, null);
                    } }, 100 );
            } else if ( str == "people and places" ) {
                d1.hide();
                d2.hide();
                d3.show();
                d4.hide();
                d5.hide();
            } else if ( str == "visuals" ) {
                d1.hide();
                d2.hide();
                d3.hide();
                d4.hide();
                d5.show();
            } else {
                d1.hide();
                d2.hide();
                d3.hide();
                d4.show();
                d5.hide();
                $timeout( function() { $scope.drawTime(); }, 100 );
            }
        }
    };

    ///////////////////////////////////////////////////////////////
    // document details / popup

    // get the details for a url item
    $scope.details = function(url) {
        if ( url && session && url != 'KAI' ) {
            documentViewerSvc.show(session, url);
        }
    };

    // show images
    $scope.getImageUrl = function(item) {
        console.log("getImageUrl ");
        if ( session != null && item != null && item.url != null ) {
            return documentSvc.getDocumentImageUrl(session, item.url);
        }
        return "";
    };

    ///////////////////////////////////////////////////////////////

    $scope.next = function() {
        var text = $scope.prevNextText;
        if ( text.length > 0 ) {
            var searchObj = {'search_text': text, 'synset_set_list': ambiguousWordsSvc.getSelectedSynsets()};
            var distanceValue = $scope.sliderValueToDocumentDistance(sliderControl.slider('getValue'));
            searchSvc.search(session, searchObj, paginationSvc.page, paginationSvc.itemsPerPage, distanceValue, $scope.searchDone);
        }
    };

    $scope.prev = function() {
        var text = $scope.prevNextText;
        if ( text.length > 0 ) {
            var searchObj = {'search_text': text, 'synset_set_list': ambiguousWordsSvc.getSelectedSynsets()};
            var distanceValue = $scope.sliderValueToDocumentDistance(sliderControl.slider('getValue'));
            searchSvc.search(session, searchObj, paginationSvc.page, paginationSvc.itemsPerPage, distanceValue, $scope.searchDone);
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
        if ( url && session && url != 'KAI') {
            console.log("view " + url);
            documentSvc.viewDocument(session, url);
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

    ///////////////////////////////////////////////////////////////////////////////////////////////

    $scope.sliderValueToDocumentDistance = function(value) {
        if ( value == 1 ) return 250;
        if ( value == 2 ) return 50;
        return 0;
    };

    $scope.search = function ( text, callback ) {
        console.log(text);
        if ( text ) { // super search?
            $scope.prevNextText = text;
            // reset pagination of searching through this function
            $scope.callback = callback;
            paginationSvc.reset();
            var searchObj = { 'search_text': text, 'synset_set_list': ambiguousWordsSvc.getSelectedSynsets() };
            var distanceValue = $scope.sliderValueToDocumentDistance(sliderControl.slider('getValue'));
            searchSvc.search(session, searchObj, paginationSvc.page, paginationSvc.itemsPerPage, distanceValue, $scope.searchDone);
        } else {
            globalSvc.error('search term(s) empty');
        }
    };

    $scope.nl_search = function ( text, callback ) {
        console.log("nl_search: \"" + text + "\"");
        if ( text ) { // super search?
            // reset pagination of searching through this function
            $scope.callback = callback;
            paginationSvc.reset();
            var distanceValue = $scope.sliderValueToDocumentDistance(sliderControl.slider('getValue'));
            searchSvc.nl_search(session, text, paginationSvc.page, paginationSvc.itemsPerPage, $scope.searchDone);
        } else {
            globalSvc.error('nl_search term(s) empty');
        }
    };

    // setup service callbacks to here
    keywordSearchSvc.doSearchCallback = $scope.search;
    advancedSearchSvc.doSearchCallback = $scope.search;
    logicQuerySvc.doSearchCallback = $scope.search;
    //nlSearchSvc.doSearchCallback = $scope.nl_search;

    // text -> wav call
    $scope.speak_string = function(text) {
        if ( text && text.length > 0 ) {
            // this is google speech
            var synth = window.speechSynthesis;
            if ( synth ) {
                var utterance = new SpeechSynthesisUtterance(text);
                utterance.lang = 'en-GB';
                synth.speak(utterance);
            } else {
                // this is our own TTS System
                audio.src = globalSvc.getNodeRR("Speech") + 'speech/to-speech/' + encodeURIComponent(session) + "/" + encodeURIComponent(text);
                var v = document.getElementsByTagName("audio")[0];
                v.volume = 0.5;
                v.play();
            }
        }
    };

    // interrupt speech playback
    $scope.stop_talking = function () {
        var v = document.getElementsByTagName("audio")[0];
        v.pause();
        v.currentTime = 0;
    };

    // cleanup the string for speech compatibility
    $scope.cleanupForSpeech = function(str) {
        if ( str ) {
            var str1 = str.replace("KAI", "kai");
            str1 = str1.replace(" de ", "the");
            str1 = str1.replace("Vocht", "Vok");
            return str1;
        }
        return str;
    };

    // process search results
    $scope.searchDone = function(data) {
        if ( data && data.ai_answer) {
            console.log("received: ai_answer");
            $scope.speak_string($scope.cleanupForSpeech(data.ai_answer));
            $("#aimlResponse").show();
            $("#txtResult").html(data.ai_answer);

        } else if ( data && data.search_result_list ) {
            $("#aimlResponse").hide();
            console.log("received: " + data.search_result_list.length + " search results");
            $scope.graphDrawn = false; // reset

            // callback the calling control?
            if ( $scope.callback ) {
                $scope.callback(data);
            }

            // get results and reset indexes and sizes for the fragments
            $scope.result_list = data.search_result_list;
            $scope.fragment_size = [];
            $scope.fragment_index = [];
            $scope.url_list = [];

            paginationSvc.setupResults($scope.result_list);
            ambiguousWordsSvc.setup(data.synset_set_list);

            if ( $scope.result_list.length > 0 ) {

                $("#noResults").hide();

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

                // setup the sigma data structure for each item in the set
                $scope.entity_to_url = {};
                $.each( $scope.result_list, function(i, result) {
                    $scope.addEntity(result.url, 'person', result.person_set);
                    $scope.addEntity(result.url, 'location', result.location_set);
                    $scope.addEntity(result.url, 'time', result.time_set);
                });

                $scope.people = $scope.getTypeList('person');
                $scope.places = $scope.getTypeList('location');
                $scope.time = $scope.getTypeList('time');

                $scope.people.sort( function(str1,str2) { return (str1.word < str2.word) ? -1 : (str1.word > str2.word) ? 1 : 0; });
                $scope.places.sort( function(str1,str2) { return (str1.word < str2.word) ? -1 : (str1.word > str2.word) ? 1 : 0; });

                $("#pagination").show();

            } else {

                $scope.entity_to_url = {};
                $scope.people = [];
                $scope.places = [];
                $scope.time = [];

                $("#noResults").show();
                $("#pagination").hide();

            }

            // adjust drawing for specific interfaces depending on their selection
            if ( $scope.displayTime == "10 blue lines" ) {

            } else if ( $scope.displayType == "relationship graph" ) {
                $timeout(function () {
                    if (!$scope.graphDrawn) {
                        $scope.graphDrawn = true;
                        $scope.setupSigma(null, null);
                    }
                }, 100);

            } else if ( $scope.displayType == "people and places" ) {

            } else {
                $scope.drawTime();
            }


        } else {
            paginationSvc.setupResults([]);
            console.log('search failed');
        }
    };


    /////////////////////////////////////////////////////////////////////
    // sigma graph library

    // simple graph lookup - does list contain word == word and type == type?
    $scope.getExistingItem = function( word, type, list ) {
        var item = null;
        $.each(list, function(i, existingItem) {
            if ( existingItem.word == word && existingItem.type == type ) {
                item = existingItem;
            }
        });
        return item;
    };

    // process new set entries
    $scope.addEntity = function( url, type, set ) {

        $.each(set, function(word,count) {

            var item = $scope.entity_to_url[word];
            if ( item != null ) {
                item.count = item.count + count;
                item.url_list.push( url );
            } else {
                item = {};
                item.word = word;
                item.type = type;
                item.count = count;
                item.url_list = [];
                item.url_list.push( url );
                $scope.entity_to_url[word] = item;
            }
        });

    };

    $scope.getTypeList = function(typeStr) {
        var list = [];
        $.each($scope.entity_to_url, function( str, item ) {
            if ( item && item.type == typeStr && item.count > 0 ) {
                //console.log(item.word + " @ " + item.url_list.length);
                list.push( { 'word': item.word, 'count': item.count, 'url_list': item.url_list } );
            }
        });
        return list;
    };

    // return true if the lists share a common item
    $scope.url_related = function( list1, list2 ) {
        if ( list1 && list2 && list1.length > 0 && list2.length > 0 ){
            var set1 = {};
            for ( var i = 0; i < list1.length; i++ ) {
                set1[list1[i]] = true;
            }
            var counter = 0;
            for ( var j = 0; j < list2.length; j++ ) {
                if ( set1[list2[j]] ) {
                    counter = counter + 1;
                }
            }
            return counter;
        }
        return 0;
    };

    // Add a method to the graph model that returns an
    // object with every neighbors of a node inside:
    var sma = new sigma('sigma-container');
    if ( !sma.graph.neighbors ) {
        sigma.classes.graph.addMethod('neighbors', function (nodeId) {
            var k,
                neighbors = {},
                index = this.allNeighborsIndex[nodeId] || {};

            for (k in index)
                neighbors[k] = this.nodesIndex[k];

            return neighbors;
        });
    }

    // setup the standard diagram with ALL items in it
    $scope.setupSigma = function(word, type) {

        //this gets rid of all the ndoes and edges
        $("#sigma-container").empty();

        // Let's first initialize sigma:
        var s = new sigma('sigma-container');

        var nodeId = 1;
        var minCount = 4;
        $.each( $scope.people, function(i, person) {
            person.nodeId = 'n' + nodeId;
            s.graph.addNode({
                // Main attributes:
                id: 'n' + nodeId,
                label: person.word,
                // Display attributes:
                x: nodeId % 16,
                y: parseInt(nodeId / 16),
                size: minCount + person.count,
                color: personColour,
                originalColor: personColour,
                type: 'exact person'
            });
            nodeId = nodeId + 1;
        });

        var edgeId = 1;
        for ( var i = 0; i < $scope.people.length; i++ ) {
            for ( var j = i + 1; j < $scope.people.length; j++ ) {
                if ( $scope.url_related($scope.people[i].url_list, $scope.people[j].url_list) > 1 ) {
                    if ( word == null || (word === $scope.people[i].word && (type == 'person' || type == 'exact person')) ||
                        (word === $scope.people[j].word && (type == 'person' || type == 'exact person') ) ) {
                        s.graph.addEdge({
                            id: 'e' + edgeId,
                            // Reference extremities:
                            source: $scope.people[i].nodeId,
                            target: $scope.people[j].nodeId,
                            color: linkColour
                        });
                    }
                }
                edgeId = edgeId + 1;
            }
        }

        $.each( $scope.places, function(i, location) {
            location.nodeId = 'n' + nodeId;
            s.graph.addNode({
                // Main attributes:
                id: 'n' + nodeId,
                label: location.word,
                // Display attributes:
                x: nodeId % 16,
                y: parseInt(nodeId / 16),
                size: minCount + location.count,
                color: locationColour,
                originalColor: locationColour,
                type: 'exact location'
            });
            nodeId = nodeId + 1;
        });

        for ( var i = 0; i < $scope.people.length; i++ ) {
            for ( var j = 0; j < $scope.places.length; j++ ) {
                if ( $scope.url_related($scope.people[i].url_list, $scope.places[j].url_list) > 0 ) {
                    if ( word == null || (word === $scope.people[i].word && (type == 'person' || type == 'exact person') ) ||
                        (word === $scope.places[j].word && (type == 'location' || type == 'exact location')) ) {
                        s.graph.addEdge({
                            id: 'e' + edgeId,
                            // Reference extremities:
                            source: $scope.people[i].nodeId,
                            target: $scope.places[j].nodeId,
                            color: linkColour
                        });
                    }
                }
                edgeId = edgeId + 1;
            }
        }

        $.each( $scope.time, function(i, time) {
            time.nodeId = 'n' + nodeId;
            s.graph.addNode({
                // Main attributes:
                id: 'n' + nodeId,
                label: time.word,
                // Display attributes:
                x: nodeId % 16,
                y: parseInt(nodeId / 16),
                size: minCount + time.count,
                color: timeColour,
                originalColor: timeColour,
                type: 'exact time'
            });
            nodeId = nodeId + 1;
        });

        for ( var i = 0; i < $scope.people.length; i++ ) {
            for ( var j = 0; j < $scope.time.length; j++ ) {
                if ( $scope.url_related($scope.people[i].url_list, $scope.time[j].url_list) > 0 ) {
                    if ( word == null || (word === $scope.people[i].word && (type == 'person' || type == 'exact person') ) ||
                        (word === $scope.time[j].word && (type == 'time' || type == 'exact time')) ) {
                        s.graph.addEdge({
                            id: 'e' + edgeId,
                            // Reference extremities:
                            source: $scope.people[i].nodeId,
                            target: $scope.time[j].nodeId,
                            color: linkColour
                        });
                    }
                }
                edgeId = edgeId + 1;
            }
        }

        for ( var i = 0; i < $scope.places.length; i++ ) {
            for ( var j = 0; j < $scope.time.length; j++ ) {
                if ( $scope.url_related($scope.places[i].url_list, $scope.time[j].url_list) > 0 ) {
                    if ( word == null || (word === $scope.places[i].word && (type == 'location' || type == 'exact location') ) ||
                        (word === $scope.time[j].word && (type == 'time' || type == 'exact time')) ) {
                        s.graph.addEdge({
                            id: 'e' + edgeId,
                            // Reference extremities:
                            source: $scope.places[i].nodeId,
                            target: $scope.time[j].nodeId,
                            color: linkColour
                        });
                    }
                }
                edgeId = edgeId + 1;
            }
        }


        // GRAPH EVENTS - sigma click

        // Finally, let's ask our sigma instance to refresh:
        //s.startForceAtlas2();
        var listener = sigma.layouts.fruchtermanReingold.start(s, {iterations: 5000});

        // Bind the events: overNode outNode clickNode doubleClickNode rightClickNode
        s.bind('clickNode', function(e) {
            // console.log(e.type, e.data.node.label, e.data.captor);
            if ( e.data.captor.shiftKey ) { // shift click = add to query
                var typeStr = e.data.node.type;
                if ( typeStr ) {
                    if ( typeStr == 'exact time' ) {
                        var str = e.data.node.label;
                        str = str.replace(/-/g,"/");
                        var dateTime = str.split(" ");
                        $scope.addComponentWithType(typeStr, null, dateTime[0], dateTime[1], null, null);
                    } else {
                        $scope.addComponentWithType(typeStr, e.data.node.label, null, null, null, null);
                    }
                    $scope.$apply();
                }
            } else { // click = zoom
                //s.cameras[0].goTo({x: e.data.node['read_cam0:x'], y: e.data.node['read_cam0:y'], ratio: 0.065});
                var nodeId = e.data.node.id,
                    toKeep = s.graph.neighbors(nodeId);
                toKeep[nodeId] = e.data.node;

                s.graph.nodes().forEach(function(n) {
                    if (toKeep[n.id])
                        n.color = n.originalColor;
                    else
                        n.color = fadedColour;
                });

                s.graph.edges().forEach(function(e) {
                    if (toKeep[e.source] && toKeep[e.target])
                        e.color = e.originalColor;
                    else
                        e.color = fadedColour;
                });

                // Since the data has been modified, we need to
                // call the refresh method to make the colors
                // update effective.
                s.refresh();
            }
        });

        // s.bind('rightClickNode', function(e) {
        //     var str = e.data.node.label;
        //     var typeStr = e.data.node.type;
        //     // console.log(e.type, e.data.node.label, e.data.captor);
        //     $scope.setupSigma(str, typeStr);
        //     s.cameras[0].goTo({x: e.data.node['read_cam0:x'], y: e.data.node['read_cam0:y'], ratio: 0.1});
        // });

        // When the stage is clicked, we just color each
        // node and edge with its original color.
        s.bind('clickStage', function(e) {
            s.graph.nodes().forEach(function(n) {
                n.color = n.originalColor;
            });

            s.graph.edges().forEach(function(e) {
                e.color = linkColour;
            });

            // Same as in the previous event:
            s.refresh();
        });


        s.refresh();

    };


    /////////////////////////////////////////////////////////////////////
    // time line display

    $scope.selectedItem = null;

    // draw the people
    $scope.drawPerson = function(vis, item, x, y, colour) {
        vis.append("rect")
            .attr("rx", 2)
            .attr("ry", 2)
            .attr("x", x )
            .attr("y", y )
            .attr("width", 125)
            .attr("height", 25)
            .style("fill", colour)
            .on("click", function() {
                $scope.selectedItem = item;
                $scope.drawTime();
            })
            .append("svg:title")
            .text(item.word);

        vis.append("text")
            .attr("x", x + 20)
            .attr("y", y + 15)
            .attr("text-anchor", "left")
            .style("font-size", "12px")
            .style("fill", "white")
            .on("click", function() {
                $scope.selectedItem = item;
                $scope.drawTime();
            })
            .text(item.word);
    };


    // draw the locations
    $scope.drawLocation = function(vis, item, x, y, colour) {
        vis.append("rect")
            .attr("rx", 2)
            .attr("ry", 2)
            .attr("x", x )
            .attr("y", y )
            .attr("width", 125)
            .attr("height", 25)
            .style("fill", colour)
            .on("click", function() {
                $scope.selectedItem = item;
                $scope.drawTime();
            })
            .append("svg:title")
            .text(item.word);

        vis.append("text")
            .attr("x", x + 20)
            .attr("y", y + 15)
            .attr("text-anchor", "left")
            .style("font-size", "12px")
            .style("fill", "white")
            .on("click", function() {
                $scope.selectedItem = item;
                $scope.drawTime();
            })
            .text(item.word);
    };

    // word, offset, type
    $scope.addTimelineItem = function( list, item ) {
        var existing = null;
        $.each( list, function(i, obj) {
            if ( obj.word == item.word && obj.type == item.type ) {
                existing = obj;
            }
        });
        if ( existing == null ) {
            list.push( { "word": item.word, "type": item.type, "offset_list": [item.offset] } );
        } else {
            existing.offset_list.push(item.offset);
        }
    };

    $scope.drawConnectingLine = function( vis, x1, y1, x2, y2, offset, colour ) {
        vis.append("line")
            .style("stroke", colour)
            .attr("x1", x1)
            .attr("y1", y1)
            .attr("x2", x1 + offset)
            .attr("y2", y1);
        vis.append("line")
            .style("stroke", colour)
            .attr("x1", x1 + offset)
            .attr("y1", y1)
            .attr("x2", x1 + offset)
            .attr("y2", y2 - offset);
        vis.append("line")
            .style("stroke", colour)
            .attr("x1", x1 + offset)
            .attr("y1", y2 - offset)
            .attr("x2", x2)
            .attr("y2", y2 - offset);
        vis.append("line")
            .style("stroke", colour)
            .attr("x1", x2)
            .attr("y1", y2 - offset)
            .attr("x2", x2)
            .attr("y2", y2);
    };

    $scope.prevPeople2 = function() {
        if ( $scope.peoplePage > 0 ) {
            $scope.peoplePage = $scope.peoplePage - 1;
            $scope.drawTime();
        }
    };
    $scope.nextPeople2 = function() {
        if ( ($scope.peoplePage+1)* $scope.pageSize < $scope.people.length ) {
            $scope.peoplePage = $scope.peoplePage + 1;
            $scope.drawTime();
        }
    };

    // draw time
    $scope.drawTime = function() {

        $("#gridVisualisation").html("");
        var vis = d3.select("#gridVisualisation");
        $scope.time.sort( function(a,b) { return a.count - b.count; } );

        // determine the extends - min to max month
        var minYear = 10000;
        var maxYear = 0;
        var minMonth = 10000;
        var maxMonth = 0;

        for ( var i = 0; i < $scope.time.length; i++ ) {
            var items = $scope.time[i].word.split('-');
            if ( items.length >= 2 ) {
                var year = parseInt(items[0]);
                var month = parseInt(items[1]);
                if ( year < minYear ) {
                    minYear = year;
                    minMonth = month;
                } else if ( year == minYear ) {
                    if ( month < minMonth ) {
                        minMonth = month;
                    }
                }
                if ( year > maxYear ) {
                    maxYear = year;
                    maxMonth = month;
                } else if ( year == maxYear ) {
                    if ( month > maxMonth ) {
                        maxMonth = month;
                    }
                }
            }
        }

        // draw time line if appropriate
        if ( minYear < 10000 && maxYear > 0 ) {
            var tl_left = 150;
            var tl_top = 500;
            var width = 1000;
            var yearDiff = ((maxYear - minYear) + 1) * 12;
            if ( yearDiff == 0 ) {
                yearDiff = 12; // 12 months
            }
            console.log("min:" + minYear + "/" + minMonth + " to " + maxYear + "/" + maxMonth + ", diff:" +yearDiff );

            vis.append("line")
                .style("stroke", "#cccccc")
                .attr("x1", tl_left)
                .attr("y1", tl_top)
                .attr("x2", tl_left + width)
                .attr("y2", tl_top);

            // draw time divisions and dates
            var x_start = tl_left;
            var month_distance = width / yearDiff;
            var yr = minYear;
            var m = 1;
            for ( var i = 0; i < yearDiff; i++ ) {
                vis.append("line")
                    .style("stroke", "#333333")
                    .attr("x1", x_start)
                    .attr("y1", tl_top - 5)
                    .attr("x2", x_start)
                    .attr("y2", tl_top);

                vis.append("text")
                    .attr("transform", function(d) {
                        return "translate(" + x_start + "," + (tl_top+15) + ") rotate(55)"
                    })
                    .text("" + yr + "/" + m + "/1");

                m = m + 1;
                if ( m > 12 ) {
                    m = 1;
                    yr = yr + 1;
                }
                x_start = x_start + month_distance;
            }

            var peopleStart = $scope.peoplePage * $scope.pageSize;
            var peopleEnd = peopleStart + $scope.pageSize;
            if ( peopleEnd > $scope.people.length ) {
                peopleEnd = $scope.people.length;
                if ( peopleStart > peopleEnd ) {
                    peopleStart = peopleEnd;
                }
            }

            var timeStart = 0;
            var timeEnd = timeStart + $scope.pageSize;
            if ( timeEnd > $scope.time.length ) {
                timeStart = 0;
                timeEnd = $scope.time.length;
            }

            var placeStart = 0;
            var placeEnd = placeStart + $scope.pageSize;
            if ( placeEnd > $scope.places.length ) {
                placeStart = 0;
                placeEnd = $scope.places.length;
            }

            var itemList = [];
            for ( var i = peopleStart; i < peopleEnd; i++ ) {
                for ( var j = timeStart; j < timeEnd; j++ ) {
                    if ( $scope.url_related($scope.people[i].url_list, $scope.time[j].url_list) > 0 ) {
                        var items = $scope.time[j].word.split('-');
                        if ( items.length >= 2 ) {
                            var o1 = (parseInt(items[0])-minYear) * 12 + (parseInt(items[1])-1) + (parseInt(items[2]) / 31);
                            var o2 = parseInt(o1 * month_distance);
                            var obj = {'word': $scope.people[i].word, 'offset': tl_left + o2, 'type': 'person' };
                            $scope.addTimelineItem(itemList, obj);
                        }
                    }
                }
            }
            for ( var i = placeStart; i < placeEnd; i++ ) {
                for ( var j = timeStart; j < timeEnd; j++ ) {
                    if ( $scope.url_related($scope.places[i].url_list, $scope.time[j].url_list) > 0 ) {
                        var items = $scope.time[j].word.split('-');
                        if ( items.length >= 2 ) {
                            var o1 = (parseInt(items[0])-minYear) * 12 + (parseInt(items[1])-1) + (parseInt(items[2]) / 31);
                            var o2 = parseInt(o1 * month_distance);
                            var obj = {'word': $scope.places[i].word, 'offset': tl_left + o2, 'type': 'location' };
                            $scope.addTimelineItem(itemList, obj);
                        }
                    }
                }
            }

            var box_height = 30;
            var x = 100;
            var y = 10;
            var line_left = 125;
            var line_offset = 20;
            $.each(itemList, function(i, item) {
                var colour;
                if ( item.type == 'person' ) {
                    colour = "#aaaaaa";
                    if ( $scope.selectedItem != null && $scope.selectedItem.word == item.word ) {
                        colour = "#ee6666";
                    }
                    $scope.drawPerson(vis, item, x, y, colour);
                } else {
                    colour = "#666666";
                    if ( $scope.selectedItem != null && $scope.selectedItem.word == item.word ) {
                        colour = "#ee6666";
                    }
                    $scope.drawLocation(vis, item, x, y, colour);
                }
                $.each(item.offset_list, function (j, offset) {
                    $scope.drawConnectingLine(vis, x + line_left, y + 15, offset, tl_top, line_offset, "#cccccc");
                });
                y = y + box_height;
                line_offset = line_offset + 10;
                if (y > 10 + box_height * 8) {
                    y = 10;
                    x = x + 250;
                    line_left = 125;
                    line_offset = 20;
                }
            });

            // draw red selection lines separately afterwards for more emphasis
            if ($scope.selectedItem != null) {
                x = 100;
                y = 10;
                $.each(itemList, function (i, item) {
                    if ($scope.selectedItem.word == item.word) {
                        $.each(item.offset_list, function (j, offset) {
                            $scope.drawConnectingLine(vis, x + line_left, y + 15, offset, tl_top, line_offset, "red");
                        });
                    }
                    y = y + box_height;
                    line_offset = line_offset + 10;
                    if (y > 10 + box_height * 8) {
                        y = 10;
                        x = x + 250;
                        line_left = 125;
                        line_offset = 20;
                    }
                });
            }


        } // if year within range

    };



    /////////////////////////////////////////////////////////////////////


    // setup controls
    sliderControl = $("#searchDistance").slider();
    sliderControl.slider('setValue', 0);

    // display and icon types for AI display items
    $scope.icon_from_url = function(url) {
        if (url && url == 'KAI') {
            return "glyphicon-asterisk";
        }
        return "glyphicon-search";
    };

    // display and icon types for AI display items
    $scope.is_visible = function(url) {
        return url != 'KAI';
    };


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
        console.log('user tab count:' + $scope.user_tab_list.length);
        $timeout( function() { $scope.$apply(); }, 100 );
    };

    // signed in?
    globalSvc.getSession( function(pSession) {
        if ( !pSession ) {
            console.log("logout");
            $scope.signout();
        } else {
            session = pSession;
            keywordSearchSvc.session = pSession;
            paginationSvc.setup( $scope.prev, $scope.next );
        }
    }, $scope.userCallback);


});

