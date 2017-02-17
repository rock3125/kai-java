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
 * @name searchApp.controller:ExpertsController
 * @description
 * # ExpertsController
 * Controller of the searchApp
 */
angular.module('searchApp')
    .controller('ExpertsController', function( $scope, $location, globalSvc, searchSvc ) {

    var session = null;
    var sliderControl = null;
    var minPercentage = 0.1;

    $scope.text = ""; // search text

    $scope.user_tab_list = []; // user tabs

    // setup the pie chart
    $scope.initChart = function(data) {

        var w = 600,                            // width
            h = 600,                            // height
            r = 300,                            // radius
            color = d3.scale.category20c();     // builtin range of colors

        var vis = d3.select("#expertVisualisation")
            .style("stroke", "white")
            .style("fill", "white")
            .append("svg:svg")              //create the SVG element inside the <body>
            .data([data])                   //associate our data with the document
            .attr("width", w)           //set the width and height of our visualization (these will be attributes of the <svg> tag
            .attr("height", h)
            .append("svg:g")                //make a group to hold our pie chart
            .attr("transform", "translate(" + r + "," + r + ")");  //move the center of the pie chart from 0, 0 to radius, radius

        var arc = d3.svg.arc()              //this will create <path> elements for us using arc data
            .outerRadius(r);

        var pie = d3.layout.pie()           //this will create arc data for us given a list of values
            .value(function(d) { return d.value; });    //we must tell it out to access the value of each element in our data array

        var arcs = vis.selectAll("g.slice")     //this selects all <g> elements with class slice (there aren't any yet)
            .data(pie)                          //associate the generated pie data (an array of arcs, each having startAngle, endAngle and value properties)
            .enter()                            //this will create <g> elements for every "extra" data element that should be associated with a selection. The result is creating a <g> for every object in the data array
            .append("svg:g")                //create a group to hold each slice (we will have a <path> and a <text> element associated with each slice)
            .attr("class", "slice");    //allow us to style things in the slices (like text)

        arcs.append("svg:path")
            .attr("fill", function(d, i) { return color(i); } ) //set the color for each slice to be chosen from the color function defined above
            .attr("d", arc);                                    //this creates the actual SVG path using the associated data (pie) with the arc drawing function

        arcs.append("svg:text")                                     //add a label to each slice
            .attr("transform", function(d) {                    //set the label's origin to the center of the arc
                //we have to make sure to set these before calling arc.centroid
                d.innerRadius = 0;
                d.outerRadius = r;
                return "translate(" + arc.centroid(d) + ")";        //this gives us a pair of coordinates like [50, 50]
            })
            .attr("text-anchor", "middle")                          //center the text on it's origin
            .text(function(d, i) { return data[i].label; });        //get the label from our original data array

    };

    // call back from the service -setup time view
    $scope.expertSetCallback = function(expertData) {
        $("#expertVisualisation").html("");
        if ( expertData && expertData.authorList) {
            var data = [];
            $.each(expertData.authorList, function(i, item) {
                data.push( { 'label': item.author, 'value': item.score } );
            });
            $scope.initChart(data);
        }
    };

    $scope.sliderValueToDocumentDistance = function(value) {
        if ( value == 1 ) return 250;
        if ( value == 2 ) return 50;
        return 0;
    };

    $scope.search = function() {
        console.log($scope.text);
        if ( $scope.text ) { // super search?
            var searchObj = { 'search_text': $scope.text }; // 'synset_set_list': ambiguousWordsSvc.getSelectedSynsets() };
            var distanceValue = $scope.sliderValueToDocumentDistance(sliderControl.slider('getValue'));
            searchSvc.expert_search(session, searchObj, minPercentage, $scope.expertSetCallback);
        } else {
            globalSvc.error('search term(s) empty');
        }
    };


    /////////////////////////////////////////////////////////////////////
    // setup controls

    sliderControl = $("#searchDistance2").slider();
    sliderControl.slider('setValue', 2);

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


});

