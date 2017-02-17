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

angular.module('searchApp')
.controller('DocumentViewer', function($scope, $timeout, documentViewerSvc, documentSvc) {

    $scope.metadata = {};
    $scope.summarisation_item = [];
    $scope.emotional_graph = [];
    $scope.stats = null;
    $scope.knowledge = [];

    $scope.getImageUrl = function(item) {
        if ( documentViewerSvc.session != null && documentViewerSvc.url != null ) {
            return documentSvc.getDocumentImageUrl(documentViewerSvc.session, documentViewerSvc.url);
        }
        return "";
    };

    $scope.showModal = function() {

        $("#visualisation").html(""); // clear out previous graphs
        $("#statisticsVisualisation").html("");

        // filter the scope metadata to certain sizes only
        $scope.metadata = {};
        $.each(documentViewerSvc.metadata, function(name, value) {
            if ( value && value.length > 200 ) {
                $scope.metadata[name] = value.substr(0, 200) + " ...";
            } else {
                $scope.metadata[name] = value;
            }
        });

        $scope.summarisation_item = documentViewerSvc.summarisation_item;
        $scope.emotional_graph = documentViewerSvc.emotional_graph;
        $scope.stats = documentViewerSvc.stats;
        $scope.knowledge = documentViewerSvc.knowledge;

        $("#DocumentDetailDialogTitle").text(documentViewerSvc.title);
        $("#DocumentDetailDialog").modal('show');

        $scope.initEmotionalChart();
        $scope.initStatsChart();
    };

    $scope.closeModal = function() {
        $("#DocumentDetailDialog").modal('hide');
    };

    // request to download the document
    $scope.download = function() {
        documentSvc.viewDocument(documentViewerSvc.session, documentViewerSvc.url);
    };

    $scope.viewDetails = function(url) {
        documentViewerSvc.show(documentViewerSvc.session, url);
    };

    /////////////////////////////////////////////////////////////////////////
    // emotions

    // work out the minx / maxx etc for active items
    $scope.calculateEmotionalDimensions = function(data) {
        var maxx = 1;
        var miny = -1.0;
        var maxy = 1.0;
        $.each( data, function(j, emotionalItem) {
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
        });
        return { 'maxx': maxx, 'miny': miny, 'maxy': maxy };
    };

    // setup the emotional graph
    $scope.initEmotionalChart = function() {

        var data = $scope.emotional_graph;

        var dimensions = $scope.calculateEmotionalDimensions(data);
        var maxx = dimensions.maxx;
        var maxy = dimensions.maxy;
        var miny = dimensions.miny;

        var vis = d3.select("#visualisation");

        var width = 800; // $("#navTabs").width();
        var height = $("#visualisation").height();
        var margins = {
            top: 20,
            right: 20,
            bottom: 20,
            left: 50
        };

        var xScale = d3.scale.linear().range([margins.left, width - margins.right]).domain([0, maxx]);
        var yScale = d3.scale.linear().range([height - margins.top, margins.bottom]).domain([miny, maxy]);

        var xAxis = d3.svg.axis().scale(xScale);

        var yAxis = d3.svg.axis().scale(yScale).orient("left");

        vis.append("svg:g")
            .attr("class", "x axis")
            .attr("transform", "translate(0," + ((height / 2) - 2) + ")")
            .call(xAxis);

        vis.append("svg:g")
            .attr("class", "y axis")
            .attr("transform", "translate(" + (margins.left) + ",0)")
            .call(yAxis);

        // drawing algorithm implementation, how to map data to x,y
        var lineGen = d3.svg.line()
            .x(function(d) {
                return xScale(d.sentence_id);
            })
            .y(function(d) {
                return yScale(d.value);
            })
            .interpolate("cardinal");

        $.each( data, function(j, emotionalItem) {
            vis.append('svg:path')
                .attr('d', lineGen(emotionalItem.emotional_list))
                .attr('stroke', "red")
                .attr('stroke-width', 1)
                .attr('id', 'item' + j)
                .attr('fill', 'none');
        });

        $.each( data, function(j, emotionalItem) {
            // don't draw too many points ~ 100 should do
            var stepSize = parseInt( emotionalItem.emotional_list.length / 100);
            if ( stepSize == 0 ) {
                stepSize = 1;
            }
            for ( var i = 0; i < emotionalItem.emotional_list.length; i = i + stepSize ) {
                var item = emotionalItem.emotional_list[i];
                var sentence_id = item.sentence_id;
                var formattedValue = parseInt(item.value * 100.0) / 100;
                vis.append('circle')
                    .attr('cx', xScale(sentence_id))
                    .attr('cy', yScale(item.value))
                    .attr('stroke', "black")
                    .attr('r', 3)
                    .attr('fill', 'black')
                    .append("svg:title")
                    .text("sentence " + sentence_id + ", " + formattedValue);
            }
        });

        // select a sentence from the graph
        $('#visualisation').on('click', function(e) {
            if (e) {
                var graphX = xScale.invert(e.offsetX);
                var graphY = yScale.invert(e.offsetY);
                var showing = false; // are we showing an emotion?
                $.each(data, function (j, emotionalItem) {
                    var stepSize = parseInt( emotionalItem.emotional_list.length / 100);
                    if ( stepSize == 0 ) {
                        stepSize = 1;
                    }
                    for ( var i = 0; i < emotionalItem.emotional_list.length; i = i + stepSize ) {
                        var item = emotionalItem.emotional_list[i];
                        var cx = item.sentence_id;
                        var cy = item.value;
                        if ( Math.abs(cx - graphX) < 0.5 && Math.abs(cy - graphY) < 0.1 ) {
                            showing = true;
                            $scope.showEmotion(item.sentence_id);
                        }
                    }
                });
                if ( !showing ) {
                    $scope.getSentenceCallback(null); // wipe the contents
                }
            }
        });

    };

    $scope.showEmotion = function( sentence_id ) {
        if ( sentence_id >= 0 ) {
            console.log(sentence_id);
            documentSvc.getSentence(documentViewerSvc.session, documentViewerSvc.url, sentence_id, $scope.getSentenceCallback);
        }
    };

    $scope.getSentenceCallback = function (url, sentence_id, text) {
        if ( url ) {
            $("#sentenceText").text("" + sentence_id + ": " + text);
        } else {
            $("#sentenceText").text("");
        }
    };

    /////////////////////////////////////////////////////////////////////////
    // stats

    $scope.barWidth = 30;
    $scope.barDistance = 31;

    // helper function - draw circle with all its details
    $scope.drawBar = function( vis, id, x, y, height, value, fillColour, borderColour, short_statsLabel, statsLabel ) {
        vis.append("rect")
            .attr("id", id)
            .attr("x", x)
            .attr("y", y - height)
            .attr("width", $scope.barWidth)
            .attr("height", height)
            .style("fill", fillColour)
            .style("stroke", borderColour)
            .append("svg:title")
            .text($scope.statsLabel(statsLabel, value));

        vis.append("text")
            .attr("transform", function(d) {
                return "translate(" + x + "," + (y+15) + ") rotate(55)"
            })
            .text(short_statsLabel);

        vis.append("text")
            .attr("transform", function(d) {
                return "translate(" + (x-16) + "," + (y+22) + ") rotate(55)"
            })
            .text(("" + value).integerPrettyPrint());

    };

    $scope.getStatsMax = function() {
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

    $scope.statsLabel = function( str, count ) {
        var num = ("" + count).integerPrettyPrint();
        return str + ": " + num;
    };

    // setup a chart
    $scope.initStatsChart = function() {

        if ( !$scope.stats ) {
            return;
        }

        var vis = d3.select("#statisticsVisualisation");

        var width = 800; // $("#navTabs").width();
        var height = $("#statisticsVisualisation").height();
        var margins = {
            top: 110,
            right: 20,
            bottom: 20,
            left: 50
        };

        var minx = 0;
        var maxx = 500;
        var miny = 0;
        var v = $scope.getStatsMax();
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
            .attr("x", 900)
            .attr("y", 50)
            .style("text-anchor","end")
            .text($scope.statsLabel("total number of sentences", $scope.stats.sentence_count));

        vis.append("text")
            .attr("x", 900)
            .attr("y", 70)
            .style("text-anchor","end")
            .text($scope.statsLabel("total number of tokens", $scope.stats.total_count));

        vis.append("text")
            .attr("x", 900)
            .attr("y", 90)
            .style("text-anchor","end")
            .text($scope.statsLabel("total number valid of tokens", $scope.stats.total_valid_count));

    };


    /////////////////////////////////////////////////////////////////////////

    // setup callbacks
    documentViewerSvc.showModal = $scope.showModal;
    documentViewerSvc.closeModal = $scope.closeModal;

})
.directive('documentViewer', function() {
    return {
        templateUrl: 'views/widgets/document_viewer.html'
    };
});

