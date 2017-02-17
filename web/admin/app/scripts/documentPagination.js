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
function DocumentPagination( pageName, ulName, serviceName ) {

    var service = this;

    service.page = 0;
    service.itemsPerPage = 5; // list entirely on client side for now
    service.prevUrl = null;
    service.prevUrlArray = [];
    service.itemList = [];
    service.index = 0;
    service.prevFilter = null;

    // setup the pagination each time after recieving the results
    service.setup = function(list) {
        service.itemList = list;
        if ( list.length == service.itemsPerPage ) {
            if ( service.index + 1 >= service.prevUrlArray.length ) {
                service.prevUrlArray.push( list[list.length - 1].url );
            }
        }
        service.setupPagination();
    };

    // setup the filter
    service.setupFilter = function(str) {
        if ( str != service.prevFilter ) {
            service.prevFilter = str;
            service.page = 0;
            service.index = 0;
            service.prevUrl = null;
            service.prevUrlArray = [];
            service.itemList = [];
        }
    };

    service.setupPagination = function () {
        var str = "";
        var disabledStr1 = (service.index > 0) ? "" : " class=\"disabled\"";
        str = str + '<li' + disabledStr1 +  '><a href="#/' + pageName + '" onclick="$(\'#' + serviceName + '\').scope().prev();">prev</a>';
        var disabledStr2 = (service.itemList.length == service.itemsPerPage) ? "" : " class=\"disabled\"";
        str = str + '<li' + disabledStr2 + '><a href="#/' + pageName + '" onclick="$(\'#' + serviceName + '\').scope().next()";>next</a>';
        $("#" + ulName).html(str);
    };

    service.prev = function() {
        console.log("goto prev page");
        if ( service.index > 0 ) {
            service.page = service.page - 1;
            if ( service.page < 0 ) {
                service.page = 0;
            }
            service.index = service.index - 1;
            if ( service.index == 0 ) {
                service.prevUrl = null;
            } else {
                service.prevUrl = service.prevUrlArray[service.index-1];
            }
        } else {
            service.index = 0;
            service.prevUrl = null;
        }
        return true;
    };

    service.next = function() {
        console.log("goto next page");
        service.page = service.page + 1;
        if ( service.index < service.prevUrlArray.length ) {
            service.prevUrl = service.prevUrlArray[service.index];
            service.index = service.index + 1;
            return true;
        }
        return false; // can't go any further
    };

}
