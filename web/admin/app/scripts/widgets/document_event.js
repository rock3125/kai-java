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
.controller('DocumentEvent', function($scope, documentEventSvc) {

    $scope.origin = '';
    $scope.document_type_csv = '';

    $scope.showModal = function() {
        $("#DocumentEventDialog").modal('show');
    };

    $scope.closeModal = function () {
        documentEventSvc.result = null;
        $("#DocumentEventDialog").modal('hide');
    };

    $scope.modalOK = function () {
        // set the result object
        documentEventSvc.result = {};
        documentEventSvc.result.data = {};

        if ( $scope.origin.length > 0 ) {
            documentEventSvc.result.data.origin_filter = $scope.origin;
        }

        if ( $scope.document_type_csv.length > 0 ) {
            documentEventSvc.result.data.document_type_filter = $scope.document_type_csv;
        }

        $("#DocumentEventDialog").modal('hide');
        if (documentEventSvc.callback) {
            documentEventSvc.callback();
        }
    };

    ////////////////////////////////////////////////////////////////////////////////

    // setup callbacks
    documentEventSvc.showModal = $scope.showModal;
    documentEventSvc.closeModal = $scope.closeModal;

    if ( documentEventSvc ) {
    }


})
.directive('documentEvent', function() {
    return {
        templateUrl: 'views/widgets/document_event.html'
    };
});

