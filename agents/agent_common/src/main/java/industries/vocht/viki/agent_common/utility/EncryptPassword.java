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

package industries.vocht.viki.agent_common.utility;

import industries.vocht.viki.agent_common.AgentAESEncryption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created by peter on 16/06/16.
 *
 * create an encrypted password using AES
 *
 */
@Component
public class EncryptPassword {

    @Autowired
    private AgentAESEncryption aes;

    // example main - won't work without the application context proper
    public static void main( String[] args ) throws IOException {
        if ( args.length == 1 ) {
            ApplicationContext context = new ClassPathXmlApplicationContext("/file_agent/application-context.xml");
            EncryptPassword ep = context.getBean(EncryptPassword.class);
            System.out.println(ep.encrypt(args[0]));
        }
    }

    public EncryptPassword() {
    }

    public String encrypt( String plainText ) throws IOException {
        return aes.encrypt(plainText);
    }

}

