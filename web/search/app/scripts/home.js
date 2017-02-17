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
 * @name searchApp.controller:HomeService
 * @description
 * # HomeService
 * Controller of the searchApp
 */
angular.module('searchApp')
.controller('HomeService', function ($scope, $location, userSvc, globalSvc) {

    var session = null;

    $scope.email = '';
    $scope.password = '';

    $scope.disabled = false;

    // go back to main search site
    $scope.goHome = function () {
        console.log('go home: vocht.industries');
        window.location = "http://vocht.industries/";
    };

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
            console.log("login successful, goto /search");
            $location.path("/search");
        }
    };

    // signed in?
    globalSvc.getSession( function(pSession) {
        if ( !pSession ) {
            $location.path("/");
        } else {
            $location.path("/search");
        }
    });


    // on enter in password field - try and login
    $("#lg_password").keypress(function(e) {
        if(e.which == 13) {
            if ( !$scope.disabled ) {
                $scope.login();
            }
        }
    });

    var words = [
        'ARTIFICIAL INTELLIGENCE',
        'INTERACTIVE',
        'KEY PERFORMANCE',
        'ANSWERS',
        'INTELLIGENCE',
        'AWARENESS',
        'INSIGHTFUL',
        'KNOWLEDGE'
    ], i = 0;

    setInterval(function(){
        $('#changingword').fadeOut(function(){
            $(this).html(words[i=(i+1)%words.length]).fadeIn();
        });
    }, 6000);


});
