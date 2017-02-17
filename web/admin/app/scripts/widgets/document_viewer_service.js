
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
.service('documentViewerSvc', function(summarySvc, documentSvc, anomalySvc, statsSvc) {

    var service = this;

    service.url = null;
    service.gotAllData = 0; // coordinate get getting of async resources before showing

    service.metadata = {};
    service.summarisation_item = [];
    service.emotional_graph = [];
    service.stats = null;

    service.title = "";
    service.session = null; // the session object of the user
    service.showModal = null; // callbacks
    service.closeModal = null;

    // counter to determine when all data has been received
    service.gotAllData = 0;

    service.summarySetCallback = function(data) {
        if (data && data.summarisationItemList && data.summarisationItemList.length == 1) {
            service.summarisation_item = data.summarisationItemList[0];
            service.gotAllData = service.gotAllData + 1;
        } else {
            service.summarisation_item = [];
        }
        if (service.gotAllData == 4 && service.showModal) {
            service.showModal();
        }
    };

    service.metadataCallback = function(data) {
        if (data && data.map ) {
            service.metadata = data.map;
            service.gotAllData = service.gotAllData + 1;
        } else {
            service.metadata = {};
        }
        if (service.gotAllData == 4 && service.showModal) {
            service.showModal();
        }
    };

    service.anomalyCallback = function(data) {
        if (data && data.emotionalSetList ) {
            service.emotional_graph = data.emotionalSetList;
            service.gotAllData = service.gotAllData + 1;
        } else {
            service.emotional_graph = [];
        }
        if (service.gotAllData == 4 && service.showModal) {
            service.showModal();
        }
    };

    service.statsCallback = function(data) {
        if ( data ) {
            service.stats = data;
            service.gotAllData = service.gotAllData + 1;
        } else {
            service.stats = null;
        }
        if (service.gotAllData == 4 && service.showModal) {
            service.showModal();
        }
    };

    // get the details for a url item - and do the callbacks
    service.show = function(session, url) {
        if ( session && url ) {

            service.session = session;
            service.url = url;

            service.gotAllData = 0;

            service.metadata = {};
            service.summarisation_item = [];
            service.emotional_graph = [];
            service.emotional_graph = [];
            service.title = url;

            summarySvc.getSummarySet(service.session, [url], service.summarySetCallback);
            documentSvc.getMetadata(service.session, url, service.metadataCallback);
            anomalySvc.getAnomalySet(service.session, [url], service.anomalyCallback);
            statsSvc.getDocumentStatistics(service.session, url, service.statsCallback);

        }
    };


});

