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

'use strict';

/**
 * @ngdoc function
 * @name searchApp.controller:teachService
 * @description
 * # teachService
 * Controller of the searchApp
 */
angular.module('searchApp')
    .controller('TeachController', function ($scope, globalSvc, teachSvc ) {

    var session = null;

    $scope.teach = function() {

        var text = $("#txtTeach").val();
        if ( text && text.length > 0 ) {
            //teachSvc.teach( session, text, $scope.teachDone);

            // this is google speech
            var synth = window.speechSynthesis;
            if ( synth ) {
                console.log('using google tts');
                var utterance = new SpeechSynthesisUtterance(text);
                utterance.lang = 'en-GB';
                synth.speak(utterance);
            } else {
                // this is our own Mary TTS
                console.log('using mary tts');
                var ta = $("#teach_audio")[0];
                ta.src = globalSvc.getNodeRR("Speech") + 'speech/to-speech/' + encodeURIComponent(session) + "/" + encodeURIComponent(text);
                var v = document.getElementsByTagName("audio")[0];
                v.volume = 0.5;
                v.play();
            }

        }

    };

    $scope.teachDone = function (data) {
        if ( data ) {
            globalSvc.info_message('new information learned.');
        }
    };


    //////////////////////////////////////////////////////////////

    $scope.signout = function() {
        globalSvc.setSession(null);
        $location.path("/");
    };

    // signed in?
    globalSvc.getSession( function(pSession) {
        if ( !pSession ) {
            console.log("logout");
            $scope.signout();
        } else {
            session = pSession;
        }
    });


});

