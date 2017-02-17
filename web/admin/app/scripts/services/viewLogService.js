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
 * webApp Module
 *
 * Description user service, user related functionality
 */
angular.module('webApp').service('viewLogSvc', function ($location, $rootScope, $cookies, $http, globalSvc) {

    // user assist functions

    var service = this;

    // view a numberOfLines of a log at node url
    service.viewLog = function (sessionID, url, numberOfLines, successCallback) {
        if (url && numberOfLines ) {
            $http({
                    "url": url + "view-log/log/" + encodeURIComponent(sessionID) + "/" + encodeURIComponent(numberOfLines),
                    method: 'GET',
                    contentType: 'application/json',
                    dataType: 'json'
                }
            ).then(
                function success(response) {
                    if (response && response.data) {
                        if (successCallback) {
                            successCallback(response.data);
                        }
                    }
                }, function error(response) {
                    globalSvc.error(response);
                    if (successCallback) {
                        successCallback(null);
                    }
                });
        } else {
            globalSvc.error("function missing parameters url / numberOfLines");
        }
    };


});
