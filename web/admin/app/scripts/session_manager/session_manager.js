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
 * @name webApp.controller:SessionController
 * @description
 * # SessionController
 * Controller of the webApp
 */
angular.module('webApp')
.controller('SessionController', function ($scope, $cookies, globalSvc, userSvc) {

    var session = null;
    $scope.sessionList = [];



    // callback after list load done
    $scope.getSesssionListDone = function(data) {
        if ( data && data.sessionList ) {
            $scope.sessionList = data.sessionList;
        } else {
            $scope.sessionList = [];
        }
    };

    // signed in?
    globalSvc.getSession( function(pSession) {
        if ( !pSession ) {
            globalSvc.goHome();
        } else {
            session = pSession;
            // get the user list
            userSvc.getSessionList(session, 0, 100, $scope.getSesssionListDone)
        }
    });


});

