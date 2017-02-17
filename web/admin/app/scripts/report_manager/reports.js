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
 * @name webApp.controller:ReportManagerController
 * @description
 * # ReportManagerController
 * Controller of the webApp
 */
angular.module('webApp')
.controller('ReportManagerController', function ($scope, $cookies, globalSvc, reportSvc, reportEntityTimeSvc) {

    var session = null;

    $scope.reportList = [];

    ////////////////////////////////////////////////////////////////////////////////

    $scope.getReportCallback = function(data) {
        if ( data && data.reportList ) {
            $scope.reportList = data.reportList;
        } else {
            $scope.reportList = [];
        }
    };

    $scope.reportCallback = function () {

    };

    $scope.run = function(id) {
        if ( id ) {
            reportEntityTimeSvc.show($scope.reportCallback);
        }
    };

    ////////////////////////////////////////////////////////////////////////////////

    // signed in?
    globalSvc.getSession( function(pSession) {
        if ( !pSession ) {
            globalSvc.goHome();
        } else {
            session = pSession;
            reportSvc.getReportList(session, $scope.getReportCallback);
        }
    });


});


