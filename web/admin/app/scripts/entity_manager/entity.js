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
    .controller('EntityManagerController', function ($scope, $cookies, globalSvc, entitySvc) {

    var session = null;

    $scope.entityList = [];

    $scope.id = null;
    $scope.name = '';
    $scope.isa = '';
    $scope.alias_list = '';
    $scope.filter = '';

    var paginator = new EntityPagination('entity-manager', 'pagination', 'entityService');

    $scope.next = function() {
        if (paginator.next()) {
            entitySvc.getEntityList(session, 'entity', paginator.entityUrl, paginator.items_per_page,
                'name', $scope.filter, $scope.getEntityListDone);
        }
    };

    $scope.prev = function() {
        if (paginator.prev()) {
            entitySvc.getEntityList(session, 'entity', paginator.entityUrl, paginator.items_per_page,
                'name', $scope.filter, $scope.getEntityListDone);
        }
    };

    $scope.gotoPage = function(num) {
        if (paginator.gotoPage(num)) {
            entitySvc.getEntityList(session, 'entity', paginator.entityUrl, paginator.items_per_page,
                'name', $scope.filter, $scope.getEntityListDone);
        }
    };

    // callback after list load done
    $scope.getEntityListDone = function(entityList) {
        if ( entityList && entityList.list ) {
            console.log("entity list gotten");
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
        $("#modalTitle").html("new entity");
        $scope.id = null;
        $scope.name = '';
        $scope.isa = '';
        $scope.alias_list = '';
        $('#addEntity').modal('show');
    };

    // edit button - edit the object in the ui
    $scope.edit = function(id) {
        var entity = globalSvc.getObjectFromListByID($scope.entityList,'id',id);
        if ( entity && entity.json_data ) {
            $("#modalTitle").html("update entity");
            var temp = JSON.parse(entity.json_data);
            $scope.id = entity.id;
            $scope.name = temp.name;
            $scope.isa = temp.isa;
            if (!temp.alias_list) {
                temp.alias_list = [];
            }
            $scope.alias_list = temp.alias_list.join(",");
            $('#addEntity').modal('show');
        }
    };

    // delete an object
    $scope.delete = function(id) {
        var entity = globalSvc.getObjectFromListByID($scope.entityList,'id',id);
        if ( entity && entity.id && entity.json_data ) {
            var temp = JSON.parse(entity.json_data);
            bootbox.confirm("Are you sure you want to delete " + temp.name + "/" + temp.isa + "?",
                function(result) {
                    if ( result ) {
                        entitySvc.deleteEntity(session, 'entity', entity.id, $scope.deleteResult);
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
            entitySvc.getEntityList(session, 'entity', paginator.entityUrl, paginator.items_per_page,
                'name', $scope.filter, $scope.getEntityListDone)
        }
    };

    // deleted?
    $scope.deleteResult = function (success) {
        if ( success ) {
            globalSvc.info("entity deleted");
            // re-get the entity list
            entitySvc.getEntityList(session, 'entity', paginator.entityUrl, paginator.items_per_page,
                'name', $scope.filter, $scope.getEntityListDone)
        }
    };

    $scope.search = function() {
        console.log("apply filter");
        entitySvc.getEntityList(session, 'entity', paginator.entityUrl, paginator.items_per_page,
            'name', $scope.filter, $scope.getEntityListDone)
    };

    // signed in?
    globalSvc.getSession( function(pSession) {
        if ( !pSession ) {
            globalSvc.goHome();
        } else {
            session = pSession;
            // get the entity list
            entitySvc.getEntityList(session, 'entity', paginator.entityUrl, paginator.items_per_page,
                'name', $scope.filter, $scope.getEntityListDone)
        } });


});

