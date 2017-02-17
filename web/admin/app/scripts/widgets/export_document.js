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
.controller('ExportDocument', function($scope, exportDocumentSvc) {

    $scope.protocol = '';
    $scope.url = '';
    $scope.path = '';
    $scope.username = '';
    $scope.password = '';
    $scope.domain = '';

    $scope.showModal = function() {
        $scope.protocol = '';
        $scope.url = '';
        $scope.path = '';
        $scope.username = '';
        $scope.password = '';
        $scope.domain = '';

        $("#ExportDocumentDialog").modal('show');
    };

    $scope.selectProto = function(protocol) {
        if ( protocol ) {
            $scope.protocol = protocol;
        }
    };

    $scope.closeModal = function () {
        exportDocumentSvc.result = null;
        $("#ExportDocumentDialog").modal('hide');
    };

    $scope.modalOK = function () {
        if ( $scope.protocol.length > 0 && $scope.url.length > 0 ) {

            exportDocumentSvc.result = {};
            exportDocumentSvc.result.data = {};
            exportDocumentSvc.result.data.protocol = $scope.protocol;
            exportDocumentSvc.result.data.url = $scope.url;
            exportDocumentSvc.result.data.path = $scope.path;
            exportDocumentSvc.result.data.username = $scope.username;
            exportDocumentSvc.result.data.password = $scope.password;
            exportDocumentSvc.result.data.domain = $scope.domain;
            exportDocumentSvc.result.description = exportDocumentSvc.prettyPrint();

            $("#ExportDocumentDialog").modal('hide');
            if (exportDocumentSvc.callback) {
                exportDocumentSvc.callback();
            }
        }
    };

    if ( exportDocumentSvc ) {
        // setup callbacks
        exportDocumentSvc.showModal = $scope.showModal;
        exportDocumentSvc.closeModal = $scope.closeModal;
    }

})
.directive('exportDocument', function() {
    return {
        templateUrl: 'views/widgets/export_document.html'
    };
});

