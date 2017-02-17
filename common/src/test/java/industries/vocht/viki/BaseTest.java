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

package industries.vocht.viki;

import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 20/06/16.
 *
 * helper class for unit tests
 *
 */
public class BaseTest {

    /**
     * load an item from the test resources
     * @param resourcePathAndName the path and filename of the resource item
     * @return the byte[] that is the item, or null if not found
     */
    public byte[] loadItemFromResources( String resourcePathAndName ) throws IOException {
        if ( resourcePathAndName != null ) {
            InputStream in = getClass().getResourceAsStream(resourcePathAndName);
            if ( in != null ) {
                return IOUtils.toByteArray(in);
            }
        }
        return null;
    }

}

