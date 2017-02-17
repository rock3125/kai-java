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
    .controller('StatisticsManagerController', function ($scope, $cookies, $location, globalSvc, documentSvc, statsSvc) {

        var session = null;
        $scope.stats = null;

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
            $.each( $scope.stats, function(name, value) {
                if ( name != "total_count" && name != "total_valid_count" && name != "total_index_count" &&
                     name != "sentence_count" && name != "total_content_bytes") {
                    if ( value > maxValue ) {
                        maxValue = value;
                    }
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

            $scope.drawBar(vis, "document_count", xScale(x), yScale(0), yScale(maxy - $scope.stats.document_count), $scope.stats.document_count,
                "red", "black", "document count", "total number of documents");

            x = x + $scope.barDistance;
            $scope.drawBar(vis, "noun", xScale(x), yScale(0), yScale(maxy - $scope.stats.noun), $scope.stats.noun,
                "gray", "black", "nouns", "total of all nouns");

            x = x + $scope.barDistance;
            $scope.drawBar(vis, "proper_noun", xScale(x), yScale(0), yScale(maxy - $scope.stats.proper_noun), $scope.stats.proper_noun,
                "gray", "black", "proper nouns", "total of all proper-nouns");

            x = x + $scope.barDistance;
            $scope.drawBar(vis, "verb", xScale(x), yScale(0), yScale(maxy - $scope.stats.verb), $scope.stats.verb,
                "gray", "black", "verbs", "total of all verbs");

            x = x + $scope.barDistance;
            $scope.drawBar(vis, "adjective", xScale(x), yScale(0), yScale(maxy - $scope.stats.adjective), $scope.stats.adjective,
                "gray", "black", "adjectives", "total of all adjectives");

            x = x + $scope.barDistance;
            $scope.drawBar(vis, "adverb", xScale(x), yScale(0), yScale(maxy - $scope.stats.adverb), $scope.stats.adverb,
                "gray", "black", "adverbs", "total of all adverbs");

            x = x + $scope.barDistance;
            $scope.drawBar(vis, "percent", xScale(x), yScale(0), yScale(maxy - $scope.stats.percent), $scope.stats.percent,
                "gray", "black", "percentage", "total of all percentages");

            x = x + $scope.barDistance;
            $scope.drawBar(vis, "url", xScale(x), yScale(0), yScale(maxy - $scope.stats.url), $scope.stats.url,
                "gray", "black", "url", "total of all urls");

            x = x + $scope.barDistance;
            $scope.drawBar(vis, "email", xScale(x), yScale(0), yScale(maxy - $scope.stats.email), $scope.stats.email,
                "gray", "black", "email", "total of all emails");

            x = x + $scope.barDistance;
            $scope.drawBar(vis, "date", xScale(x), yScale(0), yScale(maxy - $scope.stats.date), $scope.stats.date,
                "gray", "black", "date", "total of all dates");

            x = x + $scope.barDistance;
            $scope.drawBar(vis, "time", xScale(x), yScale(0), yScale(maxy - $scope.stats.time), $scope.stats.time,
                "gray", "black", "time", "total of all times");

            x = x + $scope.barDistance;
            var total = $scope.stats.number + $scope.stats.decimal;
            $scope.drawBar(vis, "number", xScale(x), yScale(0), yScale(maxy - total), total,
                "gray", "black", "numbers", "total of all numbers");

            x = x + $scope.barDistance;
            $scope.drawBar(vis, "money", xScale(x), yScale(0), yScale(maxy - $scope.stats.money), $scope.stats.money,
                "gray", "black", "money", "total of all monetary amounts");

            x = x + $scope.barDistance;
            $scope.drawBar(vis, "phone", xScale(x), yScale(0), yScale(maxy - $scope.stats.phone), $scope.stats.phone,
                "gray", "black", "phone numbers", "total of all phone numbers");

            vis.append("text")
                .attr("x", 1000)
                .attr("y", 50)
                .style("text-anchor","end")
                .text($scope.label("total number of tokens", $scope.stats.total_count));

            vis.append("text")
                .attr("x", 1000)
                .attr("y", 70)
                .style("text-anchor","end")
                .text($scope.label("total number valid of tokens", $scope.stats.total_valid_count));

            vis.append("text")
                .attr("x", 1000)
                .attr("y", 90)
                .style("text-anchor","end")
                .text($scope.label("total number of sentences", $scope.stats.sentence_count));

            vis.append("text")
                .attr("x", 1000)
                .attr("y", 110)
                .style("text-anchor","end")
                .text($scope.label("total number of indexes", $scope.stats.total_index_count));

            // vis.append("text")
            //     .attr("x", 1000)
            //     .attr("y", 130)
            //     .style("text-anchor","end")
            //     .text($scope.label("number of content bytes", $scope.stats.total_content_bytes));

        };


        $scope.label = function( str, count ) {
            var num = ("" + count).integerPrettyPrint();
            return str + ": " + num;
        };

        $scope.refresh = function() {
            $("#statisticsVisualisation").html("");
            statsSvc.getGeneralStatistics(session, $scope.generalStatisticsCallback);
        };

        $scope.generalStatisticsCallback = function (data) {
            if ( data ) {
                $scope.stats = data;
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
                statsSvc.getGeneralStatistics(session, $scope.generalStatisticsCallback);
            }

        });


    });

