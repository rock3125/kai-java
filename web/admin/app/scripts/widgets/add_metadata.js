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
.controller('AddMetadata', function($scope, addMetadataSvc) {

    $scope.name = '';
    $scope.value = '';

    $scope.showModal = function() {
        $scope.name = '';
        $scope.value = '';
        if ( addMetadataSvc.type == 'remove-metadata' ) {
            $("#AddMetadataTitle").html("Remove Metadata");
            $("#txtValue").hide();
        } else {
            $("#AddMetadataTitle").html("Add Metadata");
            $("#txtValue").show();
        }
        $("#AddMetadataDialog").modal('show');
    };

    $scope.closeModal = function () {
        addMetadataSvc.result = null;
        $("#AddMetadataDialog").modal('hide');
    };

    $scope.modalOK = function () {
        if ( $scope.name.length > 1 && (addMetadataSvc.type == 'remove-metadata' || $scope.value.length > 0 ) ) {

            addMetadataSvc.result = {};
            addMetadataSvc.result.data = {};
            addMetadataSvc.result.data.name = $scope.name;
            addMetadataSvc.result.data.value = $scope.value;
            addMetadataSvc.result.description = addMetadataSvc.prettyPrint();
            addMetadataSvc.result.type = addMetadataSvc.type;

            $("#AddMetadataDialog").modal('hide');
            if (addMetadataSvc.callback) {
                addMetadataSvc.callback();
            }
        }
    };

    if ( addMetadataSvc ) {
        // setup callbacks
        addMetadataSvc.showModal = $scope.showModal;
        addMetadataSvc.closeModal = $scope.closeModal;
    }

})
.directive('addMetadata', function() {
    return {
        templateUrl: 'views/widgets/add_metadata.html'
    };
});

