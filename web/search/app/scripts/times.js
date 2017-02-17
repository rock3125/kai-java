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
.controller('TimeController', function( $scope, $location, globalSvc, documentSvc, documentViewerSvc, timeSvc ) {

    var session = null;

    var year1 = 2001;
    var month1 = 12;
    var day1 = -1;
    var hour1 = -1;

    var page = 0;
    var pageSize = 50;

    $scope.timeUrlList = [];

    $scope.user_tab_list = []; // user tabs

    // setup a chart
    $scope.initChart = function() {

        var vis = d3.select("#timeVisualisation");

        // label the graph
        var dateStr = monthOfYear[month1 - 1] + " " + year1;
        $("#date1").val(dateStr);
        // doesn't seem to exist
        //$('#datetimepicker1').data("DateTimePicker").date(dateStr);

        var width = 1000;
        var height = 600;
        var margins = {
            top: 110,
            right: 20,
            bottom: 20,
            left: 50
        };

        var maxx = 35;
        var miny = 0;
        var mx = $scope.getMaxX();
        var maxy = mx * 2;
        if ( maxy < 30 ) {
            maxy = 30;
        }
        var circleSize = 7 - parseInt(mx / 50);

        var xScale = d3.scale.linear().range([margins.left, width - margins.right]).domain([0, maxx]);
        var yScale = d3.scale.linear().range([height - margins.top, margins.bottom]).domain([miny, maxy]);

        var xAxis = d3.svg.axis()
            .scale(xScale);

        var yAxis = d3.svg.axis()
            .scale(yScale)
            .orient("left");

        vis.append("svg:g")
            .attr("class", "x axis")
            .attr("transform", "translate(0," + (height - (margins.top - 5)) + ")")
            .call(xAxis);

        vis.append("svg:g")
            .attr("class", "y axis")
            .attr("transform", "translate(" + (margins.left) + ",0)")
            .call(yAxis);

        // draw the graph
        var dayCounter = {};
        var rainbowTableCounter = 0;
        var rainbowTableSet = {};

        var entitySet = {};
        $.each($scope.timeUrlList, function(i, timeUrl) {

            // assing a unique colour to each url
            var entity_name = timeUrl.entity_name;

            // re-get object?
            var entityObj;
            if ( entitySet[entity_name] == undefined ) {
                entityObj = { 'name': entity_name, 'urls': timeUrl.urlList };
            } else {
                entityObj = entitySet[entity_name];
            }

            // make a unique list of the URLs involved
            var urlStr = "";
            var urlDetective = {};
            $.each( timeUrl.urlList, function(i, url) {
                if ( urlDetective[url] == undefined ) {
                    urlDetective[url] = true;
                    if (urlStr != "") {
                        urlStr = urlStr + ", " + url;
                    } else {
                        urlStr = url;
                    }
                }
            });

            // find a good colour for this item
            var colour;
            if ( rainbowTableSet[entity_name] == undefined ) {
                colour = colourPalette[rainbowTableCounter];
                rainbowTableCounter = (rainbowTableCounter + 53) % colourPalette.length;
                rainbowTableSet[entity_name] = colour;
            } else {
                colour = rainbowTableSet[entity_name];
            }

            entityObj['colour'] = colour;

            // count the days for the month graphs
            var day = timeUrl.day;
            var value = 0;
            if ( dayCounter[day] != undefined ) {
                value = dayCounter[day];
                value = value + 1;
                dayCounter[day] = value;
            } else {
                dayCounter[day] = value;
            }

            // draw a little circle on the location
            var x = xScale(day);
            var y = yScale(value);
            var circleId = "circle_" + i;
            vis.append("circle")
                .attr("id", circleId)
                .attr("cx", x )
                .attr("cy", y )
                .attr("class", "y")
                .style("fill", colour)
                .style("stroke", "black")
                .attr("r", 7)
                .on("click", function() {
                    $scope.openUrls(timeUrl.urlList);
                })
                .append("svg:title")
                .text(entity_name + ": " + urlStr);

            // setup the name of this person
            if ( entityObj['circle_list'] == undefined ) {
                entityObj['circle_list'] = [circleId];
            } else {
                entityObj['circle_list'].push(circleId);
            }
            entityObj['flag'] = 0;
            entitySet[entity_name] = entityObj;

        });

        // order entity names alphabetically by constructing a list of the names
        // and ids mixed
        var sortSet = [];
        $.each( entitySet, function( id, obj ) {
            sortSet.push(obj.name.capitalizeFirstLetter() + "|" + id);
        });
        sortSet.sort();

        // draw all the different people involved
        var index = 0;
        var top_y = 50;
        var line_height = 15;
        var person_x = 1010;
        var person_y = top_y;
        $.each( sortSet, function( i, name) {

            var uuid = name.split("|");
            var id = uuid[uuid.length - 1];
            var obj = entitySet[id];

            var circleId = "person_circle_" + index;
            vis.append("circle")
                .attr("id", circleId)
                .attr("cx", person_x )
                .attr("cy", person_y )
                .attr("class", "y")
                .style("fill", obj.colour)
                .style("stroke", "black")
                .attr("r", 5)
                .on("click", function() {
                    if ( obj.circle_list ) {
                        var flag = obj['flag'];
                        $.each(obj.circle_list, function(i, circleId) {
                            if ( flag == 0 ) {
                                d3.select("#" + circleId).style("fill", "white");
                                d3.select("#" + circleId).style("stroke", "red");
                            } else {
                                d3.select("#" + circleId).style("fill", obj.colour);
                                d3.select("#" + circleId).style("stroke", "black");
                            }
                        });
                        if ( flag == 0 ) {
                            d3.select("#" + circleId).style("fill", "white");
                            d3.select("#" + circleId).style("stroke", "red");
                            obj['flag'] = 1;
                        } else {
                            d3.select("#" + circleId).style("fill", obj.colour);
                            d3.select("#" + circleId).style("stroke", "black");
                            obj['flag'] = 0;
                        }
                    }
                })
                .append("svg:title")
                .text(obj.name);

            var text = obj.name;
            if ( text.length > 20 ) {
                text = text.substring(0,20);
            }

            vis.append("text")
                .attr("x", person_x + 10)
                .attr("y", person_y)
                .attr("text-anchor", "left")
                .style("font-size", "12px")
                .style("text-decoration", "underline")
                .text(text);

            index = index + 1;
            person_y = person_y + line_height;

            if ( index % 30 == 0 ) {
                person_y = top_y;
                person_x = person_x + 140;
            }
        });


    };

    ///////////////////////////////////////////////////////////////////////////
    // paginated sets

    $scope.prev = function() {
        if ( page > 0 ) {
            page = page - 1;
            $("#timeVisualisation").html("");
            timeSvc.getTimeSet(session, year1, month1, day1, hour1, 0,
                year1, month1, day1, hour1, 0, page, pageSize, $scope.timeSetCallback);
        }
    };

    $scope.next = function() {
        var size = $scope.timeUrlList.length;
        if ( size >= pageSize ) {
            $("#timeVisualisation").html("");
            page = page + 1;
            timeSvc.getTimeSet(session, year1, month1, day1, hour1, 0,
                year1, month1, day1, hour1, 0, page, pageSize, $scope.timeSetCallback);
        }
    };



    ///////////////////////////////////////////////////////////////////////////
    // previous/next month

    $scope.previousMonth = function() {
        month1 = month1 - 1;
        if ( month1 < 1) {
            month1 = 12;
            year1 = year1 - 1;
        }
        $("#timeVisualisation").html("");
        globalSvc.setObject("last-month-time", { 'year': year1, 'month': month1 });
        timeSvc.getTimeSet(session, year1, month1, day1, hour1, 0,
            year1, month1, day1, hour1, 0, page, pageSize, $scope.timeSetCallback);
    };

    $scope.nextMonth = function() {
        month1 = month1 + 1;
        if ( month1 > 12 ) {
            month1 = 1;
            year1 = year1 + 1;
        }
        $("#timeVisualisation").html("");
        globalSvc.setObject("last-month-time", { 'year': year1, 'month': month1 });
        timeSvc.getTimeSet(session, year1, month1, day1, hour1, 0,
            year1, month1, day1, hour1, 0, page, pageSize, $scope.timeSetCallback);
    };

    $scope.openUrls = function(urlList) {
        if ( urlList && urlList.length > 0 ) {
            $scope.details(urlList[0]);
        }
    };

    // work out the maximum value of x required to display them all
    // on the y-axis
    $scope.getMaxX = function() {
        var dayCounter = {};
        $.each($scope.timeUrlList, function(i, timeUrl) {
            // assing a unique colour to each url
            var day = timeUrl.day;
            if (dayCounter[day] != undefined) {
                var value = dayCounter[day];
                dayCounter[day] = value + 1;
            } else {
                dayCounter[day] = 0;
            }
        });
        var maxx = 0;
        $.each(dayCounter, function(day, count) {
            if ( count > maxx ) {
                maxx = count;
            }
        });
        return maxx;
    };

    // call back from the service -setup time view
    $scope.timeSetCallback = function( timeSet ) {
        if (timeSet && timeSet.timeUrlList ) {
            $scope.timeUrlList = timeSet.timeUrlList;
            $scope.initChart();
        } else {
            $scope.timeUrlList = [];
            $scope.initChart();
        }
    };

    ///////////////////////////////////////////////////////////////
    // document details / popup

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

            var dtObject = globalSvc.getObject("last-month-time");
            if ( dtObject ) {
                year1 = dtObject.year;
                month1 = dtObject.month;
            }

            timeSvc.getTimeSet(session, year1, month1, day1, hour1, 0,
                year1, month1, day1, hour1, 0, page, pageSize, $scope.timeSetCallback);
        }
    }, $scope.userCallback);

    $scope.changePicker = function(date, old_date) {
        alert('change!');
    };

    $scope.getIndex = function(arr, str) {
        for ( var i = 0; i < arr.length; i++ ) {
            if ( arr[i] == str ) {
                return i;
            }
        }
        return -1;
    };

    $('#datetimepicker1').datetimepicker({viewMode:'years', format: 'MMM YYYY'});
    
    // change event - use calendar picker to change dates
    $('#datetimepicker1').on("dp.change", function() {
        var value = $("#date1").val();
        var parts = value.split(" ");
        if ( parts.length == 2 ) {
            var month = $scope.getIndex(monthOfYear, parts[0]);
            if ( month >= 0 ) {
                month = month + 1;
                var year = parseInt(parts[1]);
                if ( month != month1 || year != year1 ) {
                    $("#timeVisualisation").html("");
                    month1 = month;
                    year1 = year;
                    timeSvc.getTimeSet(session, year1, month1, day1, hour1, 0,
                        year1, month1, day1, hour1, 0, page, pageSize, $scope.timeSetCallback);
                }
            }
        }
    });


});
