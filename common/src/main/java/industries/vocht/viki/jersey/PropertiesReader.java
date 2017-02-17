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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Created by peter on 20/03/16.
 *
 * helper to read .properties files
 *
 */
public class PropertiesReader {

    private final Logger logger = LoggerFactory.getLogger(PropertiesReader.class);

    public PropertiesReader() {
    }

    /**
     * translate a port into ssl / non-ssl based on a set sequence of numbers
     * - anything with 443 at the end of the number is SSL, otherwise not
     * @param port the port of the server
     * @return true if ssl is to be used
     */
    public boolean useSSL( int port ) {
        return (port == 443) || ((port % 1_000) == 443) || ((port % 10_000) == 443);
    }

    /**
     * use a few clever tricks to get a properties file - throw an exception if
     * we can't get to the properties file after all
     * @param propertiesFilename the filename of the properties file to load in the file system or class-path
     * @return the properties file
     */
    public Properties getProperties(String propertiesFilename ) throws IOException, VikiException {
        if (propertiesFilename == null) {
            throw new VikiException("invalid properties file, null");
        }
        if ( !new File(propertiesFilename).exists() ) {
            logger.error("file not found " + propertiesFilename);

            // load the file from resources
            Path p = Paths.get(propertiesFilename);
            Properties properties = new Properties();
            logger.error("using RESOURCE properties " + p.getFileName().toString());
            InputStream input = getClass().getResourceAsStream(p.getFileName().toString());
            if ( input == null ) {
                throw new VikiException("could not load class resource " + p.getFileName().toString());
            }
            properties.load(input);
            return properties;

        } else {
            logger.info("using FILE properties " + propertiesFilename);
            Properties properties = new Properties();
            properties.load(new FileInputStream(new File(propertiesFilename)));
            return properties;
        }
    }

    /**
     * get an integer from the properties file by name
     * @param properties the properties file (cannot be null)
     * @param name the name of the property to read
     * @param defaultValue the default value if the property is missing
     * @return the integer value of the property
     * @throws VikiException
     */
    public int getInt(Properties properties, String name, int defaultValue) throws VikiException {
        if ( properties == null || name == null ) {
            throw new VikiException("invalid parameters, null");
        }
        if ( properties.getProperty(name) == null ) {
            logger.debug("properties file missing " + name + ", using defaultValue = " + defaultValue);
            return defaultValue;
        }
        return Integer.parseInt(properties.getProperty(name));
    }

    /**
     * get an integer from the properties file by name
     * @param properties the properties file (cannot be null)
     * @param name the name of the property to read
     * @return the integer value of the property
     * @throws VikiException
     */
    public int getInt(Properties properties, String name) throws VikiException {
        if ( properties == null || name == null ) {
            throw new VikiException("invalid parameters, null");
        }
        if ( properties.getProperty(name) == null ) {
            throw new VikiException("invalid parameters, missing '" + name + "'");
        }
        return Integer.parseInt(properties.getProperty(name));
    }

    /**
     * get a string from the properties file by name
     * @param properties the properties file (cannot be null)
     * @param name the name of the property to read
     * @param defaultValue the default value if the property is missing
     * @return the string value of the property
     * @throws VikiException
     */
    public String getString(Properties properties, String name, String defaultValue) throws VikiException {
        if ( properties == null || name == null ) {
            throw new VikiException("invalid parameters, null");
        }
        if ( properties.getProperty(name) == null ) {
            logger.debug("properties file missing " + name + ", using defaultValue = " + defaultValue);
            return defaultValue;
        }
        return properties.getProperty(name);
    }

    /**
     * get a string from the properties file by name
     * @param properties the properties file (cannot be null)
     * @param name the name of the property to read
     * @return the string value of the property
     * @throws VikiException
     */
    public String getString(Properties properties, String name) throws VikiException {
        if ( properties == null || name == null ) {
            throw new VikiException("invalid parameters, null");
        }
        if ( properties.getProperty(name) == null ) {
            throw new VikiException("invalid parameters, missing '" + name + "'");
        }
        return properties.getProperty(name);
    }

}
