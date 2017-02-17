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
 * @name webApp.controller:ClusterDetailsManagerController
 * @description
 * # ClusterDetailsManagerController
 * Controller of the webApp
 */
angular.module('webApp')
    .controller('ClusterDetailsManagerController', function ($scope, $cookies, $location, globalSvc, clusterSvc, documentViewerSvc) {

        var session = null;
        $scope.cluster_id = 0;
        $scope.cluster_contents = [];
        $scope.cluster_description = [];
        $scope.hasCoordinates = false;


        $scope.clusterDoneCallback = function(data) {
            if ( data && data.clusterContents ) {
                $scope.cluster_contents = data.clusterContents;
                $scope.cluster_description = data.clusterDescription;

                // determine if the cluster has (x,y) coordinates
                $scope.hasCoordinates = false;
                $.each($scope.cluster_contents, function(i, clusterItem) {
                    if ( clusterItem.x != 0.0 || clusterItem.y != 0.0 ) {
                        $scope.hasCoordinates = true;
                    }
                });

                $scope.initChart();

            } else {
                $scope.cluster_contents = [];
                $scope.cluster_description = [];
            }
        };

        $scope.backToClusters = function () {
            $location.path("/cluster");
        };

        ///////////////////////////////////////////////////////////////
        // document details / popup

        // get the details for a url item
        $scope.details = function(url) {
            if ( url && session ) {
                documentViewerSvc.show(session, url);
            }
        };

        ///////////////////////////////////////////////////////////////

        // signed in?
        globalSvc.getSession( function(pSession) {
            if ( !pSession ) {
                globalSvc.goHome();
            } else {
                session = pSession;
                var cluster = globalSvc.getObject("cluster_id");
                if ( cluster && cluster.cluster_id ) {
                    $scope.cluster_id = cluster.cluster_id;
                    clusterSvc.getClusterByID(session, $scope.cluster_id, $scope.clusterDoneCallback);
                }
            }
        });

        // helper function - draw circle with all its details
        $scope.drawCircle = function( vis, id, cx, cy, radius, fillColour, borderColour, label, callbackData ) {
            // draw the centroid at (0,0)
            if ( callbackData ) {
                vis.append("circle")
                    .attr("id", id)
                    .attr("cx", cx)
                    .attr("cy", cy)
                    .attr("class", "y")
                    .style("fill", fillColour)
                    .style("stroke", borderColour)
                    .attr("r", radius)
                    .on("click", function() { $scope.details(callbackData) } )
                    .append("svg:title")
                    .text(label);
            } else {
                vis.append("circle")
                    .attr("id", id)
                    .attr("cx", cx)
                    .attr("cy", cy)
                    .attr("class", "y")
                    .style("fill", fillColour)
                    .style("stroke", borderColour)
                    .attr("r", radius)
                    .append("svg:title")
                    .text(label);
            }
        };



        // setup a chart
        $scope.initChart = function() {

            var vis = d3.select("#clusterVisualisation");

            var width = 1000;
            var height = 600;
            var margins = {
                top: 110,
                right: 20,
                bottom: 20,
                left: 50
            };

            var minx = -500;
            var maxx = 500;
            var miny = -300;
            var maxy = 300;

            var xScale = d3.scale.linear().range([margins.left, width - margins.right]).domain([minx, maxx]);
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

            // add centroid
            $scope.drawCircle(vis, "centroid", xScale(0.0), yScale(0.0), 4, "yellow", "black", "cluster " + $scope.cluster_id + " centroid", null);

            // get biggest and smallest distances in the cluster
            var minDistance = -1.0;
            var maxDistance = -1.0;
            $.each($scope.cluster_contents, function(i, clusterItem) {
                var distance = clusterItem.distance;
                if ( minDistance == -1.0 ) {
                    minDistance = distance;
                    maxDistance = distance;
                } else {
                    if ( distance < minDistance ) {
                        minDistance = distance;
                    }
                    if ( distance > maxDistance ) {
                        maxDistance = distance;
                    }
                }
            });

            var length = maxDistance;
            if ( length == 0 ) {
                length = 1;
            }

            // draw the graph
            if ( $scope.hasCoordinates ) {
                // use x,y coordinates to draw this system
                var hw = (width - 100.0) / 2;
                var hh = (height - 100.0) / 2;
                $.each($scope.cluster_contents, function (i, clusterItem) {
                    var cx = clusterItem.x * hw;
                    var cy =  clusterItem.y * hh;
                    var url = clusterItem.url;
                    var id = "cluster_item_" + i;
                    $scope.drawCircle(vis, id, xScale(cx), yScale(cy), 4, "red", "black", url, url);
                });
            } else {
                // draw it the old fashioned way with circle offsets
                var angle = 0.0;
                var deg2rad = Math.PI / 180.0;
                $.each($scope.cluster_contents, function (i, clusterItem) {
                    var distance = clusterItem.distance;
                    var cx = (distance / length) * maxx;
                    var cy = (distance / length) * maxy;
                    var url = clusterItem.url;
                    var id = "cluster_item_" + i;

                    // add cos / sin
                    cx = cx * Math.cos(deg2rad * angle);
                    cy = cy * Math.sin(deg2rad * angle);
                    $scope.drawCircle(vis, id, xScale(cx), yScale(cy), 4, "red", "black", url, url);

                    angle = angle + 5.0;
                    if (angle >= 360.0) {
                        angle = angle - 360.0;
                    }
                });
            }


            var top_y = 50;
            var line_height = 15;
            var person_x = 1010;
            var person_y = top_y;

            // draw the keywords for this cluster to emphasize its content
            $.each($scope.cluster_description, function(i, text) {
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
                person_y = person_y + line_height;
            });


        }; // end of init-chart






    });

