
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

.service('keywordSearchSvc', function($http, globalSvc) {

    var service = this;

    // variables
    var leftchannel = [];
    var rightchannel = [];
    var recorder = null;
    var recording = false;
    var recordingLength = 0;
    var volume = null;
    var audioInput = null;
    var sampleRate = null;
    var audioContext = null;
    var context = null;

    // google's speech recognition
    var recognition = null;

    // the session available for search
    service.session = null;

    // upload recorded sound for text conversion
    service.upload = function(data, uploadCallback) {
        console.log('uploading sound wave');
        var fd = new FormData();
        fd.append('wave', data);
        var url1 = globalSvc.getNodeRR("Speech") + 'speech/to-text/' + encodeURIComponent(service.session);
        $http.post(url1, fd, {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        })
            .success(function(response) {
                if ( response && uploadCallback && response.text && response.text.length > 0 ) {
                    uploadCallback(response.text);
                } else if ( uploadCallback ) {
                    uploadCallback('');
                }
            }).error(function(response) {
            globalSvc.error(response);
            if ( uploadCallback ) {
                uploadCallback(null);
            }
        });
    };

    // speech -> text callback
    // callback after the sound upload has done and text is returned to the from the service
    service.uploadDone = function(text) {
        if ( text ) {
            final_span.value = text;
        }
    };

    // start with an AI query against the AIML system
    service.query = function(str, callback) {
        var url1 = globalSvc.getNodeRR("Search") + 'search/ai-query/' + encodeURIComponent(service.session) + "/" + encodeURIComponent(str);
        $http.get(url1).then(
            function success(response) {
                if ( response && response.data ) {
                    service.queryResult(response.data, callback);
                }
            }, function error(response) {
                service.queryResult(null, callback);
            }
        );
    };

    // AIML system query response
    service.queryResult = function(result, callback) {
        if (result && result.message) {
            service.speakString(service.cleanupForSpeech(result.message));
            if ( callback ) {
                callback(result.message);
            }
        } else {
            if ( callback ) {
                callback(null); // failed aiml
            }
        }
    };

    // cleanup the string for speech compatibility
    service.cleanupForSpeech = function(str) {
        if ( str ) {
            var str1 = str.replace("KAI", "kai");
            str1 = str1.replace(" de ", "the");
            str1 = str1.replace("Vocht", "Vok");
            return str1;
        }
        return str;
    };

    // text -> wav
    // use the system to create/generate a speech wav file
    service.speak = function() {
        var text = $("#txtSpeak").val();
        service.speakString(text);
    };

    // text -> wav call
    service.speakString = function(text) {
        if ( text && text.length > 0 ) {
            // this is google speech
            // var synth = window.speechSynthesis;
            // if ( synth ) {
            //     var utterance = new SpeechSynthesisUtterance(text);
            //     utterance.lang = 'en-GB';
            //     synth.speak(utterance);
            // } else {
            // }

            // this is our own Mary TTS
            audio.src = globalSvc.getNodeRR("Speech") + 'speech/to-speech/' + encodeURIComponent(service.session) + "/" + encodeURIComponent(text);
            var v = document.getElementsByTagName("audio")[0];
            v.volume = 0.5;
            v.play();

        }
    };

    // interrupt speech playback
    service.stop_talking = function () {
        var v = document.getElementsByTagName("audio")[0];
        v.pause();
        v.currentTime = 0;
    };

    // start a new speech recording on the mic
    service.start_recording = function() {
        recording = true;

        if (!('webkitSpeechRecognition' in window)) {
            console.log('starting kai speech to text');
            // reset the buffers for the new recording
            leftchannel.length = rightchannel.length = 0;
            recordingLength = 0;
        } else {

            var final_transcript = '';

            // google speech to text
            recognition = new webkitSpeechRecognition();
            recognition.continuous = true;
            recognition.interimResults = false;
            recognition.onresult = function(event) {
                for (var i = event.resultIndex; i < event.results.length; ++i) {
                    if (event.results[i].isFinal) {
                        final_transcript += event.results[i][0].transcript;
                    }
                }
            };

            recognition.onend = function(event) {
                recording = false;
                service.uploadDone(final_transcript);
            };

            recognition.start();
        }
    };

    // user clicks stop recording, collect the results
    service.stop_recording = function() {
        // we stop recording
        recording = false;

        if (!('webkitSpeechRecognition' in window)) {
            console.log('stopping kai speech to text');

            // we flat the left and right channels down
            var leftBuffer = service.mergeBuffers(leftchannel, recordingLength);
            var rightBuffer = service.mergeBuffers(rightchannel, recordingLength);
            // we interleave both channels together
            var interleaved = service.interleave(leftBuffer, rightBuffer);

            // we create our wav file
            var buffer = new ArrayBuffer(44 + interleaved.length * 2);
            var view = new DataView(buffer);

            // RIFF chunk descriptor
            service.writeUTFBytes(view, 0, 'RIFF');
            view.setUint32(4, 44 + interleaved.length * 2, true);
            service.writeUTFBytes(view, 8, 'WAVE');
            // FMT sub-chunk
            service.writeUTFBytes(view, 12, 'fmt ');
            view.setUint32(16, 16, true);
            view.setUint16(20, 1, true);
            // stereo (2 channels)
            view.setUint16(22, 2, true);
            view.setUint32(24, sampleRate, true);
            view.setUint32(28, sampleRate * 4, true);
            view.setUint16(32, 4, true);
            view.setUint16(34, 16, true);
            // data sub-chunk
            service.writeUTFBytes(view, 36, 'data');
            view.setUint32(40, interleaved.length * 2, true);

            // write the PCM samples
            var lng = interleaved.length;
            var index = 44;
            var volume = 1;
            for (var i = 0; i < lng; i++) {
                view.setInt16(index, interleaved[i] * (0x7FFF * volume), true);
                index += 2;
            }

            // our final binary blob audio/wav
            var blob = new Blob([view], {type: 'application/octet-stream'});
            service.upload(blob, service.uploadDone);
        } else {
            // google speech to text
            recognition.stop();
        }
    };

    // interleave audio function - used in creating the wav
    service.interleave = function(leftChannel, rightChannel) {
        var length = leftChannel.length + rightChannel.length;
        var result = new Float32Array(length);

        var inputIndex = 0;

        for (var index = 0; index < length;) {
            result[index++] = leftChannel[inputIndex];
            result[index++] = rightChannel[inputIndex];
            inputIndex++;
        }
        return result;
    };

    // merge audio buffers
    service.mergeBuffers = function(channelBuffer, recordingLength) {
        var result = new Float32Array(recordingLength);
        var offset = 0;
        var lng = channelBuffer.length;
        for (var i = 0; i < lng; i++) {
            var buffer = channelBuffer[i];
            result.set(buffer, offset);
            offset += buffer.length;
        }
        return result;
    };

    // helper in creating a proper WAV file header
    service.writeUTFBytes = function(view, offset, string) {
        var lng = string.length;
        for (var i = 0; i < lng; i++) {
            view.setUint8(offset + i, string.charCodeAt(i));
        }
    };

    service.success = function(e) {
        // creates the audio context
        audioContext = window.AudioContext || window.webkitAudioContext;
        context = new audioContext();

        // we query the context sample rate (varies depending on platforms)
        sampleRate = context.sampleRate;

        console.log('succcess');

        // creates a gain node
        volume = context.createGain();

        // creates an audio node from the microphone incoming stream
        audioInput = context.createMediaStreamSource(e);

        // connect the stream to the gain node
        audioInput.connect(volume);

        /* From the spec: This value controls how frequently the audioprocess event is
         dispatched and how many sample-frames need to be processed each call.
         Lower values for buffer size will result in a lower (better) latency.
         Higher values will be necessary to avoid audio breakup and glitches */
        var bufferSize = 2048;
        recorder = context.createScriptProcessor(bufferSize, 2, 2);

        recorder.onaudioprocess = function (e) {
            if (!recording) return;
            var left = e.inputBuffer.getChannelData(0);
            var right = e.inputBuffer.getChannelData(1);
            // we clone the samples
            leftchannel.push(new Float32Array(left));
            rightchannel.push(new Float32Array(right));
            recordingLength += bufferSize;
            //console.log('recording');
        };

        // we connect the recorder
        volume.connect(recorder);
        recorder.connect(context.destination);
    };

    // browser speech feature detection
    if (!navigator.getUserMedia) {
        navigator.getUserMedia = navigator.getUserMedia || navigator.webkitGetUserMedia || navigator.mozGetUserMedia || navigator.msGetUserMedia;
    }
    if (navigator.getUserMedia) {
        navigator.getUserMedia({audio: true}, service.success, function (e) {
            globalSvc.error_message('Error capturing audio.', false);
        });
    } else {
        globalSvc.error_message('getUserMedia not supported in this browser.', false);
    }

    // setup data callback to controller
    service.setupDataFn = null;

    // whoever wants a callback better put their name here
    // two parameters - 1: super query string to use, 2: callback for internal setup
    service.doSearchCallback = null;

    service.setup = function(str) {
        if ( service.setupDataFn ) {
            service.setupDataFn(str);
        }
    };


})

.controller('KeywordSearchController', function($scope, keywordSearchSvc) {

    $scope.text = '';
    $scope.command = '';
    $scope.commandTime = 0;
    $scope.recording = false;

    // start recording
    $scope.startButton = function (event) {
        if ( !$scope.recording ) {
            $scope.recording = true;
            start_img.src = '/search/images/mic-animate.gif';
            keywordSearchSvc.start_recording();
        } else {
            $scope.recording = false;
            start_img.src = '/search/images/mic.gif';
            keywordSearchSvc.stop_recording();
        }
    };

    $scope.setText = function(str) {
        if (str) {
            $scope.text = str;
        }
    };

    // ui clicks search
    $scope.search = function() {
        if ( $scope.text != '' && keywordSearchSvc ) {
            keywordSearchSvc.doSearchCallback( $scope.text, $scope.searchDone );
        }
    };

    // callback
    $scope.searchDone = function(data) {
        if ( data ) {
        }
    };

    // setup callback
    keywordSearchSvc.setupDataFn = $scope.setText;
})

.directive('keywordSearch', function() {
    return {
        templateUrl: 'views/widgets/keyword_search_widget.html'
    };
});

