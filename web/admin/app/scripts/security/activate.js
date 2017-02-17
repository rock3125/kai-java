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
 * @name webApp.controller:ActivateSecurityService
 * @description
 * # ActivateSecurityService
 * Controller of the webApp
 */
angular.module('webApp')
    .controller('ActivateSecurityService', function ($scope, $http, $cookies, GlobalService, $location, $rootScope) {

        var email = $location.$$search["email"];
        var activationid = $location.$$search["activationid"];

        if ( email && email.length > 1 && activationid && activationid.length > 1 ) {
            $http({
                    "url": securityServiceEntry + "security/activate/" + encodeURIComponent(email) + "/" +
                                    encodeURIComponent(activationid),
                    method: 'POST',
                    contentType: 'application/json',
                    dataType: 'json'
                }
            ).then(
                function success(response) {
                    if (response && response.data) {
                        $("#divPleaseWait").hide();
                        GlobalService.info("your account has been activated", true);
                    }
                }, function error(response) {
                    $("#divPleaseWait").hide();
                    var errorStr = GlobalService.getErrorTextFromResponse(response);
                    GlobalService.error(errorStr, true);
                });
        } else {
            GlobalService.error("invalid activation link: missing activationID or email parameters");
        }


    });
