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
 * searchApp Module
 *
 * Description user service, user related functionality
 */
angular.module('searchApp').service('userSvc', function ($location, $rootScope, $cookies, $http, globalSvc) {

    // user assist functions
    var service = this;

    // calls back callback with the session id on success
    // calls back callback with null on failure (and sets the global error)
    service.login = function(username, password, callback) {
        console.log("login viki user");
        $http.get(securityServiceEntry + "security/token/" + encodeURIComponent(username) + "/" + encodeURIComponent(password)).then(
            function success(response) {
                if ( response && response.data.sessionID ) {
                    console.log("session id=" + response.data.sessionID);
                    globalSvc.setSession(response.data.sessionID);
                    if ( callback ) {
                        callback(response.data.sessionID);
                    }
                }
            }, function error(response) {
                globalSvc.error(response);
                if ( callback ) {
                    callback(null);
                }
            }
        );
    };


});

