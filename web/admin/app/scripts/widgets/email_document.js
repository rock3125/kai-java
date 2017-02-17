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
.controller('EmailDocument', function($scope, emailDocumentSvc) {

    $scope.to = '';
    $scope.subject = '';

    $scope.showModal = function() {
        // setup incoming data
        $scope.to = '';
        $scope.subject = '';
        $("#EmailDocumentDialog").modal('show');
    };

    $scope.closeModal = function () {
        emailDocumentSvc.result = null;
        $("#EmailDocumentDialog").modal('hide');
    };

    $scope.modalOK = function () {
        if ( $scope.to.length > 0 && $scope.subject.length > 0 ) {

            emailDocumentSvc.result = {};
            emailDocumentSvc.result.data = { 'to': $scope.to, 'subject': $scope.subject };
            emailDocumentSvc.result.description = emailDocumentSvc.prettyPrint();

            $("#EmailDocumentDialog").modal('hide');

            if (emailDocumentSvc.callback) {
                emailDocumentSvc.callback();
            }
        }
    };

    // setup callbacks
    emailDocumentSvc.showModal = $scope.showModal;
    emailDocumentSvc.closeModal = $scope.closeModal;

    if ( emailDocumentSvc ) {
    }

})
.directive('emailDocument', function() {
    return {
        templateUrl: 'views/widgets/email_document.html'
    };
});

