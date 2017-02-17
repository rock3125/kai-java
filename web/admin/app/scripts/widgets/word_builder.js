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

angular.module('webApp')
.controller('WordBuilder', function($scope, globalSvc, wordBuilderSvc) {

    $scope.wordList = [];
    $scope.currentType = 'Type';
    $scope.metadata = '';
    $scope.word = '';
    $scope.exact = false;
    $scope.idCounter = 1;

    $scope.selectType = function(typeStr) {
        console.log("type set to " + typeStr);
        if ( typeStr ) {
            $scope.currentType = typeStr;
        }
    };

    $scope.remove = function(list, wordStr) {
        var newList = [];
        $.each(list, function(i, item) {
            if ( item && item.word && item.word != wordStr ) {
                newList.push(item);
            }
        });
        return newList;
    };

    $scope.contains = function(list, wordStr) {
        var found = false;
        $.each(list, function(i, item) {
            if ( item && item.word && item.word == wordStr ) {
                found = true;
            }
        });
        return found;
    };

    // convert an item to a string
    $scope.itemToString = function(item, i, size) {
        if ( item && item.word ) {
            var str = item.word;
            if ( item.type && item.type != 'any' ) {
                str = str + "," + item.type;
            } else {
                str = str + ",";
            }
            if ( item.exact ) {
                str = str + ",exact";
            } else {
                str = str + ",";
            }
            if ( i + 1 < size ) {
                str = str + "," + item.logic;
            } else {
                str = str + ",eol";
            }
            return str;
        }
        return "";
    };

    // convert an item to a string
    $scope.itemToPrettyString = function(item, i, size) {
        if ( item && item.word ) {
            var str = item.word;
            if ( item.type && item.type != 'any' ) {
                str = str + ":" + item.type;
            }
            if ( item.exact ) {
                str = str + " (exact)";
            }
            if ( i + 1 < size ) {
                str = str + " " + item.logic;
            }
            return str;
        }
        return "";
    };

    $scope.prettyPrint = function() {
        var str = "";
        var size = $scope.wordList.length;
        $.each($scope.wordList, function(i, item) {
            if ( item.word ) {
                if ( str.length > 0 ) {
                    str = str + ", ";
                }
                str = str + $scope.itemToPrettyString(item, i, size);
            }
        });
        return str;
    };

    // add the current word
    $scope.add = function() {
        if ( $scope.word.length > 1 && !$scope.contains($scope.wordList, $scope.word) && $scope.currentType != 'Type' ) {

            $("#txtWord").val('');

            $scope.wordList.push({'id': $scope.idCounter, 'logic': 'and', 'word': $scope.word, 'exact': $scope.exact, 'type': $scope.currentType});
            $scope.idCounter = $scope.idCounter + 1;

            if ( $scope.wordList.length > 1 ) {
                var id = $scope.wordList[$scope.wordList.length - 2].id;
                $("#logic_" + id).show();
            }
        }
    };

    // remove a word from the list
    $scope.removeWord = function (wordStr) {
        $scope.wordList = $scope.remove($scope.wordList, wordStr);
        if ( $scope.wordList.length > 0 ) {
            var id = $scope.wordList[$scope.wordList.length - 1].id;
            $("#logic_" + id).hide();
        }
    };

    // set id's logic to ... logic
    $scope.setLogic = function(id, logic) {
        var obj = globalSvc.getObjectFromListByID($scope.wordList, 'id', id);
        if ( obj ) {
            obj.logic = logic;
        }
    };

    $scope.showModal = function() {
        $scope.wordList = [];
        if ( wordBuilderSvc.title ) {
            $("#WordBuilderTitle").html(wordBuilderSvc.title);
        }
        if ( wordBuilderSvc.showMetadata ) {
            $("#trShowMetadata").show();
        } else {
            $("#trShowMetadata").hide();
            $scope.metadata = '';
        }
        $("#WordBuilderDialog").modal('show');
    };

    $scope.closeModal = function () {
        $("#WordBuilderDialog").modal('hide');
    };

    $scope.modalOK = function () {
        if ( $scope.wordList.length > 0 ) {
            var valid = false;

            var word_csv = "";
            var size = $scope.wordList.length;
            $.each($scope.wordList, function(i, item) {
                if ( word_csv.length > 0 ) {
                    word_csv = word_csv + "|";
                }
                word_csv = word_csv + $scope.itemToString(item, i, size);
            });

            wordBuilderSvc.result = {};
            wordBuilderSvc.result.data = {};

            if ( wordBuilderSvc.showMetadata && $scope.metadata.length > 1 ) {

                wordBuilderSvc.result.data.metadata = $scope.metadata;
                wordBuilderSvc.result.data.word_csv = word_csv;
                wordBuilderSvc.result.description = $scope.prettyPrint();
                wordBuilderSvc.result.type = wordBuilderSvc.type;
                valid = true;

            } else if ( !wordBuilderSvc.showMetadata ) {

                wordBuilderSvc.result.data.word_csv = word_csv;
                wordBuilderSvc.result.description = $scope.prettyPrint();
                wordBuilderSvc.result.type = wordBuilderSvc.type;
                valid = true;

            }
            if ( valid ) {
                $("#WordBuilderDialog").modal('hide');
                if (wordBuilderSvc.callback) {
                    wordBuilderSvc.callback();
                }
            }

        }
    };

    if ( wordBuilderSvc ) {
        // setup callbacks
        wordBuilderSvc.showModal = $scope.showModal;
        wordBuilderSvc.closeModal = $scope.closeModal;
    }

})
.directive('wordBuilder', function() {
    return {
        templateUrl: 'views/widgets/word_builder.html'
    };
});

