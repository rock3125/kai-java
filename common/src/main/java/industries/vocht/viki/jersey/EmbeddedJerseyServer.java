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

package industries.vocht.viki.jersey;

import industries.vocht.viki.VikiException;
import industries.vocht.viki.utility.AESEncryption;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.DispatcherType;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.EnumSet;

/**
 * Created by peter on 20/03/16.
 *
 * common configuration and setup for my web servers
 *
 */
public class EmbeddedJerseyServer {

    private final Logger logger = LoggerFactory.getLogger(EmbeddedJerseyServer.class);

    private AESEncryption aesEncryption;

    private int port; // server port

    private String webBasePath; // path of web pages

    private boolean useMultipartUpload;

    private boolean staticWeb; // is this a static web-site or a service?

    private String keystoreFile;

    private String keyStoreCertName;

    private String keyStorePassword;

    private String keyPassword;

    private String description;

    private String springContext;

    private String serviceNameSpace;

    private TrustManager[] trustAllCerts;

    private boolean ssl; // using ssl?
    private boolean usingApi; // using an api?  or just static pages
    private boolean usingSwagger; // using swagger?

    public EmbeddedJerseyServer() {
        usingApi = false;
        usingSwagger = false;

        // overwrite default trust checks
        trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {  }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {  }
                }
        };
    }


    public void init() throws Exception {

        // overwrite default security check mechanism to allow connections from localhost
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // create the jetty server
        Server server = setup();
        if ( staticWeb ) {
            setupStaticWeb(server);
        } else {
            ServletContextHandler context = setupAPI(server);
            setupSwagger( context, description );
        }
        // start the server and wait for it to finish execution
        startServer(server);
    }

    // determine the usage of ssl from the port number, anything ending in 443 is SSL
    private boolean useSSL( int port ) {
        return (port == 443) || ((port % 1_000) == 443) || ((port % 10_000) == 443);
    }

    /**
     * Setup the basic server with optional ssl, with an optionally encrypted AES key
     * @return the server with its connector modified for ssl (optional)
     * @throws IOException
     */
    protected Server setup() throws IOException, VikiException {

        this.ssl = useSSL(port);
        Server server = new Server( port );

        // setup ssl
        if ( ssl ) {
            HttpConfiguration https = new HttpConfiguration();
            https.addCustomizer(new SecureRequestCustomizer());
            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStorePath(keystoreFile);

            // check the key - is IT encrypted with aes?
            if ( keyStorePassword.toLowerCase().startsWith("aes:") ) {
                sslContextFactory.setKeyStorePassword(aesEncryption.decrypt(keyStorePassword.substring(4).trim()));
            } else {
                sslContextFactory.setKeyStorePassword(keyStorePassword);
            }

            // check the key - is IT encrypted with aes?
            if ( keyPassword.toLowerCase().startsWith("aes:") ) {
                sslContextFactory.setKeyManagerPassword(aesEncryption.decrypt(keyPassword.substring(4).trim()));
            } else {
                sslContextFactory.setKeyManagerPassword(keyPassword);
            }

            if ( keyStoreCertName != null && keyStoreCertName.length() > 0 ) {
                sslContextFactory.setCertAlias(keyStoreCertName);
            }

            ServerConnector sslConnector = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(https));
            sslConnector.setPort(port);
            server.setConnectors(new Connector[]{sslConnector});
        }
        return server;
    }

    /**
     * this function creates the jersey context for a given server and sets the package scanner to the fixed
     * SERVICES_NAMESPACE package for all projects.  The spring-context supplied will be booted by jersey and
     * the project this server represents when the server starts
     * @param server the server previously created
     * @return the context for the server
     */
    protected ServletContextHandler setupAPI(Server server ) {

        this.usingApi = true;

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        if ( springContext != null && springContext.length() > 0 ) {
            context.setInitParameter("contextConfigLocation", "classpath:" + springContext); // spring init file location
        }
        context.addEventListener(new ContextLoaderListener()); // add spring listener

        if ( staticWeb ) {
            // setup a separate static site - default site on / (relative to resource-base
            ServletHolder staticServlet = context.addServlet(DefaultServlet.class, "/*");
            staticServlet.setInitParameter("pathInfoOnly", "true");
            context.setResourceBase(webBasePath); // location of the static web data
            logger.info("static web base @ " + webBasePath);
        } else {
            context.setContextPath("/");
            context.setResourceBase(webBasePath); // location of the static web data
            context.addFilter(CORSResponseFilter.class, "/viki/*", EnumSet.of(DispatcherType.REQUEST));
            server.setHandler(context);

            ServletHolder jerseyServlet = context.addServlet(org.glassfish.jersey.servlet.ServletContainer.class, "/*");
            jerseyServlet.setInitOrder(1);

            // This parameter tells the Jersey Servlet which of your REST resources to load.
            // this parameter is set to load a "packages" level system - the name-space following it indicating which packages to look for
            // NB. the services always need to live in the services folder of the packages
            jerseyServlet.setInitParameter("jersey.config.server.provider.packages", serviceNameSpace);

            // register the application configuration to overwrite resources for file upload
            if (useMultipartUpload) {
                jerseyServlet.setInitParameter("javax.ws.rs.Application", "industries.vocht.viki.jersey.AppMultiPartFeature");
            }

            // setup a separate swagger site
            ServletHolder staticServlet = context.addServlet(DefaultServlet.class, "/api/*");
            staticServlet.setInitParameter("pathInfoOnly", "true");
            context.setResourceBase(webBasePath); // location of the static web data
            logger.info("static web base @ " + webBasePath + "/api/");
        }
        return context;
    }

    /**
     * this function creates the jersey context for a given server and sets the package scanner to the fixed
     * SERVICES_NAMESPACE package for all projects.  The spring-context supplied will be booted by jersey and
     * the project this server represents when the server starts
     * @param server the server previously created
     * @return the context for the server
     */
    private ServletContextHandler setupStaticWeb(Server server) {

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.setResourceBase(webBasePath); // location of the static web data
        server.setHandler(context);
        logger.info("static web base @ " + webBasePath);

        // setup a separate static site - default site on / (relative to resource-base
        ServletHolder staticServlet = context.addServlet(DefaultServlet.class, "/*");
        staticServlet.setInitParameter("pathInfoOnly", "true");

        return context;
    }

    /**
     * setup the swagger api - which dynamically generates test fixtures and documentation for the previously generated
     * context
     * @param context the previously generated context
     * @param apiProjectName a pretty name for the api
     */
    protected void setupSwagger(ServletContextHandler context, String apiProjectName)
            throws VikiException, IOException {
        // swagger.json
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setVersion("1.0.0");
        beanConfig.setResourcePackage(serviceNameSpace);
        beanConfig.setScan(true);
        beanConfig.setBasePath("/");
        beanConfig.setDescription(apiProjectName);
        beanConfig.setTitle(apiProjectName);

        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.packages(serviceNameSpace, ApiListingResource.class.getPackage().getName());
        resourceConfig.register(MultiPartFeature.class);

        ServletContainer servletContainer = new ServletContainer(resourceConfig);
        ServletHolder entityBrowser = new ServletHolder(servletContainer);
        context.addServlet(entityBrowser, "/api-doc/*");

        this.usingSwagger = true; // swagger has been setup
    }

    /**
     * start the server and wait forever (pass execution to spring application context)
     * as previously setup
     * @param server the server
     */
    protected void startServer(Server server) throws Exception {

        if ( ssl ) {
            logger.info("starting application on https://localhost:" + port + "/");
            if ( usingApi ) {
                logger.info("services on https://localhost:" + port + "/");
                if ( usingSwagger ) {
                    logger.info("swagger JSON on https://localhost:" + port + "/api-doc/swagger.json");
                    logger.info("swagger UI on https://localhost:" + port + "/api/swagger/");
                }
            }
        } else {
            logger.info("starting application on http://localhost:" + port + "/");
            if ( usingApi ) {
                logger.info("services on http://localhost:" + port + "/");
                if ( usingSwagger ) {
                    logger.info("swagger JSON on http://localhost:" + port + "/api-doc/swagger.json");
                    logger.info("swagger UI on http://localhost:" + port + "/api/swagger/");
                }
            }
        }

        server.start();
        //server.join();
    }

    public AESEncryption getAesEncryption() {
        return aesEncryption;
    }

    public void setAesEncryption(AESEncryption aesEncryption) {
        this.aesEncryption = aesEncryption;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getWebBasePath() {
        return webBasePath;
    }

    public void setWebBasePath(String webBasePath) {
        this.webBasePath = webBasePath;
    }

    public boolean isUseMultipartUpload() {
        return useMultipartUpload;
    }

    public void setUseMultipartUpload(boolean useMultipartUpload) {
        this.useMultipartUpload = useMultipartUpload;
    }

    public boolean isStaticWeb() {
        return staticWeb;
    }

    public void setStaticWeb(boolean staticWeb) {
        this.staticWeb = staticWeb;
    }

    public String getKeystoreFile() {
        return keystoreFile;
    }

    public void setKeystoreFile(String keystoreFile) {
        this.keystoreFile = keystoreFile;
    }

    public String getKeyStoreCertName() {
        return keyStoreCertName;
    }

    public void setKeyStoreCertName(String keyStoreCertName) {
        this.keyStoreCertName = keyStoreCertName;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSpringContext() {
        return springContext;
    }

    public void setSpringContext(String springContext) {
        this.springContext = springContext;
    }

    public void setServiceNameSpace(String serviceNameSpace) {
        this.serviceNameSpace = serviceNameSpace;
    }
}

