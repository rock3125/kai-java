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
 * @name webApp.controller:GroupManagerController
 * @description
 * # GroupManagerController
 * Controller of the webApp
 */
angular.module('webApp')
    .controller('GroupManagerController', function ($scope, $http, $cookies, globalSvc, groupSvc) {

        var session = null;

        $scope.groupList = [];

        $scope.name = '';
        $scope.user_list = [];
        $scope.filter = '';

        var paginator = new Pagination('group-manager', 'pagination', 'groupService');

        $scope.next = function() {
            if (paginator.next()) {
                groupSvc.getGroupList(session, paginator.page, paginator.itemsPerPage, $scope.filter, $scope.getGroupListDone);
            }
        };

        $scope.prev = function() {
            if (paginator.prev()) {
                groupSvc.getGroupList(session, paginator.page, paginator.itemsPerPage, $scope.filter, $scope.getGroupListDone);
            }
        };

        $scope.gotoPage = function(num) {
            if (paginator.gotoPage(num)) {
                groupSvc.getGroupList(session, paginator.page, paginator.itemsPerPage, $scope.filter, $scope.getGroupListDone);
            }
        };

        // callback after list load done
        $scope.getGroupListDone = function(groupList) {
            if ( groupList && groupList.group_list ) {
                console.log("group list gotten");
                $scope.groupList = groupList.group_list;
                paginator.setup(groupList.total_group_count);
            } else {
                $scope.groupList = [];
            }
        };

        // setup the ui for a new group
        $scope.newGroup = function() {
            $("#modalTitle").html("new group");
            $scope.name = '';
            $scope.user_list = [];
            $("#reg_group").prop('disabled', false);
            $('#addGroup').modal('show');
        };

        // edit button - edit the object in the ui
        $scope.edit = function(name) {
            var group = globalSvc.getObjectFromListByID($scope.groupList,'name',name);
            if ( group ) {
                $("#modalTitle").html("update group");
                $scope.name = group.name;
                $scope.user_list = group.user_list.join(",");
                $("#reg_group").prop('disabled', true);
                $('#addGroup').modal('show');
            }
        };

        // delete an object
        $scope.delete = function(name) {
            var group = globalSvc.getObjectFromListByID($scope.groupList,'name',name);
            if ( group ) {
                bootbox.confirm("Are you sure you want to delete group " + group.name + "?",
                    function(result) {
                        if ( result ) {
                            groupSvc.deleteGroup(session, group.name, $scope.deleteResult);
                        }
                    });
            }
        };

        // save - create or update the object
        $scope.save = function() {
            // password ok?
            var valid = true;
            // new accounts must have a password
            if ( $scope.name.length == 0 || $scope.user_list.length == 0 ) {
                globalSvc.error('you must supply a group name and at least one user');
                valid = false;
            }
            if ( valid ) {
                if ($scope.name && $scope.name.length > 0) {
                    groupSvc.updateGroup(session, {
                        "name": $scope.name,
                        "user_list": $scope.user_list.split(",") // csv string to list
                    }, $scope.saveCallback);
                } else {
                    groupSvc.createGroup(session, {
                        "name": $scope.name,
                        "user_list": $scope.user_list.split(",") // csv string to list
                    }, $scope.saveCallback);
                }
            }
        };

        // saved?
        $scope.saveCallback = function(success) {
            if ( success ) {
                globalSvc.info("group saved");
                $('#addGroup').modal('hide');
                // re-get the group list
                groupSvc.getGroupList(session, paginator.page, paginator.itemsPerPage, $scope.filter, $scope.getGroupListDone)
            }
        };

        // deleted?
        $scope.deleteResult = function (success) {
            if ( success ) {
                globalSvc.info("group deleted");
                // re-get the group list
                groupSvc.getGroupList(session, paginator.page, paginator.itemsPerPage, $scope.filter, $scope.getGroupListDone)
            }
        };

        $scope.search = function() {
            console.log("apply group filter");
            groupSvc.getGroupList(session, paginator.page, paginator.itemsPerPage, $scope.filter, $scope.getGroupListDone)
        };

        // signed in?
        globalSvc.getSession( function(pSession) {
            if ( !pSession ) {
                globalSvc.goHome();
            } else {
                session = pSession;
                // get the group list
                groupSvc.getGroupList(session, paginator.page, paginator.itemsPerPage, $scope.filter, $scope.getGroupListDone)
            } });


    });
