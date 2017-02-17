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
 * @name webApp.controller:AnomalyManagerService
 * @description
 * # AnomalyManagerService
 * Controller of the webApp
 */
angular.module('webApp')
.controller('AgentController', function ($scope, $http, $cookies, globalSvc) {

    var session = null;

    $scope.agentList = [
        {'description': 'Enron email uploader', 'technology': 'Java 8', 'platform': 'all', 'url': 'http://blah', 'instructions': 'http://instructions'},
        //{'description': 'Windows files', 'technology': '.NET 3.5', 'platform': 'Windows', 'url': 'http://blah', 'instructions': 'http://instructions'},
        {'description': 'Windows files', 'technology': 'Java 8', 'platform': 'all', 'url': 'http://blah', 'instructions': 'http://instructions'}
        // {'description': 'Microsoft Sharepoint', 'technology': '.NET 3.5', 'platform': 'Windows', 'url': 'http://blah', 'instructions': 'http://instructions'},
        // {'description': 'Microsoft CRM 3.0', 'technology': '.NET 3.5', 'platform': 'Windows', 'url': 'http://blah', 'instructions': 'http://instructions'},
        // {'description': 'Microsoft Exchange', 'technology': '.NET 3.5', 'platform': 'Windows', 'url': 'http://blah', 'instructions': 'http://instructions'}
    ];

    $scope.download = function(url) {
        alert("download:  " + url);
    };

    $scope.instructions = function(url) {
        alert("instructions:  " + url);
    };

    // signed in?
    globalSvc.getSession( function(pSession) {
        if ( !pSession ) {
            globalSvc.goHome();
        } else {
            session = pSession;
        }
    });

});

