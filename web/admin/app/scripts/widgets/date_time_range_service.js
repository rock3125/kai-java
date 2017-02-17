
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
.service('dateTimeRangeSvc', function() {

    var service = this;

    service.showModal = null; // callbacks
    service.closeModal = null;

    service.callback = null;

    service.result = null;

    service.title = null;
    service.type = null;

    // show the dialog
    service.show = function(title, type, callback) {
        if ( title && type ) {
            service.title = title;
            service.type = type;
            if (service.showModal) {
                service.callback = callback;
                service.showModal();
            }
        }
    };

});

