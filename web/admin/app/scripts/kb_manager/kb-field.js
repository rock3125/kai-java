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
    .controller('KBField', function ($scope, $cookies, globalSvc, $location, entitySvc) {

    var session = null;
    $scope.entity = null; // the entity to edit (null if new)
    $scope.name = '';

    // entity settings
    $scope.entity_name = '';
    $scope.tab_visible = false;
    $scope.tab_name = '';
    $scope.html_template = ''; // template for tab search

    $scope.field_name = ''; // field's name
    $scope.field_indexed = true; // whether the field is indexed or not
    $scope.field_list = [];

    // saveEntity done
    $scope.saveSuccessfullCallback = function(success) {
        if ( success ) {
            globalSvc.setObject("kb_obj", null);
            $location.path("/kb-manager");
        }
    };

    $scope.get_name = function() {
        if ($scope.name.length > 0) {
            return $scope.name;
        }
        return "new item";
    };

    $scope.saveEntity = function() {
        if ($scope.entity && $scope.entity.id) {

            if ( $scope.entity_name.length > 0 && (!$scope.tab_visible ||
                $scope.tab_name.length > 0 && $scope.html_template.length > 0 ) ) {

                if ($scope.entity.id == null) {
                    $scope.entity.id = globalSvc.guid();
                }
                $scope.entity.field_list = $scope.field_list;
                $scope.entity.name = $scope.entity_name;
                $scope.entity.tab_visible = $scope.tab_visible;
                $scope.entity.tab_name = $scope.tab_name;
                $scope.entity.html_template = $scope.html_template;
                var entityObj = {
                    "id": $scope.entity.id,
                    "type": "schema",
                    "json_data": JSON.stringify($scope.entity)
                };
                entitySvc.saveEntity(session, entityObj, $scope.saveSuccessfullCallback);
            } else {
                globalSvc.error_message("you must provide data for all fields.")
            }
        }
    };

    // cancel the editing/creating of this object
    $scope.cancelEntity = function() {
        globalSvc.setObject("kb_obj", null);
        $location.path("/kb-manager");
    };

    // edit aiml on its screen
    $scope.aiml = function() {
        var temp = { "id": $scope.entity.id,
            "name": $scope.entity_name,
            "tab_visible": $scope.tab_visible,
            "tab_name": $scope.tab_name,
            "html_template": $scope.html_template,
            "field_list": $scope.field_list,
            "aiml_list": $scope.entity.aiml_list};
        globalSvc.setObject("kb_obj", temp);
        $location.path("/kb-aiml");
    };

    $scope.instances = function() {
        var temp = { "id": $scope.entity.id,
            "name": $scope.entity_name,
            "tab_visible": $scope.tab_visible,
            "tab_name": $scope.tab_name,
            "html_template": $scope.html_template,
            "field_list": $scope.field_list,
            "aiml_list": $scope.entity.aiml_list};
        globalSvc.setObject("kb_obj", temp);
        $location.path("/kb-instance");
    };

    // setup the ui for a new field
    $scope.newField = function() {
        $("#fieldModalTitle").html("new field");
        $scope.id = -1;
        $scope.field_name = '';
        $scope.field_indexed = true;
        $('#addField').modal('show');
    };

    // edit button - edit fiekd in a dialog
    $scope.edit = function(i, field_name, field_indexed) {
        if ( field_name && i >= 0 ) {
            $("#modalTitle").html("update field");
            $scope.field_name = field_name;
            $scope.field_indexed = field_indexed;
            $scope.id = i;
            $('#addField').modal('show');
        }
    };

    // delete a field
    $scope.delete = function(i, field_name) {
        if ( field_name && i >= 0 ) {
            bootbox.confirm("Are you sure you want to remove field " + field_name + "?",
                function(result) {
                    if ( result ) {
                        $scope.field_list.splice(i, 1);
                        $scope.$apply();
                    }
                });
        }
    };

    // save - add / update a field
    $scope.save = function() {
        var valid = true;
        // new accounts must have a password
        if ( $scope.field_name.length == 0 ) {
            globalSvc.error('you must supply a field name');
            valid = false;
        }
        if ( valid ) {
            var obj = {'name': $scope.field_name, 'indexed': $scope.field_indexed };
            if ( $scope.id >= 0 ) {
                $scope.field_list[$scope.id] = obj;
            } else {
                $scope.field_list.push(obj);
            }
            $('#addField').modal('hide');
            $scope.$apply();
        }
    };

    $scope.kb_has_id = function() {
        return $scope.entity.id != null;
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
                $scope.name = $scope.entity.name;
                $scope.field_list = $scope.entity.field_list;
                $scope.entity_name = $scope.entity.name;
                $scope.tab_visible = $scope.entity.tab_visible;
                $scope.tab_name = $scope.entity.tab_name;
                $scope.html_template = $scope.entity.html_template;
            }
        } });



});

