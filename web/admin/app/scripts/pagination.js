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

/**
 * Peter's pagination component
 */
function Pagination( pageName, ulName, serviceName ) {

    var service = this;

    service.page = 0;
    service.itemsPerPage = 10; // list entirely on client side for now
    service.paginationItemsOnscreen = 5; // how many numbers in the paginator
    service.numPages = 1;
    service.currentPage = 0;
    service.totalItemCount = 0;

    // setup the pagination each time after recieving the results
    service.setup = function( totalItemCount ) {
        service.totalItemCount = totalItemCount;
        service.numPages = parseInt(totalItemCount / service.itemsPerPage);
        if ( totalItemCount % service.itemsPerPage != 0 ) {
            service.numPages = service.numPages + 1;
        }
        service.setupPagination();
    };

    // reset, go back to page zero
    service.reset = function() {
        service.page = 0;
        service.setupPagination();
    };

    service.setupPagination = function () {
        var str = "";
        var disabledStr = (service.currentPage > 0) ? "" : " class=\"disabled\"";
        str = str + '<li' + disabledStr +  '><a href="#/' + pageName + '" onclick="$(\'#' + serviceName + '\').scope().prev();">prev</a>';
        var endPoint = service.currentPage + service.paginationItemsOnscreen;
        var numPages = parseInt(service.totalItemCount / service.itemsPerPage);
        if ( endPoint > numPages ) {
            endPoint = numPages;
        }
        if ( endPoint == service.currentPage ) {
            endPoint = service.currentPage + 1;
        }
        for ( var i = service.currentPage; i < endPoint; i++ ) {
            var activeStr = (i == service.page) ? "class=\"active\"" : "";
            str = str + '<li ' + activeStr + '><a href="#/' + pageName + '" onclick="$(\'#' + serviceName + '\').scope().gotoPage(' + i + ');">' + (i+1) + '</a>';
        }
        if ( service.currentPage + service.paginationItemsOnscreen < service.numPages ) {
            str = str + '<li><a href="#/' + pageName + '" onclick="$(\'#' + serviceName + '\').scope().next()";>next</a>';
        }
        $("#" + ulName).html(str);
    };

    service.prev = function() {
        console.log("goto prev page");
        if ( service.currentPage > 0 ) {
            service.currentPage = service.currentPage - service.paginationItemsOnscreen;
            if ( service.currentPage < 0 ) {
                service.currentPage = 0;
            }
            service.page = service.currentPage;
            return true;
        } else {
            return false;
        }
    };

    service.next = function() {
        console.log("goto next page");
        if ( service.currentPage + service.paginationItemsOnscreen < service.totalItemCount ) {
            service.currentPage = service.currentPage + service.paginationItemsOnscreen;
            service.page = service.currentPage;
            return true;
        } else {
            return false;
        }
    };

    service.gotoPage = function(newPage) {
        console.log("goto page " + newPage);
        if ( newPage >= 0 ) {
            service.page = newPage;
            return true;
        } else {
            return false;
        }
    };


}
