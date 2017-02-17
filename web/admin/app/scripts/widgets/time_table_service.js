
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
.service('timeTableSvc', function() {

    var service = this;

    service.showModal = null; // callbacks
    service.closeModal = null;
    service.callback = null;

    service.timeGrid = {};

    service.result = null;

    // return the internal data as a string
    service.getDataAsString = function() {
        var str = "";
        $.each(service.timeGrid, function (name, value) {
            if (value) {
                if ( str.length > 0 ) {
                    str = str + ",";
                }
                str = str + name;
            }
        });
        return str;
    };

    // set the internal data using a csv string
    service.setDataFromString = function(str) {
        service.timeGrid = {};
        var strArray = str.split(",");
        for ( var i = 0; i < strArray.length; i++ ) {
            service.timeGrid[strArray[i]] = true;
        }
    };

    // get a descriptive string of the times in the time-grid
    service.prettyPrint = function() {
        var data = [];
        $.each( service.timeGrid, function(name, value) {
            if ( value ) {
                data.push(name);
            }
        });
        data.sort();

        var timeSet = [];
        var prefixArray = ["mon-", "tue-", "wed-", "thu-", "fri-", "sat-", "sun-"];
        for ( var prefix in prefixArray ) {
            var prefixStr = prefixArray[prefix];
            var str = "";
            for (var i = 0; i < data.length; i++) {
                var item = data[i];
                if (item.startsWith(prefixStr)) {
                    var num = item.substr(4);
                    if (num.length == 1) {
                        num = "0" + num;
                    }
                    num = num + ":00";
                    if (str.length > 0) {
                        str = str + ", ";
                    }
                    str = str + num;
                }
            }
            if (str.length > 0) {
                if ( prefixStr == 'mon-' ) {
                    str = "Monday " + str;
                } else if ( prefixStr == 'tue-' ) {
                    str = "Tuesday " + str;
                } else if ( prefixStr == 'wed-' ) {
                    str = "Wednesday " + str;
                } else if ( prefixStr == 'thu-' ) {
                    str = "Thursday " + str;
                } else if ( prefixStr == 'fri-' ) {
                    str = "Friday " + str;
                } else if ( prefixStr == 'sat-' ) {
                    str = "Saturday " + str;
                } else if ( prefixStr == 'sun-' ) {
                    str = "Sunday " + str;
                }
                timeSet.push(str);
            }
        }

        var finalStr = "every ";
        for ( var i = 0; i < timeSet.length; i++ ) {
            var timeStr = timeSet[i];
            finalStr = finalStr + timeStr + "; ";
        }
        return finalStr;
    };



    // get the details for a url item - and do the callbacks
    service.show = function(callback) {
        if (service.showModal) {
            service.callback = callback;
            service.showModal();
        }
    };


});

