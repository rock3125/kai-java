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
 * Peter's entity pagination component
 * pageName: the name of the url for navigation
 * ulName: the id of the dom component for html injection of the paging
 * serviceName: the id of the content page
 */
function EntityPagination( pageName, ulName, serviceName ) {

    var service = this;

    service.page = 0;
    service.items_per_page = 5; // list entirely on client side for now
    service.entityUrl = null;
    service.entityUrlArray = [];
    service.itemList = [];
    service.index = 0;
    service.prevFilter = null;

    // setup the pagination each time after recieving the results
    service.setup = function(list) {
        if ( list ) {
            service.itemList = list;
            if ( list.length == service.items_per_page ) {
                if ( service.index + 1 >= service.entityUrlArray.length ) {
                    service.entityUrlArray.push( list[list.length - 1].id );
                }
            }
            service.setupPagination();
        }
    };

    // setup the filter
    service.setupFilter = function(str) {
        if ( str != service.prevFilter ) {
            service.prevFilter = str;
            service.page = 0;
            service.index = 0;
            service.entityUrl = null;
            service.entityUrlArray = [];
            service.itemList = [];
        }
    };

    service.setupPagination = function () {
        var str = "";
        var disabledStr1 = (service.index > 0) ? "" : " class=\"disabled\"";
        str = str + '<li' + disabledStr1 +  '><button ' + ' onclick="$(\'#' + serviceName + '\').scope().prev();" class="btn btn-pagination">prev</button>';
        var disabledStr2 = (service.itemList.length == service.items_per_page) ? "" : " class=\"disabled\"";
        str = str + '<li' + disabledStr2 + '><button ' + ' onclick="$(\'#' + serviceName + '\').scope().next()"; class="btn btn-pagination">next</button>';
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
                service.entityUrl = null;
            } else {
                service.entityUrl = service.entityUrlArray[service.index-1];
            }
        } else {
            service.index = 0;
            service.entityUrl = null;
        }
        return true;
    };

    service.next = function() {
        console.log("goto next page");
        service.page = service.page + 1;
        if ( service.index < service.entityUrlArray.length ) {
            service.entityUrl = service.entityUrlArray[service.index];
            service.index = service.index + 1;
            return true;
        }
        return false; // can't go any further
    };

}
