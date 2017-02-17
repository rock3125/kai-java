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
 * @name webApp.controller:AccountActivationController
 * @description
 * # AccountActivationController
 * Controller of the webApp
 */
angular.module('webApp')
    .controller('AccountActivationController', function ($scope, $http, $location, globalSvc) {

        var email = $location.$$search["email"];
        var activationid = $location.$$search["activationid"];

        // tell the user what is going on
        $scope.status = function(title, text) {
            console.log(text);
            $("#activationTitle").html(title);
            $("#activationText").html(text);
        };

        if ( email && email.length > 1 && activationid && activationid.length > 1 ) {
            $http({
                    "url": globalSvc.getNodeRR("Security") + "security/activate/" + encodeURIComponent(email) + "/" + encodeURIComponent(activationid),
                    method: 'POST',
                    contentType: 'application/json',
                    dataType: 'json'
                }
            ).then(
                function success(response) {
                    if (response && response.data) {
                        $scope.status("activation successful", "your account has been activated");
                    }
                }, function error(response) {
                    globalSvc.error(response, true);
                });
        } else {
            globalSvc.error("invalid activation link", "missing activationID or email parameters", true);
        }


    });
