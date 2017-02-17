
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

angular.module('searchApp')
.service('documentViewerSvc', function(summarySvc, documentSvc, anomalySvc, statsSvc, knowledgeSvc) {

    var service = this;

    service.num_calls = 4;  // how many calls to expect

    service.url = null;
    service.gotAllData = 0; // coordinate get getting of async resources before showing

    service.metadata = {};
    service.summarisation_item = [];
    service.emotional_graph = [];
    service.stats = null;
    service.knowledge = [];

    service.title = "";
    service.session = null; // the session object of the user
    service.showModal = null; // callbacks
    service.closeModal = null;

    // counter to determine when all data has been received
    service.gotAllData = 0;

    service.summarySetCallback = function(data) {
        if (data && data.summarisationItemList && data.summarisationItemList.length == 1) {
            service.summarisation_item = data.summarisationItemList[0];
        } else {
            service.summarisation_item = [];
        }
        service.gotAllData = service.gotAllData + 1;
        if (service.gotAllData >= service.num_calls && service.showModal) {
            service.showModal();
        }
    };

    service.metadataCallback = function(data) {
        if (data && data.map ) {
            service.metadata = data.map;
        } else {
            service.metadata = {};
        }
        service.gotAllData = service.gotAllData + 1;
        if (service.gotAllData >= service.num_calls && service.showModal) {
            service.showModal();
        }
    };

    service.anomalyCallback = function(data) {
        if (data && data.emotionalSetList ) {
            service.emotional_graph = data.emotionalSetList;
        } else {
            service.emotional_graph = [];
        }
        service.gotAllData = service.gotAllData + 1;
        if (service.gotAllData >= service.num_calls && service.showModal) {
            service.showModal();
        }
    };

    service.statsCallback = function(data) {
        if ( data ) {
            service.stats = data;
        } else {
            service.stats = null;
        }
        service.gotAllData = service.gotAllData + 1;
        if (service.gotAllData >= service.num_calls && service.showModal) {
            service.showModal();
        }
    };

    service.knowledgeCallback = function(data) {
        if ( data && data.caseTupleList ) {
            service.knowledge = data.caseTupleList;
        } else {
            service.knowledge = [];
        }
        service.gotAllData = service.gotAllData + 1;
        if (service.gotAllData >= service.num_calls && service.showModal) {
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
            service.knowledge = [];
            service.stats = null;
            service.title = url;

            summarySvc.getSummarySet(service.session, [url], service.summarySetCallback);
            documentSvc.getMetadata(service.session, url, service.metadataCallback);
            anomalySvc.getAnomalySet(service.session, [url], service.anomalyCallback);
            statsSvc.getDocumentStatistics(service.session, url, service.statsCallback);
            //knowledgeSvc.getTuplesForDocument(service.session,url, 0, 10, service.knowledgeCallback);
            service.num_calls = 4;

        }
    };


});

