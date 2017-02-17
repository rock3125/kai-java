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
    .controller('StatisticsIndexController', function ($scope, $cookies, $location, globalSvc, documentSvc, statsSvc) {

        var session = null;
        $scope.stats = null;

        $scope.index_count = 16;

        $scope.barWidth = 30;
        $scope.barDistance = 31;


        // helper function - draw circle with all its details
        $scope.drawBar = function( vis, id, x, y, height, value, fillColour, borderColour, short_label, label ) {
            vis.append("rect")
                .attr("id", id)
                .attr("x", x)
                .attr("y", y - height)
                .attr("width", $scope.barWidth)
                .attr("height", height)
                .style("fill", fillColour)
                .style("stroke", borderColour)
                .append("svg:title")
                .text($scope.label(label, value));

            vis.append("text")
                .attr("transform", function(d) {
                    return "translate(" + x + "," + (y+15) + ") rotate(55)"
                })
                .text(short_label);

            vis.append("text")
                .attr("transform", function(d) {
                    return "translate(" + (x-16) + "," + (y+22) + ") rotate(55)"
                })
                .text(("" + value).integerPrettyPrint());

        };

        $scope.getMax = function() {
            var maxValue = 0;
            $.each( $scope.stats, function(i, obj) {
                if ( obj.value > maxValue ) {
                    maxValue = obj.value;
                }
            });
            return maxValue;
        };

        // setup a chart
        $scope.initChart = function() {

            var vis = d3.select("#statisticsVisualisation");

            var width = 1000;
            var height = 600;
            var margins = {
                top: 110,
                right: 20,
                bottom: 20,
                left: 100
            };

            var minx = 0;
            var maxx = 500;
            var miny = 0;
            var v = $scope.getMax();
            var maxy = v + parseInt(v / 10);

            var xScale = d3.scale.linear().range([margins.left, width - margins.right]).domain([minx, maxx]);
            var yScale = d3.scale.linear().range([height - margins.top, margins.bottom]).domain([miny, maxy]);

            var xAxis = d3.svg.axis()
                .scale(xScale)
                .tickFormat("");

            var yAxis = d3.svg.axis()
                .scale(yScale)
                .orient("left");

            vis.append("svg:g")
                .attr("class", "x axis")
                .attr("transform", "translate(0," + (height - (margins.top)) + ")")
                .call(xAxis);

            vis.append("svg:g")
                .attr("class", "y axis")
                .attr("transform", "translate(" + (margins.left) + ",0)")
                .call(yAxis);

            var x = 10;

            $.each($scope.stats, function(i, obj) {

                $scope.drawBar(vis, obj.name, xScale(x), yScale(0), yScale(maxy - obj.value), obj.value,
                    "gray", "black", obj.name, "indexes for \"" + obj.name + "\"");

                x = x + $scope.barDistance;

            });

        };

        $scope.label = function( str, count ) {
            var num = ("" + count).integerPrettyPrint();
            return str + ": " + num;
        };

        $scope.refresh = function() {
            $("#statisticsVisualisation").html("");
            statsSvc.getIndexStatistics(session, $scope.index_count, $scope.indexStatisticsCallback);
        };

        $scope.indexStatisticsCallback = function (data) {
            if ( data && data.nameValueList ) {
                $scope.stats = data.nameValueList;
                $scope.initChart();
            } else {
                $scope.stats = null;
                $("#statisticsVisualisation").html("");
            }
        };

        ///////////////////////////////////////////////////////////////

        // signed in?
        globalSvc.getSession( function(pSession) {

            if ( !pSession ) {

                globalSvc.goHome();

            } else {
                session = pSession;
                statsSvc.getIndexStatistics(session, $scope.index_count, $scope.indexStatisticsCallback);
            }

        });


    });

