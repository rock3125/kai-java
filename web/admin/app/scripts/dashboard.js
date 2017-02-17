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
 * @name webApp.controller:DashboardController
 * @description
 * # DashboardController
 * Controller of the webApp
 */
angular.module('webApp')
.controller('DashboardController', function ($scope, globalSvc, $location) {

    // signed in?
    globalSvc.getSession( function(session) {
        if ( !session ) {
            globalSvc.goHome();
        }
    }, function(user) {
        if ( user ) {
            //$("#who").value(user.first_name);
        }
    });


});
