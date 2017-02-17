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
 * @name webApp.controller:EntityManagerController
 * @description
 * # EntityManagerController
 * Controller of the webApp
 */
angular.module('webApp')
.controller('KBAiml', function ($scope, $cookies, globalSvc, $location, entitySvc) {

    var session = null;
    $scope.aiml_list = [];  // aiml data
    $scope.index = -1;  // selected edit item
    $scope.entity_name = '';  // the entity being edited

    // dialog box
    $scope.language = '';
    $scope.html_template = '';
    $scope.selected_field = '';

    // saveEntity done
    $scope.saveSuccessfullCallback = function(success) {
        if ( success ) {
            $location.path("/kb-field");
        }
    };

    // save aiml parts of the object
    $scope.saveEntity = function() {
        if ($scope.entity && $scope.entity.id) {
            $scope.entity.aiml_list = $scope.aiml_list;
            var entityObj = {
                "id": $scope.entity.id,
                "type": "schema",
                "json_data": JSON.stringify($scope.entity)
            };
            globalSvc.setObject("kb_obj", $scope.entity);
            entitySvc.saveEntity(session, entityObj, $scope.saveSuccessfullCallback);
        }
    };

    // cancel the editing/creating of this object
    $scope.cancelEntity = function() {
        $location.path("/kb-field");
    };

    // create a new aiml field
    $scope.newAimlField = function() {
        $scope.index = -1;
        $scope.language = '';
        $scope.html_template = '';
        $scope.selected_field = '';
        $('#addAimlField').modal('show');
    };

    $scope.edit = function(index, field) {
        $scope.index = index;
        $scope.language = $scope.aiml_list[index].language;
        $scope.html_template = $scope.aiml_list[index].html_template;
        $scope.selected_field = field;
        $('#addAimlField').modal('show');
    };

    $scope.delete = function(index, field) {
        if (index >= 0) {
            bootbox.confirm("Are you sure you want to remove aiml commands for field \"" + field + "\"?",
                function(result) {
                    if ( result ) {
                        $scope.aiml_list.splice(index, 1);
                        $scope.$apply();
                    }
                });
        }
    };

    // close dialog
    $scope.saveAimlDialog = function() {
        if ($scope.language.length > 0 && $scope.html_template.length > 0 &&
            $scope.selected_field ) {

            if ($scope.index >= 0) {
                $scope.aiml_list[$scope.index].language = $scope.language;
                $scope.aiml_list[$scope.index].html_template = $scope.html_template;
                $scope.aiml_list[$scope.index].field = $scope.selected_field;
            } else {
                $scope.aiml_list.push({
                    "language": $scope.language,
                    "html_template": $scope.html_template,
                    "field": $scope.selected_field
                });
            }
            $scope.$apply();
            $('#addAimlField').modal('hide');
        } else {
            globalSvc.error_message('you must provide values for all fields.');
        }
    };

    $scope.setField = function(field) {
        $scope.selected_field = field;
    };

    // signed in?
    globalSvc.getSession( function(pSession) {
        if ( !pSession ) {
            globalSvc.goHome();
        } else {
            session = pSession;
            $scope.entity = globalSvc.getObject("kb_obj"); // access the data we're editing
            if ($scope.entity) {
                $scope.id = $scope.entity.id;
                $scope.aiml_list = $scope.entity.aiml_list;
                $scope.entity_name = $scope.entity.name;
            }
        } });


});

