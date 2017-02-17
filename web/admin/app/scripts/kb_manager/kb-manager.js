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
    .controller('KBManager', function ($scope, $cookies, globalSvc, $location, entitySvc) {

    var session = null;

    $scope.entityList = [];
    // example schema list
    // $scope.entityList1 = [
    //     {"id": "1", "name": "address book", "field_list": [{"name": "some name", "indexed": true}],
    //         "tab_visible": true,
    //         "tab_name": "Our people",
    //         "aiml_list": [ {"language_list": ["who *","who is *"], "field": "name", "html_template": ""}]
    //     }
    // ];

    // url name, pagination control id, page div id
    var paginator = new EntityPagination('kb-manager', 'pagination', 'kbService');

    $scope.next = function() {
        if (paginator.next()) {
            entitySvc.getEntityList(session, 'schema', paginator.entityUrl, paginator.items_per_page,
                'name', $scope.filter, $scope.getSchemaListDone);
        }
    };

    $scope.prev = function() {
        if (paginator.prev()) {
            entitySvc.getEntityList(session, 'schema', paginator.entityUrl, paginator.items_per_page,
                'name', $scope.filter, $scope.getSchemaListDone);
        }
    };

    $scope.gotoPage = function(num) {
        if (paginator.gotoPage(num)) {
            entitySvc.getEntityList(session, 'schema', paginator.entityUrl, paginator.items_per_page,
                'name', $scope.filter, $scope.getSchemaListDone);
        }
    };

    // callback after list load done
    $scope.getSchemaListDone = function(entityList) {
        if ( entityList && entityList.list ) {
            console.log("schema list gotten");
            var el = [];
            $.each(entityList.list, function(i, item) {
                if ( item && item.json_data ) {
                    var temp = JSON.parse(item.json_data);
                    temp.id = item.id;
                    temp.origin = item.origin;
                    temp.json_data = item.json_data;
                    el.push(temp);
                }
            });
            $scope.entityList = el;
            paginator.setup(entityList.list);
        } else {
            $scope.entityList = [];
        }
    };

    // setup the ui for a new entity
    $scope.newEntity = function() {
        var new_obj = {"id": null, "name": "", "field_list": [],
            "tab_vislble": false, "tab_name": "", "aiml_list": []};
        globalSvc.setObject("kb_obj", new_obj);
        $location.path("/kb-field")
    };

    // edit button - edit the object in the ui
    $scope.edit = function(id) {
        var entity = globalSvc.getObjectFromListByID($scope.entityList,'id',id);
        if ( entity ) {
            globalSvc.setObject("kb_obj", entity);
            $location.path("/kb-field")
        }
    };

    // delete an object
    $scope.delete = function(id) {
        var entity = globalSvc.getObjectFromListByID($scope.entityList,'id',id);
        if ( entity && entity.id && entity.json_data ) {
            var temp = JSON.parse(entity.json_data);
            bootbox.confirm("Are you sure you want to delete \"" + temp.name + "\"?",
                function(result) {
                    if ( result ) {
                        entitySvc.deleteEntity(session, 'schema', entity.id, $scope.deleteResult);
                    }
                });
        }
    };

    // save - create or update the object
    $scope.save = function() {
        // password ok?
        var valid = true;
        // new accounts must have a password
        if ( $scope.name.length == 0 || $scope.isa.length == 0 ) {
            globalSvc.error('you must supply an entity name/isa');
            valid = false;
        }
        if ( valid ) {
            var jsonStr = JSON.stringify({"name": $scope.name,
                "isa": $scope.isa,
                "alias_list": $scope.alias_list.split(",")}); // csv string to list

            var obj = {"type": "entity", "json_data": jsonStr };
            if ( $scope.id ) {
                obj.id = $scope.id;
            }
            entitySvc.saveEntity(session, obj, $scope.saveCallback);
        }
    };

    // saved?
    $scope.saveCallback = function(success) {
        if ( success ) {
            globalSvc.info("entity saved");
            $('#addEntity').modal('hide');
            // re-get the entity list
            entitySvc.getEntityList(session, 'schema', paginator.entityUrl, paginator.items_per_page,
                'name', $scope.filter, $scope.getSchemaListDone)
        }
    };

    // deleted?
    $scope.deleteResult = function (success) {
        if ( success ) {
            globalSvc.info("entity deleted");
            // re-get the entity list
            entitySvc.getEntityList(session, 'schema', paginator.entityUrl, paginator.items_per_page,
                'name', $scope.filter, $scope.getSchemaListDone)
        }
    };

    $scope.search = function() {
        console.log("apply filter");
        entitySvc.getEntityList(session, 'schema', paginator.entityUrl, paginator.items_per_page,
            'name', $scope.filter, $scope.getSchemaListDone)
    };

    // signed in?
    globalSvc.getSession( function(pSession) {
        if ( !pSession ) {
            globalSvc.goHome();
        } else {
            session = pSession;
            // get the entity list
            entitySvc.getEntityList(session, 'schema', paginator.entityUrl, paginator.items_per_page,
                'name', $scope.filter, $scope.getSchemaListDone)
        } });


});

