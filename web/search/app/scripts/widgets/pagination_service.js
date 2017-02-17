
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

angular.module('searchApp')
.service('paginationSvc', function() {

    var service = this;

    service.prevCallback = null;
    service.nextCallback = null;
    service.page = 0;
    service.itemsPerPage = 5;

    service.reset = function() {
        service.page = 0;

        $("#liNext")
            .removeAttr("disabled", "true")
            .removeAttr("readonly", "true");

        $("#liPrev")
            .attr("disabled", "true")
            .attr("readonly", "true");
    };

    service.setupResults = function(list) {
        if ( list && list.length < service.itemsPerPage ) {
            $("#liNext")
                .attr("disabled", "true")
                .attr("readonly", "true");
        } else {
            $("#liNext")
                .removeAttr("disabled", "true")
                .removeAttr("readonly", "true");
        }
    };

    service.prev = function() {
        if ( service.page > 0 ) {
            service.page = service.page - service.itemsPerPage;
            if ( service.page < 0 ) {
                service.page = 0;
            }

            $("#liNext")
                .removeAttr("disabled", "true")
                .removeAttr("readonly", "true");

            if ( service.page == 0 ) {
                $("#liPrev")
                    .attr("disabled", "true")
                    .attr("readonly", "true");
            }

            if ( service.prevCallback ) {
                service.prevCallback();
            }
            return true;
        } else {
            return false;
        }
    };

    service.next = function() {
        service.page = service.page + service.itemsPerPage;

        $("#liPrev")
            .removeAttr("disabled")
            .removeAttr("readonly");

        if ( service.nextCallback ) {
            service.nextCallback();
        }
        return true;
    };

    service.setup = function( prev, next ) {
        service.prevCallback = prev;
        service.nextCallback = next;
        $("#liPrev")
            .attr("disabled", "true")
            .attr("readonly", "true");
        service.setupResults([]);
    };


})

.controller('PaginationController', function($scope, paginationSvc) {

    $scope.prev = function() {
        paginationSvc.prev();
    };

    $scope.next = function() {
        paginationSvc.next();
    };

})

.directive('pagination', function() {
    return {
        templateUrl: 'views/widgets/pagination.html'
    };
});
