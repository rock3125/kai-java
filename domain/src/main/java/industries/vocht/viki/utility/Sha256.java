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

import industries.vocht.viki.IDatabase;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Created by peter on 4/03/16.
 *
 */
public class Sha256 {

    // proper sha256 with salt
    public String generateSha256Password(UUID salt, String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String saltedStr = salt.toString() + ":" + password + ":" + IDatabase.pepper;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(saltedStr.getBytes("UTF-8")); // Change this to "UTF-16" if needed
        byte[] digest = md.digest();
        return String.format("%064x", new java.math.BigInteger(1, digest));
    }


}

