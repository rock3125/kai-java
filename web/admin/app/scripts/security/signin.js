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
 * @name webApp.controller:SignInController
 * @description
 * # SignInController
 * Controller of the webApp
 */
angular.module('webApp')
    .controller('SignInController', function ($scope, $http, globalSvc, userSvc, $location) {

        $scope.organisation = '';
        $scope.email = '';
        $scope.password = '';
        $scope.passwordConfirm = '';
        $scope.remember = false;
        $scope.firstname = '';
        $scope.lastname = '';
        $scope.agreeToTerms = false;

        $scope.disabled = false;

        // perform the login
        $scope.login = function() {
            if ( $scope.email && $scope.password && !$scope.disabled) {
                $scope.disabled = true;
                userSvc.login($scope.email, $scope.password, $scope.loginCallback);
            }
        };

        // callback when login fails or succeeds
        $scope.loginCallback = function(sessionID) {
            $scope.disabled = false;
            if ( sessionID ) {
                $("#signinMenu").hide();
                $("#signoutMenu").show();
                $location.path("/dashboard");
            }
        };

        // click create a new user
        $scope.createUser = function() {
            if (!$scope.agreeToTerms) {
                globalSvc.error("you must agree to the terms");
            } else if ($scope.password != $scope.passwordConfirm) {
                    globalSvc.error("passwords do not match");
            } else if ( $scope.email && $scope.organisation && $scope.password &&
                        $scope.firstname && $scope.lastname && !$scope.disabled) {
                $scope.disabled = true;
                userSvc.createUserOrganisation(null, $scope.organisation, {
                    "first_name": $scope.firstname,
                    "surname": $scope.lastname,
                    "password": $scope.password,
                    "email": $scope.email
                }, $scope.createUserCallback );
            } else {
                globalSvc.error("you must complete all fields");
            }
        };

        // create user callback
        $scope.createUserCallback = function(hasError) {
            $scope.disabled = false;
            if ( !hasError ) {
                globalSvc.info("user created successfully");
                $location.path("/login");
            }
        };

        // do the forgot password action
        $scope.forgotPassword = function() {
            if ( $scope.email && !$scope.disabled) {
                $scope.disabled = true;
                userSvc.passwordResetRequest($scope.email, $scope.passwordResetCallback);
            }
        };

        $scope.passwordResetCallback = function(hasError) {
            $scope.disabled = false;
            if ( !hasError ) {
                globalSvc.info("password reset request sent successfully");
            } else {
                globalSvc.error("error sending password reset request");
            }
        };


    });
