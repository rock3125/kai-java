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
 * @name webApp.controller:SearchAnomalyController
 * @description
 * # SearchAnomalyController
 * Controller of the webApp
 */
angular.module('webApp')
    .controller('SearchAnomalyController', function ($scope, $http, $cookies, globalSvc, anomalySvc, documentSvc) {

        var session = null;
        $scope.url_list = [];
        var colours = [ 'green', 'red', 'blue', 'black', 'orange'];
        $scope.graphActive = {};

        // graph dimensions that can be adjusted
        var maxx = 1;
        var miny = -1.0;
        var maxy = 1.0;

        // the amount of distance between a data point and a click for it to register
        var minClickDistance = 1.0;

        // triangle left bottom on axis with spike going up
        var trianglePoints = '-4 4 0 -4 0 -600 0 -4 4 4 -4 4';
        var tx = 50;
        var ty = 504;
        var xAxisWidth = 950;
        var selectionCounter = 0;
        var axisX1 = 0;
        var axisX2 = 0;

        // setup a chart
        $scope.initChart = function( data ) {

            $scope.calculateDimensions(data);

            var vis = d3.select("#visualisation");

            var width = 1000;
            var height = 600;
            var margins = {
                top: 110,
                right: 20,
                bottom: 20,
                left: 50
            };

            var xScale = d3.scale.linear().range([margins.left, width - margins.right]).domain([0, maxx]);
            var yScale = d3.scale.linear().range([height - margins.top, margins.bottom]).domain([miny, maxy]);

            var xAxis = d3.svg.axis()
                .scale(xScale);

            var yAxis = d3.svg.axis()
                    .scale(yScale)
                    .orient("left");

            var selectionCircleRadius = 5;
            vis.append("circle")
                .attr("id", "selection-circle")
                .style("stroke", "#000000")
                .style("fill", 'red')
                .attr("cx", -100)
                .attr("cy", -100)
                .attr("r", selectionCircleRadius);



            // two triangles for bottom selection
            vis.append('polyline')
                .attr("id", "triangle1")
                .attr('points', trianglePoints)
                .style('stroke', 'black');

            vis.append('polyline')
                .attr("id", "triangle2")
                .attr('points', trianglePoints)
                .style('stroke', 'black');

            // move t1 and t2 off-screen
            var t1 = vis.selectAll("#triangle1");
            t1.attr("transform", "translate(-100, -100)");

            var t2 = vis.selectAll("#triangle2");
            t2.attr("transform", "translate(-100, -100)");






            vis.append("svg:g")
                .attr("class", "x axis")
                .attr("transform", "translate(0," + (height - (margins.bottom + 80)) + ")")
                .call(xAxis);

            vis.append("svg:g")
                .attr("class", "y axis")
                .attr("transform", "translate(" + (margins.left) + ",0)")
                .call(yAxis);

            // 2 sd line
            vis.append("line")
                .style("stroke", "#cccccc")
                .attr("x1", 52)
                .attr("y1", 100)
                .attr("x2", 1000)
                .attr("y2", 100);

            // 1 sd line
            vis.append("line")
                .style("stroke", "#cccccc")
                .attr("x1", 52)
                .attr("y1", 180)
                .attr("x2", 1000)
                .attr("y2", 180);

            // origin line
            vis.append("line")
                .style("stroke", "#bbbbbb")
                .attr("x1", 52)
                .attr("y1", 256)
                .attr("x2", 1000)
                .attr("y2", 256);


            // -1 sd line
            vis.append("line")
                .style("stroke", "#cccccc")
                .attr("x1", 52)
                .attr("y1", 334)
                .attr("x2", 1000)
                .attr("y2", 334);

            // -2 sd line
            vis.append("line")
                .style("stroke", "#cccccc")
                .attr("x1", 52)
                .attr("y1", 412)
                .attr("x2", 1000)
                .attr("y2", 412);


            // drawing algorithm implementation, how to map data to x,y
            var lineGen = d3.svg.line()
                .x(function(d) {
                    return xScale(d.sentence_id);
                })
                .y(function(d) {
                    return yScale(d.value);
                })
                .interpolate("cardinal");


            // select a sentence from the graph
            $('#visualisation').on('click', function(e) {

                if ( e ) {

                    var graphX = xScale.invert(e.offsetX);
                    var graphY = yScale.invert(e.offsetY);

                    // pick from the graphs the items closest to this point
                    var index = parseInt(graphX);
                    var smallestDiff = maxy * 2.0;
                    var bestGraph = null;
                    var bestGraphIndex = -1;
                    $.each( data, function(j, emotionalItem) {
                        if ($scope.graphActive[emotionalItem.url]) {
                            if (index >= 0 && index < emotionalItem.emotional_list.length ) {
                                var yValue = emotionalItem.emotional_list[index].value;
                                var diff = Math.abs(yValue - graphY);
                                if ( diff <= minClickDistance && diff < smallestDiff  ) {
                                    smallestDiff = diff;
                                    bestGraph = emotionalItem;
                                    bestGraphIndex = j;
                                }
                            }
                        }
                    });

                    var circles = vis.selectAll("#selection-circle");
                    if ( bestGraph != null ) {

                        var sentenceStr = $scope.getSentence( bestGraph.url, index, $scope.getSentenceCallback );
                        if ( circles ) {
                            circles.attr("cx", xScale(index) - selectionCircleRadius)
                                   .attr("cy", yScale(bestGraph.emotional_list[index].value) - selectionCircleRadius)
                                   .style("fill", colours[bestGraphIndex % colours.length])
                        }

                    } else {

                        // is this an axis selection event
                        var selectAxis = (e.offsetX >= tx && e.offsetX <= (tx + xAxisWidth)) &&
                                         (e.offsetY >= (ty - 20) && e.offsetY <= (ty + 20));

                        if ( selectAxis ) {

                            selectionCounter = selectionCounter + 1;

                            if ( selectionCounter == 1 ) {
                                t1.attr("transform", "translate(" + e.offsetX + "," + ty + ")");
                                axisX1 = parseInt(xScale.invert(e.offsetX));
                            } else if ( selectionCounter == 2 ) {
                                t2.attr("transform", "translate(" + e.offsetX + "," + ty + ")");
                                axisX2 = parseInt(xScale.invert(e.offsetX));

                                if ( axisX2 > axisX1 ) {

                                    // redraw graphs
                                    xScale = d3.scale.linear().range([margins.left, width - margins.right]).domain([axisX1, axisX2]);
                                    var xAxis = d3.svg.axis().scale(xScale);
                                    vis.selectAll("g.x.axis").call(xAxis);

                                    var svg = vis.transition();
                                    $.each(data, function (k, emotionalItem2) {
                                        svg.select("#item" + k)   // change the line
                                            .attr("d", lineGen(emotionalItem2.emotional_list));
                                    });
                                }

                                // unselect lines etc.
                                selectionCounter = 0;
                                t1.attr("transform", "translate(-100, -100)");
                                t2.attr("transform", "translate(-100, -100)");

                                // disable the sentence selection system
                                $scope.getSentenceCallback(null, null, null);
                                if (circles) {
                                    circles
                                        .attr("cx", -100)
                                        .attr("cy", -100);
                                }



                            }

                        } else {

                            // disable the sentence selection system
                            $scope.getSentenceCallback(null, null, null);
                            if (circles) {
                                circles
                                    .attr("cx", -100)
                                    .attr("cy", -100);
                            }

                            selectionCounter = 0;
                            t1.attr("transform", "translate(-100, -100)");
                            t2.attr("transform", "translate(-100, -100)");


                            // reset
                            xScale = d3.scale.linear().range([margins.left, width - margins.right]).domain([0, maxx]);
                            xAxis = d3.svg.axis().scale(xScale);
                            vis.selectAll("g.x.axis").call(xAxis);

                            svg = vis.transition();
                            $.each( data, function(k, emotionalItem2) {
                                svg.select("#item" + k)   // change the line
                                    .attr("d", lineGen(emotionalItem2.emotional_list));
                            });



                        }


                    }
                }
            });



            var lineHeight = 22;
            $.each( data, function(j, emotionalItem) {

                var row = parseInt(j / 3);
                var col = j % 3;

                var x = parseInt(20 + col * (width / 3.2));
                var y = (height - 50) + row * lineHeight;

                vis.append("circle")
                    .attr("id", "circle_" + j)
                    .attr("cx", x )
                    .attr("cy", y )
                    .attr("class", "y")
                    .style("fill", colours[j % colours.length])
                    .style("stroke", "black")
                    .attr("r", 8)
                    .on("click", function() {

                        var thisCircle = vis.selectAll("#circle_" + j);

                        if ( $scope.graphActive[emotionalItem.url] ) {
                            $scope.graphActive[emotionalItem.url] = false;
                            d3.select("#item" + j).style("opacity", 0);
                            if ( thisCircle ) {
                                thisCircle.style("fill", "white"); // white inside circle
                            }
                        } else {
                            $scope.graphActive[emotionalItem.url] = true;
                            d3.select("#item" + j).style("opacity", 1);
                            if ( thisCircle ) {
                                thisCircle.style("fill", colours[j % colours.length]); // set back its colour
                            }
                        }
                        $scope.calculateDimensions(data);

                        xScale = d3.scale.linear().range([margins.left, width - margins.right]).domain([0, maxx]);
                        var xAxis = d3.svg.axis().scale(xScale);
                        vis.selectAll("g.x.axis").call(xAxis);

                        var svg = vis.transition();
                        $.each( data, function(k, emotionalItem2) {
                            svg.select("#item" + k)   // change the line
                                .attr("d", lineGen(emotionalItem2.emotional_list));
                        });

                    });

                // display the url text
                var emoUrl = emotionalItem.url;
                if ( emoUrl.length > 44 ) {
                    emoUrl = emoUrl.substring(0, 44);
                }
                vis.append("text")
                    .attr("x", x + 20)
                    .attr("y", y + 2)
                    .attr("text-anchor", "left")
                    .style("font-size", "14px")
                    .style("text-decoration", "underline")
                    .text(emoUrl)
                    .on("click", function() {
                        $scope.view(emotionalItem.url);
                    });
            });

            $.each( data, function(j, emotionalItem) {

                vis.append('svg:path')
                    .attr('d', lineGen(emotionalItem.emotional_list))
                    .attr('stroke', colours[j % colours.length])
                    .attr('stroke-width', 1)
                    .attr('id', 'item' + j)
                    .attr('fill', 'none');
            });




        };



        // return the sentence @ index for a given url
        $scope.getSentence = function( url, sentenceIndex, sentenceGetCallback ) {
            if ( sentenceIndex >= 0 && url ) {
                documentSvc.getSentence(session, url, sentenceIndex, sentenceGetCallback);
            }
        };

        $scope.getSentenceCallback = function( url, sentenceIndex, text ) {
            if ( url == null ) {
                console.log("off click");
                $("#selectedText").html("&nbsp;");
            } else {
                console.log("selected sentence " + sentenceIndex + " from graph " + url + " = " + text);
                $("#selectedText").text(text);
            }
        };


        // work out the minx / maxx etc for active items
        $scope.calculateDimensions = function(data) {
            maxx = 1;
            miny = -1.0;
            maxy = 1.0;
            $.each( data, function(j, emotionalItem) {
                if ( $scope.graphActive[emotionalItem.url] ) {
                    if (emotionalItem.emotional_list.length > maxx) {
                        maxx = emotionalItem.emotional_list.length;
                    }
                    $.each(emotionalItem.emotional_list, function (i, item) {
                        if (item.value < miny) {
                            miny = item.value;
                        }
                        if (item.value > maxy) {
                            maxy = item.value;
                        }
                    });
                }
            });
        };


        $scope.view = function(url) {
            if ( url ) {
                console.log("view " + url);
                documentSvc.viewDocument(session, url);
            }
        };

        $scope.canvasClick = function (evt) {
            console.log("clicked " + evt.x + "," + evt.y);
        };

        // setup the emotional list
        $scope.anomalySetCallback = function (data) {
            if (data && data.emotionalSetList ) {

                // setup all items as active initially
                $scope.graphActive = {};
                $.each( data.emotionalSetList, function (i, item) {
                    $scope.graphActive[item.url] = true;
                });

                $scope.initChart(data.emotionalSetList);
            }
        };

        // signed in?
        globalSvc.getSession( function(pSession) {

            if ( !pSession ) {

                globalSvc.goHome();

            } else {

                session = pSession;

                // set the url list from the other page
                $scope.url_list = globalSvc.getObject("url_list");

                // get an emotional analysis list
                if ( $scope.url_list.length > 0 ) {
                    anomalySvc.getAnomalySet(session, $scope.url_list, $scope.anomalySetCallback);
                }

            }
        });



    });