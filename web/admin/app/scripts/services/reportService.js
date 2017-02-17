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
 * Description rule service, rule related functionality
 */
angular.module('webApp').service('reportSvc', function ($location, $rootScope, $cookies, $http, globalSvc) {

    var service = this;
    service.rule = null; // temp storage for edit

    // calls back callback with null on failure (and sets the global error)
    service.getReportList = function(sessionID, callback) {
        console.log("get report list");
        // report-list/{sessionID}
        $http.get(globalSvc.getNodeRR("Report") + "reports/report-list/" + encodeURIComponent(sessionID) ).then(
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


