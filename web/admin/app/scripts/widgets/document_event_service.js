
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
.service('documentEventSvc', function() {

    var service = this;

    service.showModal = null; // callbacks
    service.closeModal = null;

    service.type = null;
    service.callback = null;

    service.result = {};

    // pretty print the filter results
    service.prettyPrint = function() {
        var str = "any new document arrives";
        if ( service.result && service.result.data ) {
            if ( service.result.data.origin_filter ) {
                str = str + " from origin \"" + service.result.data.origin_filter + "\"";
            }
            if ( service.result.data.document_type_filter ) {
                str = str + " of type " + service.result.data.document_type_filter;
            }
        }
        return str;
    };

    // get the details for a url item - and do the callbacks
    service.show = function(type, callback) {
        service.type = type;
        if (service.showModal) {
            service.callback = callback;
            service.showModal();
        }
    };

});

