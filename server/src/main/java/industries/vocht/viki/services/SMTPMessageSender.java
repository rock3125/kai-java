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

package industries.vocht.viki.services;

import industries.vocht.viki.ApplicationException;
import industries.vocht.viki.model.user.User;
import industries.vocht.viki.utility.Mailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;

/**
 * Created by peter on 30/03/16.
 *
 * deal with the various emails that need to be send
 *
 */
public class SMTPMessageSender {

    private final Logger logger = LoggerFactory.getLogger(SMTPMessageSender.class);

    private Mailer mailer;
    private UserService userService;

    public SMTPMessageSender(UserService userService, Mailer mailer) {
        this.userService = userService;
        this.mailer = mailer;
    }

    /**
     * send an email to the user for their account activation
     * @param email the email address to send to
     * @param activationUrl the url the user needs to click to activate their account
     * @throws ApplicationException
     */
    public void sendActivationMessage( String email, String activationUrl ) throws ApplicationException {

        if ( email == null || email.length() == 0 ) {
            throw new ApplicationException("sendActivationMessage: invalid email address");
        }

        // setup the security for resetting a password
        UUID activationID = userService.createAccountActivation(email);

        StringBuilder sb = new StringBuilder();
        sb.append("Welcome to Vocht Industries Knowledge made Interactive,\n\n");
        sb.append("Thank you for creating an account with us.  Your log-in is your email address, ");
        sb.append(email).append(".\n");
        sb.append("Please click the link below to complete the registration process.\n\n");
        try {
            sb.append(activationUrl).append("?email=")
                    .append(URLEncoder.encode(email, "UTF-8")).append("&activationid=").append(activationID).append("\n\n");
        } catch (UnsupportedEncodingException ex) {
            logger.error("sendActivationMessage", ex);
            throw new ApplicationException("Account creation email send failed.\nEncoding not supported.");
        }
        if ( !mailer.email( email, "VIKI Account Created", sb.toString()) ) {
            throw new ApplicationException("Account creation email send failed.\nPlease try again later.");
        }
    }


    /**
     * send a password reset email message
     * @param email the email to send to
     * @param resetPasswordUrl the url of the reset link
     * @param ipAddress the ip address of the request-er
     * @throws IOException
     * @throws ApplicationException
     */
    public void sendEmailPasswordResetMessage( String email, String resetPasswordUrl , String ipAddress ) throws IOException, ApplicationException {

        if ( email == null || email.length() == 0 ) {
            throw new ApplicationException("sendEmailPasswordResetMessage: invalid email address");
        }

        // check this email is one of our users
        User user = userService.getUserByEmail(email, ipAddress);
        if ( user == null ) {
            throw new ApplicationException("unknown user, email address not registered");
        }

        // setup the security for resetting a password
        UUID resetID = userService.resetPasswordRequest(email);

        StringBuilder sb = new StringBuilder();
        sb.append("Hello from Vocht Industries Knowledge made Interactive,\n\n");
        sb.append("This is your account reset password request.  Your log-in is always your email address, ");
        sb.append(email).append(".\n");
        sb.append("Please click the link below to start the password reset process.\n\n");
        sb.append(resetPasswordUrl)
                .append("?email=")
                .append(URLEncoder.encode(email, "UTF-8"))
                .append("&resetid=")
                .append(resetID.toString())
                .append("\n\n");

        if ( !mailer.email( email, "VIKI Account Password reset request", sb.toString()) ) {
            throw new ApplicationException("Account password reset email send failed.\nPlease try again later.");
        }
    }


}

