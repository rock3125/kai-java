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
 * Description group service, group related functionality
 */
angular.module('searchApp').service('timeSvc', function ($location, $rootScope, $cookies, $http, globalSvc) {

    // time assist functions

    var service = this;

    // request time information for the specified range
    // specify -1 for either day and hour, or just hour to ignore the fine-grained-ness of the service
    service.getTimeSet = function(sessionID, 
                                    year1, month1, day1, hour1, min1,
                                    year2, month2, day2, hour2, min2, page, pageSize, callback) {
        console.log("get time set");
        $http.get(globalSvc.getNodeRR("Time") + "time/look-at/" + encodeURIComponent(sessionID) +
                                    "/" + encodeURIComponent(year1) + "/" + encodeURIComponent(month1) + "/" + encodeURIComponent(day1) + "/" +
                                          encodeURIComponent(hour1) + "/" + encodeURIComponent(min1) +
                                    "/" + encodeURIComponent(year2) + "/" + encodeURIComponent(month2) + "/" + encodeURIComponent(day2) + "/" +
                                          encodeURIComponent(hour2) + "/" + encodeURIComponent(min2) + "/" +
                                          encodeURIComponent(page) + "/" + encodeURIComponent(pageSize) ).then(
            function success(response) {
                if ( response && response.data ) {
                    callback(response.data);
                }
            }, function error(response) {
                globalSvc.error(response);
                callback(null);
            }
        );
    };


});

