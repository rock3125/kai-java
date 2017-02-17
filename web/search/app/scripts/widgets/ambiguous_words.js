
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
.service('ambiguousWordsSvc', function() {

    var service = this;

    // the grouped synset structure
    service.synset_set_list = [];
    service.setDataFn = null;

    // get the selections made for any synsets
    service.getSelectedSynsets = function() {
        var selected = [];
        $.each(service.synset_set_list, function(i, synset) {
            if ( synset.word && synset.selectedSynsetId >= 0 ) {
                selected.push( { "word": synset.word, "selectedSynsetId": synset.selectedSynsetId } );
            }
        });
        return selected;
    };

    service.setup = function( synset_set_list ) {
        if ( synset_set_list && synset_set_list.length > 0 ) {
            service.synset_set_list = synset_set_list;
            if ( service.setDataFn ) {
                service.setDataFn(service.synset_set_list);
            }
        } else {
            service.synset_set_list = []; // empty
            if ( service.setDataFn ) {
                service.setDataFn(service.synset_set_list);
            }
        }
    };


})

.controller('AmbiguousWordsController', function($scope, ambiguousWordsSvc) {

    $scope.synset_set_list = [];

    $scope.setData = function(synset_set_list) {
        $scope.synset_set_list = synset_set_list;
        // setup the current selection (if any)
        $.each($scope.synset_set_list, function(i,obj) {
            if ( obj.selectedSynsetId >= 0 && obj.selectedSynsetId < obj.synset_list.length ) {
                obj.selectedWord = obj.synset_list[obj.selectedSynsetId].uniqueWord;
            } else {
                obj.selectedWord = obj.word;
            }
        });
    };

    $scope.getWord = function (word) {
        var selectedObj = null;
        if ( word ) {
            $.each( $scope.synset_set_list, function(i, obj) {
                if ( obj.word == word ) {
                    selectedObj = obj;
                }
            });
        }
        return selectedObj;
    };

    $scope.selectSyn = function(word, selectedWord,synsetId) {
        var obj = $scope.getWord(word);
        if ( obj ) {
            obj.selectedWord = selectedWord;
            obj.selectedSynsetId = synsetId;
        }
    };

    ambiguousWordsSvc.setDataFn = $scope.setData;

})

.directive('ambiguousWords', function() {
    return {
        templateUrl: 'views/widgets/ambiguous_words.html'
    };
});
