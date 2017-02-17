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
 * @name webApp.controller:RulesManagerController
 * @description
 * # RulesManagerController
 * Controller of the webApp
 */
angular.module('webApp')
.controller('RulesManagerController', function ($scope, $cookies, $location, globalSvc, ruleSvc) {

    var session = null;

    $scope.ruleList = [];

    var paginator = new EntityPagination('rules', 'pagination', 'rulesManagerController');

    $scope.exec = function(rule_name) {
        if ( rule_name ) {
            ruleSvc.execute(session, rule_name, $scope.execCallback);
        }
    };

    $scope.execCallback = function(result) {
        if ( result ) {
            globalSvc.info_message("rule executing");
        }
    };

    $scope.next = function() {
        if (paginator.next()) {
            ruleSvc.getRuleList(session, paginator.entityUrl, paginator.items_per_page, $scope.getRuleListDone);
        }
    };

    $scope.prev = function() {
        if (paginator.prev()) {
            ruleSvc.getRuleList(session, paginator.entityUrl, paginator.items_per_page, $scope.getRuleListDone);
        }
    };

    // callback after list load done
    $scope.getRuleListDone = function(ruleList) {
        if ( ruleList && ruleList.rule_list ) {
            $scope.ruleList = ruleList.rule_list;
            paginator.setup(ruleList.rule_list);
        } else {
            $scope.ruleList = [];
        }
    };

    $scope.edit = function(name) {
        var rule = globalSvc.getObjectFromListByID($scope.ruleList,'rule_name',name);
        if ( rule ) {
            ruleSvc.rule = rule;
            $location.path("/rule-editor");
        }
    };

    $scope.delete = function(name) {
        var rule = globalSvc.getObjectFromListByID($scope.ruleList,'rule_name',name);
        if ( rule ) {
            bootbox.confirm("Are you sure you want to delete rule \"" + name + "\"?",
                function(result) {
                    if ( result ) {
                        ruleSvc.deleteRule(session, name, $scope.deleteResult);
                    }
                });
        }
    };

    // deleted?
    $scope.deleteResult = function (success) {
        if ( success ) {
            globalSvc.info("rule deleted");
            // re-get the rule list
            ruleSvc.getRuleList(session, paginator.entityUrl, paginator.items_per_page, $scope.getRuleListDone);
        }
    };

    // signed in?
    globalSvc.getSession( function(pSession) {
        if ( !pSession ) {
            globalSvc.goHome();
        } else {
            session = pSession;
            ruleSvc.rule = null;
            ruleSvc.getRuleList(session, paginator.entityUrl, paginator.items_per_page, $scope.getRuleListDone);
        } });


});

