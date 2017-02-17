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

package industries.vocht.viki.utility;

import industries.vocht.viki.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by peter on 29/11/15.
 *
 * java mail wrapper
 *
 */
@Component
public class Mailer {

    private final static Logger logger= LoggerFactory.getLogger( Mailer.class );

    @Autowired
    private AESEncryption aes;

    // credentials and from and smtp details
    @Value("${smtp.username:peter}")
    private String username;
    @Value("${smtp.password:not-set}")
    private String password;
    @Value("${smtp.server:localhost}")
    private String smtpServer;
    @Value("${smtp.from:no-reply@vocht.industries}")
    private String fromAddress;
    @Value("${smtp.port:465}")
    private int smtpPort;
    @Value("${smtp.useSSL:true}")
    private boolean ssl;

    public Mailer() throws Exception {
    }

    /**
     * decrypt any encrypted values before sending an email
     * @throws ApplicationException
     */
    private void checkEncryption() throws ApplicationException {
        if ( this.username.startsWith("aes:") ) {
            try {
                this.username = aes.decrypt(this.username.substring(4));
            } catch (IOException ex) {
                throw new ApplicationException("decryption failed " + ex.getMessage());
            }
        }
        if ( this.password.startsWith("aes:") ) {
            try {
                this.password = aes.decrypt(this.password.substring(4));
            } catch (IOException ex) {
                throw new ApplicationException("decryption failed " + ex.getMessage());
            }
        }
        if ( this.smtpServer.startsWith("aes:") ) {
            try {
                this.smtpServer = aes.decrypt(this.smtpServer.substring(4));
            } catch (IOException ex) {
                throw new ApplicationException("decryption failed " + ex.getMessage());
            }
        }
    }

    // send an email - return true on success
    // on error (false) logs a message in the logs and just return
    public boolean email( String toEmailAddress, String subject, String messageContent ) throws ApplicationException {

        checkEncryption();

        if ( toEmailAddress != null && subject != null && messageContent != null ) {

            Properties properties = new Properties();
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", ssl ? "true" : "false");
            properties.put("mail.smtp.starttls.required", ssl ? "true" : "false");
            properties.put("mail.transport.protocol", "smtp");

            Session session = Session.getInstance(properties);
            Transport transport = null;

            try {
                // Create transport
                transport = session.getTransport();

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(fromAddress));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmailAddress));
                message.setSubject(subject);
                message.setText(messageContent);

                transport.connect(smtpServer, username, password);
                transport.sendMessage( message, message.getAllRecipients() );

                return true;

            } catch (MessagingException ex) {
                logger.error("email", ex);
                return false;
            } finally {
                if ( transport != null ) {
                    try {
                        transport.close();
                    } catch (Exception ex) {
                        logger.error("close transport", ex.getMessage());
                    }
                }
            }
        }
        logger.error("email: invalid parameter(s)");
        return false;
    }

}

