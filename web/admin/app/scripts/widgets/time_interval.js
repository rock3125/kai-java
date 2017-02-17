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

angular.module('webApp')
    .controller('TimeInterval', function($scope, timeIntervalSvc) {

        $scope.interval = '';
        $scope.interval_unit = 'Unit';

        $scope.showModal = function() {
            $("#TimeIntervalDialog").modal('show');
        };

        $scope.closeModal = function () {
            timeIntervalSvc.interval = null;
            timeIntervalSvc.interval_unit = null;
            $("#TimeIntervalDialog").modal('hide');
        };

        $scope.modalOK = function () {
            if ( $scope.interval != '' && $scope.interval_unit != 'Unit' && parseInt($scope.interval) > 0 ) {

                timeIntervalSvc.result = {};
                timeIntervalSvc.result.data = {};
                timeIntervalSvc.result.data.interval = $scope.interval;
                timeIntervalSvc.result.data.interval_unit = $scope.interval_unit;
                timeIntervalSvc.result.description = timeIntervalSvc.prettyPrint();

                $("#TimeIntervalDialog").modal('hide');
                if (timeIntervalSvc.callback) {
                    timeIntervalSvc.callback();
                }
            }
        };

        $scope.selectUnit = function( interval_unit ) {
            if ( interval_unit ) {
                $scope.interval_unit = interval_unit;
            }
        };

        // setup callbacks
        timeIntervalSvc.showModal = $scope.showModal;
        timeIntervalSvc.closeModal = $scope.closeModal;

        if ( timeIntervalSvc ) {
            if ( timeIntervalSvc.interval > 0 ) {
                $scope.interval = timeIntervalSvc.interval;
                if ( timeIntervalSvc.interval_unit ) {
                    $scope.interval_unit = timeIntervalSvc.interval_unit;
                    $scope.selectUnit(interval_unit);
                }
            }
        }

    })
    .directive('timeInterval', function() {
        return {
            templateUrl: 'views/widgets/time_interval.html'
        };
    });

