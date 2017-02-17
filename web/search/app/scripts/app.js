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
 * @ngdoc overview
 * @name searchApp
 * @description
 * # searchApp
 *
 * Main module of the search application.
 */
angular
  .module('searchApp', [
    'ngAnimate',
    'ngCookies',
    'ngResource',
    'ngRoute',
    'ngSanitize',
    'ngTouch',
    'Mac'
  ])
    

  .config(function ($routeProvider) {
    $routeProvider

    .when('/', {
        templateUrl: 'views/home.html',
        controller: 'HomeService',
        controllerAs: 'homeService'
    })

    .when('/license', {
        templateUrl: 'views/license.html'
    })

    .when('/reset-password-request', {
        templateUrl: 'views/security/resetpasswordrequest.html',
        controller: 'ResetPasswordRequestService',
        controllerAs: 'resetPasswordRequest'
    })

    .when('/reset-password', {
        templateUrl: 'views/security/resetpassword.html',
        controller: 'ResetPasswordService',
        controllerAs: 'resetPassword'
    })

    .when('/search', {
        templateUrl: 'views/search.html',
        controller: 'SearchController'
    })

    .when('/files', {
        templateUrl: 'views/files.html',
        controller: 'FilesController',
        controllerAs: 'filesController'
    })

    .when('/experts', {
        templateUrl: 'views/experts.html',
        controller: 'ExpertsController',
        controllerAs: 'expertsController'
    })

    .when('/times', {
        templateUrl: 'views/times.html',
        controller: 'TimeController',
        controllerAs: 'timeController'
    })

    .when('/teach', {
        templateUrl: 'views/teach.html',
        controller: 'TeachController'
    })

    .when('/user-tab', {
        templateUrl: 'views/user-tab.html',
        controller: 'UserTabController',
        controllerAs: 'userTabController'
    })


    .otherwise({
        redirectTo: '/'
    });

  })


.config(['$httpProvider', function ($httpProvider) {
    $httpProvider.defaults.useXDomain = true;
}])


.directive('ngEnter', function() {
    return function(scope, element, attrs) {
        element.bind("keydown keypress", function(event) {
            if(event.which === 13) {
                scope.$apply(function(){
                    scope.$eval(attrs.ngEnter, {'event': event});
                });

                event.preventDefault();
            }
        });
    };
});

