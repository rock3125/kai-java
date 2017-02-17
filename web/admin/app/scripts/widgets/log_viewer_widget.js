
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

.service('logViewerWidgetSvc', function() {

    var service = this;

    // setup data callback to controller
    service.setupDataFn = null;
    service.showModalFn = null;
    service.getDataFn = null;

    service.callbackIntervalInMS = 2500;

    // setup the text and the dialog
    service.setup = function(str) {
        if ( service.setupDataFn ) {
            service.setupDataFn(str);
            if ( service.showModalFn ) {
                service.showModalFn();
            }
        }
    };

    // update the text inside the widget
    service.update = function(str) {
        if ( service.setupDataFn ) {
            service.setupDataFn(str);
        }
    };


})

.controller('LogViewerController', function($scope, $interval, logViewerWidgetSvc) {

    $scope.text_list = [];

    var intervalCallback;

    $scope.setText = function(list) {
        if (list && list.length > 0 ) {
            $scope.text_list = list;
        } else {
            $scope.text_list = [];
        }
    };

    $scope.showModal = function() {
        intervalCallback = undefined;
        $("#LogViewerDialog").modal({backdrop: 'static', 'keyboard': 'false'});
    };

    $scope.closeModal = function() {
        // stop the interval running
        if (angular.isDefined(intervalCallback)) {
            $interval.cancel(intervalCallback);
            intervalCallback = undefined;
        }
        $("#LogViewerDialog").modal('hide');
    };

    // setup callback
    logViewerWidgetSvc.setupDataFn = $scope.setText;
    logViewerWidgetSvc.showModalFn = $scope.showModal;

    // stop the callback

    // setup a callback
    if ( !angular.isDefined(intervalCallback) ) {
        intervalCallback = $interval(function () {
            if (logViewerWidgetSvc.getDataFn) {
                console.log('update log');
                logViewerWidgetSvc.getDataFn();
            } }, logViewerWidgetSvc.callbackIntervalInMS);
    }

})

.directive('logViewer', function() {
    return {
        templateUrl: 'views/widgets/log_viewer.html'
    };
});

