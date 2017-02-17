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
.controller('AddField', function($scope, addFieldSvc) {

    $scope.title1 = '';
    $scope.label1 = '';
    $scope.placeholder1 = '';

    $scope.value = '';

    $scope.showModal = function() {
        // setup incoming data
        $scope.label1 = addFieldSvc.label;
        $scope.placeholder1 = addFieldSvc.placeholder;
        $scope.title1 = addFieldSvc.title;

        $scope.value = '';

        $("#AddFieldDialog").modal('show');
    };

    $scope.closeModal = function () {
        addFieldSvc.timeGrid = {};
        $("#AddFieldDialog").modal('hide');
    };

    $scope.modalOK = function () {
        if ( $scope.value.length > 0 ) {

            addFieldSvc.result = {};
            addFieldSvc.result.data = {};
            addFieldSvc.result.data.value = $scope.value;
            addFieldSvc.result.description = addFieldSvc.prettyPrint();
            addFieldSvc.result.type = addFieldSvc.type;

            $("#AddFieldDialog").modal('hide');
            if (addFieldSvc.callback) {
                addFieldSvc.callback();
            }
        }
    };

    // setup callbacks
    addFieldSvc.showModal = $scope.showModal;
    addFieldSvc.closeModal = $scope.closeModal;

    if ( addFieldSvc ) {
    }

})
.directive('addField', function() {
    return {
        templateUrl: 'views/widgets/add_field.html'
    };
});

