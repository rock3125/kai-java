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

package industries.vocht.viki.webserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.net.URI;

/*
 * Created by peter on 29/11/14.
 *
 * setup the collector host
 * setup log4j2:   -Dlog4j.configurationFile=log4j2.xml
 *
 */
public class Main
{
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    // the fixed filename of the properties file
    private final static String log4jPath = "/opt/kai/webserver/conf/log4j2.xml";

    public static void main( String[] args ) throws Exception {
        LoggerContext l4j2context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
        l4j2context.setConfigLocation(new URI("file://" + log4jPath));
        logger.info("starting Kai-jetty webserver");

        new ClassPathXmlApplicationContext("/webserver/jetty-context.xml");
    }


}

