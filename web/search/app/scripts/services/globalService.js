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
 * searchApp Module
 *
 * Description global service, application wide related functionality
 */
angular.module('searchApp').service('globalSvc', function ($location, $q, $rootScope, $cookies, $http, $timeout) {

    var service = this; // accessor
    var session = null; // test session reload

    var infrastructureMap = {}; // what the system's infrastructure looks like

    // setup array contains value
    Array.prototype.contains = function (obj) {
        var i = this.length;
        while (i--) {
            if (this[i] == obj) {
                return true;
            }
        }
        return false;
    };

    String.prototype.startsWith = function (searchString) {
        return this.substr(0, searchString.length) === searchString;
    };

    String.prototype.capitalizeFirstLetter = function () {
        return this.charAt(0).toUpperCase() + this.slice(1);
    };

    String.prototype.integerPrettyPrint = function () {
        var parts = [];
        if (this.length > 3) {

            var str = this;
            while (str.length >= 3) {
                parts.push(str.substr(str.length - 3, str.length));
                str = str.substr(0, str.length - 3);
            }
            if (str.length > 0) {
                parts.push(str);
            }

            var str = "";
            for (var i = parts.length - 1; i >= 0; i--) {
                if (str.length > 0) {
                    str = str + ",";
                }
                str = str + parts[i];
            }
            return str;

        } else {
            return this;
        }
    };

    /////////////////////////////////////////////////////////////////////////////

    // get a value from local-storage
    service.getValue = function (name) {
        if (typeof(Storage) !== "undefined") {
            if (name) {
                return localStorage.getItem(name);
            } else {
                return null;
            }
        } else {
            alert('Sorry! No Web Storage support...\nPlease upgrade your browser');
        }
    };

    // set a value into local-storage
    service.setValue = function (name, value) {
        if (typeof(Storage) !== "undefined") {
            if (name) {
                if (value == null) {
                    localStorage.removeItem(name);
                } else {
                    return localStorage.setItem(name, value);
                }
            } else {
                return null;
            }
        } else {
            alert('Sorry! No Web Storage support...\nPlease upgrade your browser');
        }
    };

    // set a value into local-storage
    service.getObject = function (name) {
        if (typeof(Storage) !== "undefined") {
            if (name) {
                return JSON.parse(localStorage.getItem(name));
            } else {
                return null;
            }
        } else {
            alert('Sorry! No Web Storage support...\nPlease upgrade your browser');
        }
    };

    // set a value into local-storage
    service.setObject = function (name, obj) {
        if (typeof(Storage) !== "undefined") {
            if (name) {
                if (obj == null) {
                    localStorage.removeItem(name);
                } else {
                    return localStorage.setItem(name, JSON.stringify(obj));
                }
            } else {
                return null;
            }
        } else {
            alert('Sorry! No Web Storage support...\nPlease upgrade your browser');
        }
    };

    // get the session id
    service.getSession = function (sessionCallback, usercallback) {
        console.log("service.getSession()");
        var s = service.getValue("vikiSearchSession");
        var user2 = service.getObject("user");
        if (s && user2) {  // fast callback
            session = s; // set local
            service.setupInfrastructure(user2);
            if (usercallback) {
                usercallback(user2);
            }
            if ( sessionCallback ) {
                sessionCallback(s);
            }
        } else if (s) { // reload session entities
            // reload user object - since we don't seem to have it in local cache
            $http.get(securityServiceEntry + "security/user/" + encodeURIComponent(s)).then(
                function success(response) {
                    if (response && response.data) {
                        //console.log(JSON.stringify(response.data));
                        console.log("re-get session: user object refreshed");
                        var user3 = response.data;
                        service.setValue("user", JSON.stringify(user3)); // set user obj in storage
                        service.setupInfrastructure(user3);
                        $("#signinMenu").hide();
                        $("#signoutMenu").show();
                        // build a new infrastructure map
                        if (usercallback) {
                            usercallback(user3);
                        }
                        if (sessionCallback) {
                            sessionCallback(s);
                        }
                    }
                }, function error(response) {
                    console.log("session invalid: object cache cleared");
                    infrastructureMap = {}; // wipe
                    session = null; // wipe local
                    s = null;
                    service.setSession(null); // remove the session
                    service.setValue("user", null); // remove user obj
                    service.error(response); // sound the alarm
                    if (sessionCallback) {
                        sessionCallback(null);
                    }
                });
        } else { // fail callback
            service.setSession(null); // remove the session
            service.setValue("user", null); // remove user obj
            if (sessionCallback) {
                sessionCallback(null);
            }
        }
    };

    // setup the infrastructure object from the user
    service.setupInfrastructure = function(user) {
        infrastructureMap = {};
        if ( user && user.cluster_address_list && user.cluster_address_list.length > 0 ) {
            $.each(user.cluster_address_list, function(i, address) { // type, host, port
                if ( infrastructureMap[address.type] == undefined ) {
                    infrastructureMap[address.type] = {};
                    infrastructureMap[address.type].list = [];
                    infrastructureMap[address.type].counter = 0;
                }
                infrastructureMap[address.type].list.push( protocol + address.host + ":" + address.port + "/viki/" )
            });
        }
    };

    // set the session id
    service.setSession = function (value) {
        service.setValue("vikiSearchSession", value);
        if (value == null) {
            // clear other objects
            service.setValue("user", null);
        }
    };

    /////////////////////////////////////////////////////////////////////////////

    // validate the correctness of an email address
    service.validateEmail = function (email) {
        if (email && email.length && email.length > 3) {
            var re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
            return re.test(email);
        } else {
            return false;
        }
    };

    // get the text of an error message from the json response object
    service.getErrorTextFromResponse = function (jsonResponse) {
        if (jsonResponse && jsonResponse.data) {
            if (jsonResponse.data.error) {
                return jsonResponse.data.error;
            } else {
                return jsonResponse.status + ", " + jsonResponse.statusText;
            }
        } else if (jsonResponse && jsonResponse.length) { // plain text
            return jsonResponse;
        } else {
            return "no response from server";
        }
    };

    // display and log an error message
    service.error = function (jsonResponse, keepMessageOpen) {
        var errorStr = service.getErrorTextFromResponse(jsonResponse);
        $("#errorMessageMessage").html(errorStr);
        $("#errorMessage").show();
        // hide the message after 5 secs
        if (!keepMessageOpen) {
            $timeout(function () {
                $("#errorMessage").hide();
            }, 5000);
        }
    };

    // display and log an error message
    service.error_message = function (errorStr, keepMessageOpen) {
        $("#errorMessageMessage").html(errorStr);
        $("#errorMessage").show();
        // hide the message after 5 secs
        if (!keepMessageOpen) {
            $timeout(function () {
                $("#errorMessage").hide();
            }, 5000);
        }
    };

    // display and log an error message
    service.info = function (jsonResponse, keepMessageOpen) {
        var infoStr = service.getErrorTextFromResponse(jsonResponse);
        $("#infoMessageMessage").html(infoStr);
        $("#infoMessage").show();
        // hide the message after 5 secs
        if (!keepMessageOpen) {
            $timeout(function () {
                $("#infoMessage").hide();
            }, 5000);
        }
    };

    // display and log an error message
    service.info_message = function (infoStr, keepMessageOpen) {
        $("#infoMessageMessage").html(infoStr);
        $("#infoMessage").show();
        // hide the message after 5 secs
        if ( !keepMessageOpen ) {
            $timeout(function () {
                $("#infoMessage").hide();
            }, 5000);
        }
    };

    // initial access the security system for login and getting infrastructure
    service.securityServiceEntry = function() {
        return securityServiceEntry;
    };

    // helper for infrastructure access
    service.getNodeRR = function( type ) {
        if ( infrastructureMap && infrastructureMap[type] &&
            infrastructureMap[type].list.length > 0 ) {
            var list = infrastructureMap[type].list;
            var counter = infrastructureMap[type].counter;
            var serviceUrl = list[counter];
            counter = counter + 1;
            if (counter >= list.length) {
                counter = 0;
            }
            infrastructureMap[type].counter = counter;
            return serviceUrl;
        }
        console.log("WARN: infrastructure map not setup, returning default /viki");
        return "/viki/";
    };

});

