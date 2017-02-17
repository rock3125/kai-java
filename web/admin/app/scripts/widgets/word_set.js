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
.controller('WordSet', function($scope, wordSetSvc) {

    $scope.exact = false;
    $scope.word_list_csv = '';

    $scope.showModal = function() {
        $("#WordSetDialog").modal('show');
    };

    $scope.closeModal = function () {
        wordSetSvc.result = null;
        $("#WordSetDialog").modal('hide');
    };

    $scope.modalOK = function () {
        if ($scope.word_list_csv.length > 0) {

            // setup the result
            wordSetSvc.result = {};
            wordSetSvc.result.data = {};
            wordSetSvc.result.data.word_csv = $scope.word_list_csv;
            wordSetSvc.result.data.exact = $scope.exact;
            wordSetSvc.result.description = wordSetSvc.prettyPrint();

            $("#WordSetDialog").modal('hide');
            if (wordSetSvc.callback) {
                wordSetSvc.callback();
            }
        }
    };

    if ( wordSetSvc ) {
        // setup callbacks
        wordSetSvc.showModal = $scope.showModal;
        wordSetSvc.closeModal = $scope.closeModal;
    }

})
.directive('wordSet', function() {
    return {
        templateUrl: 'views/widgets/word_set.html'
    };
});

