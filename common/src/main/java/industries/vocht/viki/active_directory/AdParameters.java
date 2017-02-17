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

/**
 * @author G.V.Sekhar
 * @version $Revision: 1.0 $
 * 
 * Defines all the connection parameters for Active Directory. You can
 * add extra parameters, if any required.
 * 
 */
public class AdParameters {

	private final static Logger logger = LoggerFactory.getLogger(Main.class);

	// ip address of the ad controller
	private String serverIp;
	// LDAP port 389, or 686 (defaults)
	private int portNumber = 389;
	// username to login with (domain account)
	private String userName;
	// password of this userName account
	private String password;
	// name of the domain
	private String domainName;
	// use SSL for comms? (check port is 686)
	private boolean SSLOn = false;
	// the base path of the query, a FQDN like DC=domain,DC=com
	private String basePath;
	// default attributes to retrieve from AD
	private String[] retrieveAttributes = new String[] { "sAMACCOUNTNAME", "mail", "userAccountControl" };

	/**
	 * @return the server ip address
     */
	public String getServerIp() {
		return serverIp;
	}

	/**
	 * set the server's ip address
	 * @param serverIp the ip address
     */
	public void setServerIp(String serverIp) {
		logger.debug("server IP::" + serverIp);
		this.serverIp = serverIp;
	}

	/**
	 * Method getPortNumber.
	 * 
	 * @return String
	 */
	public int getPortNumber() {
		return portNumber;
	}

	/**
	 * Method setPortNumber.
	 * 
	 * @param portNumber
	 *            String
	 */
	public void setPortNumber(int portNumber) {
		logger.debug("portNumber::" + portNumber);
		this.portNumber = portNumber;
	}

	/**
	 * Method getUserName.
	 * 
	 * @return String
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * Method setUserName.
	 * 
	 * @param userName String
	 */
	public void setUserName(String userName) {
		logger.debug("userName::" + userName);
		this.userName = userName;
	}

	/**
	 * Method getPassword.
	 * 
	 * @return String
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Method setPassword.
	 * 
	 * @param password String
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Method getDomainName.
	 * 
	 * @return String
	 */
	public String getDomainName() {
		return domainName;
	}

	/**
	 * Method setDomainName.
	 * 
	 * @param domainName String
	 */
	public void setDomainName(String domainName) {
		logger.debug("domainName::" + domainName);
		this.domainName = domainName;
	}

	/**
	 * Method getSSLOn.
	 * 
	 * @return String
	 */
	public boolean getSSLOn()
	{
		return SSLOn;
	}

	/**
	 * Method setSSLOn.
	 * 
	 * @param sSLOn String
	 */
	public void setSSLOn(boolean sSLOn) {
		logger.debug("sSLOn::" + sSLOn);
		this.SSLOn = sSLOn;
	}

	/**
	 * Method getBasePath.
	 * 
	 * @return String
	 */
	public String getBasePath() {
		return basePath;
	}

	/**
	 * Method setBasePath.
	 * 
	 * @param basePath String
	 */
	public void setBasePath(String basePath) {
        logger.debug("basePath::" + basePath);
		this.basePath = basePath;
	}

	/**
	 * Method getRetrieveAttributes.
	 * 
	 * @return String[]
	 */
	public String[] getRetrieveAttributes()
	{
		return retrieveAttributes;
	}

	/**
	 * Method setRetrieveAttributes.
	 * 
	 * @param retrieveAttributes String[]
	 */
	public void setRetrieveAttributes(String[] retrieveAttributes) {
		if ( logger.isDebugEnabled() ) {
			StringBuilder sb = new StringBuilder();
			if ( retrieveAttributes != null ) {
				for (String str : retrieveAttributes) {
					sb.append(str).append(",");
				}
			} else {
				sb.append("null");
			}
			logger.debug("retrieveAttributes::" + sb.toString());
		}
		this.retrieveAttributes = retrieveAttributes;
	}

}

