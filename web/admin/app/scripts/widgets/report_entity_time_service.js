
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
.service('reportEntityTimeSvc', function() {

    var service = this;

    service.showModal = null; // callbacks
    service.closeModal = null;
    service.callback = null;

    service.result = null;

    // get the details for a url item - and do the callbacks
    service.show = function(callback) {
        if (service.showModal) {
            service.callback = callback;
            service.showModal();
        }
    };

});

