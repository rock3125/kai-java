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
 * @name webApp.controller:ResetPasswordReuqestService
 * @description
 * # ResetPasswordReuqestService
 * Controller of the webApp
 */
angular.module('webApp')
    .controller('ResetPasswordRequestController', function ($scope, $http, $location, globalSvc) {

        $scope.email = '';
        $scope.disabled = false; // form processing - disable flag

        // validate the email address supplied
        $scope.validEmail = function() {
            return globalSvc.validateEmail($scope.email);
        };

        // submit the password reset request
        $scope.submit = function() {
            if ( !$scope.validEmail() ) {
                globalSvc.error("invalid email address");
            } else {
                $scope.disabled = true;
                $http({
                        "url": securityServiceEntry + "security/password-reset-request/" + encodeURIComponent($scope.email),
                        method: 'POST',
                        contentType: 'application/json',
                        dataType: 'json'
                    }
                ).then(
                    function success(response) {
                        $scope.disabled = false;
                        if (response && response.data) {
                            globalSvc.info("your password reset request has been sent. please check your email in a few minutes.", true);
                            $location.path("#/");
                        }
                    }, function error(response) {
                        $scope.disabled = false;
                        globalSvc.error(response);
                    });
            }
        };


    });
