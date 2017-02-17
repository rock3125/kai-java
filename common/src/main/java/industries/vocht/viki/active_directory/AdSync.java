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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.CommunicationException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;
import javax.net.ssl.SSLHandshakeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This provides the basic functionalities to retrieve the objects and their
 * parameters from the Active Directory.
 * 
 * @author G.V.Sekhar
 * @version $Revision: 1.0 $
 * 
 */
public class AdSync {

	private final static Logger logger = LoggerFactory.getLogger(Main.class);
	
	/**
	 * Field parametersHashTable.
	 */
	Hashtable<String, String> parametersHashTable = new Hashtable<>();
	/**
	 * Field ctx.
	 */
	InitialLdapContext ctx = null;
	/**
	 * Field parameters.
	 */
	AdParameters parameters = null;
	/**
	 * Field searchFilter.
	 */
	String searchFilter = null;
	/**
	 * Field searchctls.
	 */
	SearchControls searchctls = null;

	/**
	 * Constructor for AdSynch.
	 * 
	 * @param parameters
	 *            Parameters
	 */
	public AdSync(AdParameters parameters)
	{
		this.parameters = parameters;
	}

	/**
	 * Method processRequest.
	 * 
	 * @return List<User>
	 * @throws ArrayIndexOutOfBoundsException
	 * @throws SSLHandshakeException
	 * @throws CommunicationException
	 * @throws NamingException
	 */
	public List<AdUser> processRequest() throws ArrayIndexOutOfBoundsException, IOException, NamingException {
		setConnectionParameters();
		setSearchParameters();
		return getUsers();
	}

	/**
	 * Method setConnectionParameters.
	 * 
	 * @throws NamingException
	 * @throws ArrayIndexOutOfBoundsException
	 * @throws SSLHandshakeException
	 * @throws CommunicationException
	 */
	private void setConnectionParameters() throws NamingException, ArrayIndexOutOfBoundsException, SSLHandshakeException {
		logger.debug(">> setConnectionParameters()");
		
		parametersHashTable.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
		parametersHashTable.put("java.naming.security.authentication", "simple");
		parametersHashTable.put("com.sun.jndi.ldap.connect.pool", "true");
		parametersHashTable.put("com.sun.jndi.ldap.connect.pool.timeout", "60000");
		parametersHashTable.put("java.naming.referral", "follow");

		StringBuilder tempStringBuffer = new StringBuilder();
		// If SSL is enabled, set the protocol to ldaps else ldap only.
		if (parameters.getSSLOn()) {
			logger.debug("SSL is ON");
			tempStringBuffer.append("ldaps://");
			parametersHashTable.put("java.naming.security.protocol", "ssl");
		} else {
			logger.debug("SSL is OFF");
			tempStringBuffer.append("ldap://");
		}

		tempStringBuffer.append(parameters.getServerIp()).append(":").append(parameters.getPortNumber()).append("/");

		String principalValue = parameters.getUserName() + "@" + parameters.getDomainName();

		parametersHashTable.put("java.naming.provider.url", tempStringBuffer.toString());
		parametersHashTable.put("java.naming.security.principal", principalValue);
		parametersHashTable.put("java.naming.security.credentials", parameters.getPassword());

		ctx = new InitialLdapContext(parametersHashTable, null);
		
		logger.debug("<< setConnectionParameters()");
	}

	/**
	 * Method setSearchParameters.
	 */
	private void setSearchParameters() {
		logger.debug(">> setSearchParameters()");
		
		// Search controls and setting up the returning attributes
		searchctls = new SearchControls();
		searchctls.setReturningAttributes(parameters.getRetrieveAttributes());

		// Set the search scope. You can change this as according your
		// structure.
		// Remember this value has effect on performance.
		// Example: setting the scope to Subtree.
		searchctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

		// Set the search filter. Change as according by changing the values in
		// properties class.
		searchFilter = "(&(objectClass=" + AdProperties.FILTER_OBJECT_CLASS + ")(objectCategory=" +
                        AdProperties.FILTER_OBJECT_CATEGORY + "))";
		
		logger.debug("<< setSearchParameters");
	}

	/**
	 * If multiple search queries are added, then process each one.
	 * 
	 * Method getUsers.
	 * 
	 * @return List<User>
	 * @throws NamingException
	 */
	private List<AdUser> getUsers() throws NamingException, IOException {
		logger.debug(">> getUsers()");

		List<AdUser> users = new ArrayList<>();
        if ( parameters.getBasePath() != null ) {
            users.addAll(getUsers(parameters.getBasePath()));
        }

		logger.debug("<< getUsers()");
		return users;
	}

    /**
     * Retrieve the input attributes from the Active Directory for the given basePath
     * @param basePath a path like  DC=[domain-name],DC=co,DC=nz
     * @return the list of user objects found
     * @throws NamingException
     */
	private List<AdUser> getUsers(String basePath) throws NamingException, IOException {
        logger.debug(">> getUsers()");

        int pageSize = 100; // 5 entries per page

        // setup opaque cookie pagination
        byte[] cookie;
        ctx.setRequestControls(new Control[]{ new PagedResultsControl(pageSize, Control.CRITICAL) });
		List<AdUser> users = new ArrayList<>();

        do {
            AdUser user;
            NamingEnumeration<SearchResult> results = ctx.search(basePath, searchFilter, searchctls);
            while (results != null && results.hasMoreElements()) {
                user = new AdUser();
                SearchResult searchResult = results.next();
                Attributes attrs = searchResult.getAttributes();
                if (attrs != null && attrs.size() != 0) {

                    Attribute attribute;

                    String[] retrieveAttributes = parameters.getRetrieveAttributes();
                    String[] attributesValues = new String[retrieveAttributes.length];
                    for (int i = 0; i < retrieveAttributes.length; i++)  {
                        attribute = attrs.get(retrieveAttributes[i]);
                        if (attribute != null && attribute.get() != null) {
                            if (!isNullOrEmpty(attribute.get().toString())) {
                                attributesValues[i] = attribute.get().toString();
                            }
                        }
                    }
                    user.setAttributeValues(attributesValues);
                }
                users.add(user);
            }

            // get opaque cookie to see if there are more pages to get
            cookie = null;
            Control[] controls = ctx.getResponseControls();
            if (controls != null) {
                for ( Control control : controls ) {
                    if (control instanceof PagedResultsResponseControl) {
                        PagedResultsResponseControl prrc = (PagedResultsResponseControl)control;
                        cookie = prrc.getCookie();
                    }
                }
            }

            // setup the next page
            ctx.setRequestControls(new Control[] { new PagedResultsControl(pageSize, cookie, Control.CRITICAL) });

        } while (cookie != null);

        logger.debug("<< getUsers()");
		return users;
	}

    /**
     * is this string null or trim() empty?
     * @param str the string to check
     * @return true if its null or empty
     */
	private static boolean isNullOrEmpty(String str) {
		return str == null || str.trim().length() == 0;
	}

}

