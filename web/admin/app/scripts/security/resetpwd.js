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
 * @name webApp.controller:ResetPwdSecurityService
 * @description
 * # ResetPwdSecurityService
 * Controller of the webApp
 */
angular.module('webApp')
    .controller('ResetPwdSecurityService', function ($scope, $http, $cookies, GlobalService, config, $location, $rootScope) {

        $scope.password1 = '';
        $scope.password2 = '';

        $scope.disabled = false; // form processing - disable flag

        var email = $location.$$search["email"];
        var resetid = $location.$$search["resetid"];

        // hide the form if parameters are missing
        if (!( email && email.length > 1 && resetid && resetid.length > 1 )) {
            $scope.status("invalid password-reset link", "missing email/reset id parameters");
            $("#resetPasswordDiv").hide();
        }

        // click the reset button - do the reset
        $scope.reset = function() {
            if ( $scope.password1.length < config.minPasswordLength ) {
                $scope.status("error", "your password must be at least " + config.minPasswordLength + " characters long");
            } else if ( $scope.password1 != $scope.password2 ) {
                $scope.status("error", "your passwords do not match");
            } else {
                $scope.disabled = true;
                $http({
                        "url": securityServiceEntry + "security/password-reset/" + encodeURIComponent(email) + "/" +
                        encodeURIComponent(resetid) + "/" + encodeURIComponent($scope.password1),
                        method: 'POST',
                        contentType: 'application/json',
                        dataType: 'json'
                    }
                ).then(
                    function success(response) {
                        $scope.disabled = false;
                        if (response && response.data) {
                            GlobalService.info("your password has been reset", true);
                            $location.path("/");
                        }
                    }, function error(response) {
                        $scope.disabled = false;
                        GlobalService.error(response);
                    });
            }
        };


    });
