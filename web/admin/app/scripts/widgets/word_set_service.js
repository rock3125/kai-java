
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
.service('wordSetSvc', function() {

    var service = this;

    service.showModal = null; // callbacks
    service.closeModal = null;
    service.callback = null;
    service.result = null;

    service.prettyPrint = function() {
        var str = "";
        if ( service.result != null && service.result.data  ) {
            var size = service.result.data.word_list.length;
            var oversize = false;
            if ( size > 3 ) {
                size = 3;
                oversize = true;
            }
            str = "statistical anomalies with ";
            if ( service.result.data.exact ) {
                str = str + "exact matches of ";
            } else {
                str = str + "semantic matches of ";
            }
            for ( var i = 0; i < size; i++ ) {
                str = str + service.result.data.word_list[i];
                if ( i + 1 < size ) {
                    str = str + ", ";
                }
            }
            if ( oversize ) {
                str = str + " ...";
            }
        }
        return str;
    };

    service.show = function (callback) {
        service.callback = callback;
        if (service.showModal) {
            service.showModal();
        }
    };

});

