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
 * @name webApp
 * @description
 * # webApp
 *
 * Main module of the application.
 */
angular
  .module('webApp', [
    'ngAnimate',
    'ngCookies',
    'ngResource',
    'ngRoute',
    'ngSanitize',
    'ngTouch',
    'Mac',
    'webApp.global'
  ])
  .config(function ($routeProvider) {
    $routeProvider

    .when('/', {
        templateUrl: 'views/home.html',
        controller: 'HomeController'
    })

    .when('/about', {
        templateUrl: 'views/about.html',
        controller: 'AboutController'
    })

    .when('/dashboard', {
        templateUrl: 'views/dashboard.html',
        controller: 'DashboardController'
    })

    .when('/login', {
        templateUrl: 'views/signin.html',
        controller: 'SignInController',
        controllerAs: 'signin'
    })

    .when('/signout', {
        templateUrl: 'views/signout.html',
        controller: 'SignOutController',
        controllerAs: 'signout'
    })

    .when('/register', {
        templateUrl: 'views/register.html',
        controller: 'SignInController',
        controllerAs: 'register'
    })

    .when('/resetPasswordRequest', {
        templateUrl: 'views/resetpasswordrequest.html',
        controller: 'ResetPasswordRequestController',
        controllerAs: 'resetPasswordRequest'
    })

    .when('/resetpassword', {
        templateUrl: 'views/resetpassword.html',
        controller: 'ResetPasswordController',
        controllerAs: 'resetPassword'
    })

    .when('/activate', {
        templateUrl: 'views/activation.html',
        controller: 'AccountActivationController',
        controllerAs: 'activationController'
    })

    .when('/accountActivationRequest', {
        templateUrl: 'views/activationrequest.html',
        controller: 'AccountActivationRequestController',
        controllerAs: 'activationRequestController'
    })

    .when('/user-manager', {
        templateUrl: 'views/user_manager/manager.html',
        controller: 'UserManagerController',
        controllerAs: 'userManager'
    })

    .when('/group-manager', {
        templateUrl: 'views/group_manager/groups.html',
        controller: 'GroupManagerController',
        controllerAs: 'groupManager'
    })

    .when('/entity-manager', {
        templateUrl: 'views/entity_manager/entity.html',
        controller: 'EntityManagerController',
        controllerAs: 'entityManager'
    })

    .when('/kb-manager', {
        templateUrl: 'views/kb_manager/kb-manager.html',
        controller: 'KBManager',
        controllerAs: 'kbManager'
    })

    .when('/kb-field', {
        templateUrl: 'views/kb_manager/kb-field.html',
        controller: 'KBField',
        controllerAs: 'kbField'
    })

    .when('/kb-instance', {
        templateUrl: 'views/kb_manager/kb-instance.html',
        controller: 'KBInstance',
        controllerAs: 'kbInstance'
    })

    .when('/kb-aiml', {
        templateUrl: 'views/kb_manager/kb-aiml.html',
        controller: 'KBAiml',
        controllerAs: 'kbAiml'
    })

    .when('/document-manager', {
        templateUrl: 'views/document_manager/document.html',
        controller: 'DocumentManagerController',
        controllerAs: 'documentManager'
    })

    .when('/similar-manager', {
        templateUrl: 'views/similar_manager/similar.html',
        controller: 'SimilarManagerController',
        controllerAs: 'similarManager'
    })

    .when('/settings', {
        templateUrl: 'views/settings.html',
        controller: 'SettingsController',
        controllerAs: 'settingsController'
    })

    .when('/search', {
        templateUrl: 'views/search/search.html',
        controller: 'SearchController',
        controllerAs: 'searchController'
    })

    .when('/summary', {
        templateUrl: 'views/summary_manager/summary.html',
        controller: 'SummaryManagerController',
        controllerAs: 'summaryManagerController'
    })

    .when('/search_anomaly', {
        templateUrl: 'views/search/search_anomaly.html',
        controller: 'SearchAnomalyController',
        controllerAs: 'searchAnomalyController'
    })

    .when('/cluster', {
        templateUrl: 'views/cluster_manager/clusters.html',
        controller: 'ClusterManagerController',
        controllerAs: 'clusterManagerController'
    })

    .when('/cluster-details', {
        templateUrl: 'views/cluster_manager/cluster_details.html',
        controller: 'ClusterDetailsManagerController',
        controllerAs: 'clusterDetailsManagerController'
    })

    .when('/anomaly', {
        templateUrl: 'views/anomaly_manager/anomaly.html',
        controller: 'AnomalyManagerController',
        controllerAs: 'anomalyManagerController'
    })

    .when('/document-anomaly', {
        templateUrl: 'views/anomaly_manager/document_anomaly.html',
        controller: 'DocumentAnomalyManagerController',
        controllerAs: 'documentAnomalyManagerController'
    })

    .when('/time', {
        templateUrl: 'views/time_manager/time.html',
        controller: 'TimeManagerController',
        controllerAs: 'timeManagerController'
    })

    .when('/day-time', {
        templateUrl: 'views/time_manager/day_time.html',
        controller: 'DayTimeManagerController',
        controllerAs: 'dayTimeManagerController'
    })

    .when('/rules', {
        templateUrl: 'views/rules_manager/rules.html',
        controller: 'RulesManagerController',
        controllerAs: 'rulesManagerController'
    })

    .when('/rule-editor', {
        templateUrl: 'views/rules_manager/rule_editor.html',
        controller: 'RuleEditorController',
        controllerAs: 'ruleEditorController'
    })

    .when('/reports', {
        templateUrl: 'views/report_manager/reports.html',
        controller: 'ReportManagerController',
        controllerAs: 'reportManagerController'
    })

    .when('/system-stats', {
        templateUrl: 'views/system_stats/statistics.html',
        controller: 'StatisticsManagerController',
        controllerAs: 'statisticsManagerController'
    })

    .when('/index-stats', {
        templateUrl: 'views/index_stats/statistics.html',
        controller: 'StatisticsIndexController',
        controllerAs: 'statisticsIndexController'
    })

    .when('/agents', {
        templateUrl: 'views/agents/agent_controller.html',
        controller: 'AgentController'
    })

    .when('/sessions', {
        templateUrl: 'views/session_manager/session_manager.html',
        controller: 'SessionController'
    })

    .when('/terms', {
        templateUrl: 'views/terms.html'
    })

    .otherwise({
        redirectTo: '/'
    });

  })
  .config(['$httpProvider', function ($httpProvider) {
        $httpProvider.defaults.useXDomain = true;
  }])
  .constant('config', {
      minPasswordLength: 8
  })
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
    })
    
    .directive('fileModel', ['$parse', function ($parse) {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                var model = $parse(attrs.fileModel);
                var modelSetter = model.assign;

                element.bind('change', function(){
                    scope.$apply(function(){
                        modelSetter(scope, element[0].files[0]);
                    });
                });
            }
        };
    }]);