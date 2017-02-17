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

package industries.vocht.viki.active_directory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import javax.naming.NamingException;
import javax.net.ssl.SSLHandshakeException;

/**
 * Sample calling for Active Directory Synch
 * 
 * @author G.V.Sekhar
 * @version $Revision: 1.0 $
 */
public class Main {

	private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * test the ADSync class on a real domain
     * @param args not used
     */
	public static void main(String[] args) {

		AdParameters p = new AdParameters();

		p.setDomainName("domain2008.fritz.box");
        p.setUserName("Administrator");
		p.setPassword("Password1");
        p.setServerIp("10.17.1.120");
		p.setBasePath("DC=domain2008,DC=fritz,DC=box");
		

		AdSync ad = new AdSync(p);
		try {
            int validUserCount = 0;
			List<AdUser> userList = ad.processRequest();
			for (AdUser user : userList) {
				String[] attributeSet = user.getAttributeValues();
                if ( attributeSet != null ) {
                    String attrStr = "";
                    for (String string : attributeSet) {
                        if ( attrStr.length() > 0 ) {
                            attrStr = attrStr + "::";
                        }
                        attrStr = attrStr + string;
                    }
                    logger.info(attrStr);
                    validUserCount = validUserCount + 1;
                }
			}
            logger.info("retrieved " + validUserCount + " valid users");

		} catch (IOException | NamingException ex) {
			logger.error(ex.getMessage(), ex);
		}

	}


}