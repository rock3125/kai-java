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
.controller('KBInstance', function ($scope, $cookies, $location, globalSvc, entitySvc) {

    var session = null;

    $scope.instanceList = [];

    $scope.entity = null;
    $scope.id = null;
    $scope.type = '';
    $scope.field_list = [];

    // file upload
    $scope.jsonUploadFile = null;

    // edit or new instance?
    $scope.instance_id = null;

    var paginator = new EntityPagination('kb-instance', 'pagination', 'kbInstance');

    // select upload file
    $scope.upload = function () {
        var val = $("#documentUpload3").val();
        if ($scope.jsonUploadFile == null || val.length == 0) {
            globalSvc.error_message("please click \"Choose file\" before selecting UPLOAD");
        } else {
            entitySvc.upload(session, $scope.type, val, $scope.jsonUploadFile, $scope.uploadCallback);
        }
    };

    // upload done?
    $scope.uploadCallback = function(success) {
        if ( success ) {
            $("#documentUpload3").val('');
            globalSvc.info("JSON data uploaded");
            $scope.jsonUploadFile = null;
            $scope.search(); // re-get data
        }
    };

    $scope.next = function() {
        if (paginator.next()) {
            entitySvc.getEntityList(session, $scope.type, paginator.entityUrl, paginator.items_per_page,
                $scope.field_list[0].name, $scope.filter, $scope.getEntityListDone);
        }
    };

    $scope.prev = function() {
        if (paginator.prev()) {
            entitySvc.getEntityList(session, $scope.type, paginator.entityUrl, paginator.items_per_page,
                $scope.field_list[0].name, $scope.filter, $scope.getEntityListDone);
        }
    };

    $scope.gotoPage = function(num) {
        if (paginator.gotoPage(num)) {
            entitySvc.getEntityList(session, $scope.type, paginator.entityUrl, paginator.items_per_page,
                $scope.field_list[0].name, $scope.filter, $scope.getEntityListDone);
        }
    };

    // back
    $scope.cancelInstance = function() {
        $location.path("/kb-field");
    };

    $scope.pretty_print = function(obj) {
        if (obj && obj.json_data) {
            var temp = JSON.parse(obj.json_data);
            var str = "";
            $.each(temp, function(i,item) {
               if (i != "id" && item.length > 0) {
                   if (str.length > 0) {
                       str += ", ";
                   }
                   str = str + item;
               }
            });
            if ( str.length > 50 ) {
                str = str.substr(0, 50) + " ...";
            }
            return str;
        }
        return "invalid instance";
    };

    // callback after list load done
    $scope.getEntityListDone = function(instanceList) {
        if ( instanceList && instanceList.list ) {
            console.log("entity instance list gotten");
            var el = [];
            $.each(instanceList.list, function(i, item) {
                if ( item && item.json_data ) {
                    el.push(item);
                }
            });
            $scope.instanceList = el;
            paginator.setup(instanceList.list);
        } else {
            $scope.instanceList = [];
        }
    };

    // setup the ui for a new entity
    $scope.newEntity = function() {
        $("#modalTitle").html("new instance");
        $scope.instance_id = null;
        $('#addInstance').modal('show');
    };

    // edit button - edit the object in the ui
    $scope.edit = function(id) {
        var entity = globalSvc.getObjectFromListByID($scope.instanceList,'id',id);
        if ( entity && entity.json_data ) {
            $("#modalTitle").html("update instance");
            $scope.instance_id = entity.id;
            var temp = JSON.parse(entity.json_data);
            $.each(temp, function(i, item) {
                if ( i != "id" ) {
                    $("#reg_" + i).val(item);
                }
            });
            $('#addInstance').modal('show');
        }
    };

    // delete an object
    $scope.delete = function(id) {
        var entity = globalSvc.getObjectFromListByID($scope.instanceList,'id',id);
        if ( entity && entity.id && entity.json_data ) {
            bootbox.confirm("Are you sure you want to delete " + $scope.pretty_print(entity) + "?",
                function(result) {
                    if ( result ) {
                        entitySvc.deleteEntity(session, $scope.type, entity.id, $scope.deleteResult);
                    }
                });
        }
    };

    // save - create or update the object
    $scope.save = function() {
        // gather data for the object
        var instance_obj = {};
        var first_empty = false;
        $.each($scope.field_list, function(i,item) {
            var value = $("#reg_" + item.name).val();
            instance_obj[item] = value;
            if (value.length == 0 && i == 0) {
                first_empty = true;
            }
        });
        if (first_empty) {
            globalSvc.error_message("first item can never be empty")
        } else {
            var id;
            if ( $scope.instance_id ) {
                id = $scope.instance_id;
            } else {
                id = globalSvc.guid();
            }
            instance_obj.id = id;
            var entityObj = {
                "id": id,
                "type": $scope.type,
                "json_data": JSON.stringify(instance_obj)
            };
            entitySvc.saveEntity(session, entityObj, $scope.saveCallback);
        }
    };

    // saved?
    $scope.saveCallback = function(success) {
        if ( success ) {
            globalSvc.info("instance saved");
            $('#addInstance').modal('hide');
            // re-get the entity list
            entitySvc.getEntityList(session, $scope.type, paginator.entityUrl, paginator.items_per_page,
                $scope.field_list[0].name, $scope.filter, $scope.getEntityListDone)
        }
    };

    // deleted?
    $scope.deleteResult = function (success) {
        if ( success ) {
            globalSvc.info("instance deleted");
            // re-get the entity list
            entitySvc.getEntityList(session, $scope.type, paginator.entityUrl, paginator.items_per_page,
                $scope.field_list[0].name, $scope.filter, $scope.getEntityListDone)
        }
    };

    $scope.search = function() {
        console.log("apply filter");
        entitySvc.getEntityList(session, $scope.type, paginator.entityUrl, paginator.items_per_page,
            $scope.field_list[0].name, $scope.filter, $scope.getEntityListDone)
    };

    // signed in?
    globalSvc.getSession( function(pSession) {
        if ( !pSession ) {
            globalSvc.goHome();
        } else {
            session = pSession;
            $scope.entity = globalSvc.getObject("kb_obj"); // access the data we're editing
            $scope.type = $scope.entity.name;
            $scope.id = $scope.entity.id;
            $scope.field_list = $scope.entity.field_list;
            // get the entity list
            entitySvc.getEntityList(session, $scope.type, paginator.entityUrl, paginator.items_per_page,
                $scope.field_list[0].name, $scope.filter, $scope.getEntityListDone)
        } });


});

