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
 * @name webApp.controller:UserManagerController
 * @description
 * # UserManagerController
 * Controller of the webApp
 */
angular.module('webApp')
    .controller('UserManagerController', function ($scope, $cookies, globalSvc, userSvc) {

        var session = null;
        $scope.userList = [];

        $scope.first_name = '';
        $scope.surname = '';
        $scope.password = '';
        $scope.passwordConfirm = '';
        $scope.email = '';
        $scope.id = '';
        $scope.filter = '';


        var paginator = new Pagination('user-manager', 'pagination', 'mgrService');

        $scope.next = function() {
            if (paginator.next()) {
                userSvc.getUserList(session, paginator.page, paginator.itemsPerPage, $scope.filter, $scope.getUserListDone);
            }
        };

        $scope.prev = function() {
            if (paginator.prev()) {
                userSvc.getUserList(session, paginator.page, paginator.itemsPerPage, $scope.filter, $scope.getUserListDone);
            }
        };

        $scope.gotoPage = function(num) {
            if (paginator.gotoPage(num)) {
                userSvc.getUserList(session, paginator.page, paginator.itemsPerPage, $scope.filter, $scope.getUserListDone);
            }
        };






        // callback after list load done
        $scope.getUserListDone = function(userList) {
            if ( userList && userList.user_list ) {
                $scope.userList = userList.user_list;
                paginator.setup(userList.total_user_count);
            } else {
                $scope.userList = [];
            }
        };

        $scope.newUser = function() {
            $("#modalTitle").html("new user");
            $("#reg_email").prop('disabled', false);
            $scope.first_name = '';
            $scope.surname = '';
            $scope.email = '';
            $scope.id = '';
            $scope.password = '';
            $scope.passwordConfirm = '';
            $('#addUser').modal('show');
        };

        $scope.edit = function(id) {
            var user = globalSvc.getObjectFromListByID($scope.userList,'id',id);
            if ( user ) {
                $("#modalTitle").html("update user");
                $("#reg_email").prop('disabled', true);
                $scope.first_name = user.first_name;
                $scope.surname = user.surname;
                $scope.email = user.email;
                $scope.id = user.id;
                $scope.password = '';
                $scope.passwordConfirm = '';
                $('#addUser').modal('show');
            }
        };

        $scope.delete = function(id) {
            var user = globalSvc.getObjectFromListByID($scope.userList,'id',id);
            if ( user ) {
                bootbox.confirm("Are you sure you want to delete " + user.first_name + " " + user.surname + "?",
                    function(result) {
                        if ( result ) {
                            userSvc.deleteUser(session, user.email, $scope.deleteResult);
                        }
                });
            }
        };

        $scope.save = function() {
            // password ok?
            var valid = true;
            if ($scope.password && $scope.password.length > 0 ) {
                if ( $scope.password != $scope.passwordConfirm ) {
                    globalSvc.error('passwords do not match');
                    valid = false;
                }
            }
            // new accounts must have a password
            if ( valid ) {
                if ( $scope.id.length == 0 && $scope.password.length == 0 ) {
                    globalSvc.error('a new user must have a password');
                    valid = false;
                }
            }
            if ( valid ) {
                if ( $scope.first_name.length == 0 ) {
                    globalSvc.error('you must supply a first-name');
                    valid = false;
                }
            }
            if ( valid ) {
                if ( $scope.surname.length == 0 ) {
                    globalSvc.error('you must supply a surname');
                    valid = false;
                }
            }
            if ( valid ) {
                if ( $scope.email.length == 0 ) {
                    globalSvc.error('you must supply an email');
                    valid = false;
                }
            }
            if ( valid ) {
                if ($scope.id && $scope.id.length > 0) {
                    var pwd = null;
                    if ( $scope.password.length > 0 ) {
                        pwd = $scope.password;
                    }
                    userSvc.updateUser(session, {
                        "id": $scope.id,
                        "email": $scope.email,
                        "first_name": $scope.first_name,
                        "surname": $scope.surname,
                        "password": pwd
                    }, $scope.saveCallback);
                } else {
                    userSvc.createUser(session, {
                        "email": $scope.email,
                        "first_name": $scope.first_name,
                        "surname": $scope.surname,
                        "password": $scope.password
                    }, $scope.saveCallback);
                }
            }
        };

        $scope.saveCallback = function(success) {
            if ( success ) {
                $('#addUser').modal('hide');
                // get the user list
                userSvc.getUserList(session, paginator.page, paginator.itemsPerPage, $scope.filter, $scope.getUserListDone)
            }
        };

        $scope.deleteResult = function (success) {
            if ( success ) {
                globalSvc.info("user deleted");
                // get the user list
                userSvc.getUserList(session, paginator.page, paginator.itemsPerPage, $scope.filter, $scope.getUserListDone)
            }
        };

        $scope.search = function() {
            console.log("apply filter");
            userSvc.getUserList(session, paginator.page, paginator.itemsPerPage, $scope.filter, $scope.getUserListDone)
        };

        // signed in?
        globalSvc.getSession( function(pSession) {
            if ( !pSession ) {
                globalSvc.goHome();
            } else {
                session = pSession;
                // get the user list
                userSvc.getUserList(session, paginator.page, paginator.itemsPerPage, $scope.filter, $scope.getUserListDone)
            } });


    });

