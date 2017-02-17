
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
.service('emailDocumentSvc', function() {

    var service = this;

    service.showModal = null; // callbacks
    service.closeModal = null;
    service.callback = null;

    service.result = null;

    service.prettyPrint = function() {
        var str = "email document to ";
        str = str + " \"" + service.result.data.to + "\"";
        return str;
    };

    service.show = function (callback) {
        service.callback = callback;

        if (service.showModal) {
            service.showModal();
        }
    };

});

