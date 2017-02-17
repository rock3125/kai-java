
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
.service('addMetadataSvc', function() {

    var service = this;

    service.showModal = null; // callbacks
    service.closeModal = null;
    service.callback = null;
    service.type = null;

    service.result = null;

    service.prettyPrint = function() {
        var str = "";
        if ( service.result && service.result.data ) {
            if (service.type == 'remove-metadata') {
                str = str + "metadata item " + service.result.data.name;
            } else {
                str = str + "metadata item " + service.result.data.name + " = \"" + service.result.data.value + "\"";
            }
        }
        return str;
    };

    service.show = function (type, callback) {
        service.type = type;
        service.callback = callback;
        if (service.showModal) {
            service.showModal();
        }
    };

});

