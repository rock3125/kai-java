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

package industries.vocht.viki.encryption;

import industries.vocht.viki.utility.AESEncryption;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

/**
 * Created by peter on 27/04/16.
 *
 * test the AES encryption system
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test-context.xml"})
public class AESEncryptionTest {

    @Autowired
    private AESEncryption aes;

    @Test
    public void testAES1() throws IOException {
        genericAESTest("Some text to EnCrYpT!", "pwd1@#!@");
        genericAESTest("Some other text to EnCrYpT!", "pwd3124!@@#");
    }

    @Test
    public void testAES2() throws IOException {
        genericAESTest("Some other text to EnCrYpT!", "pwdAsdasd!@#!");
        genericAESTest("Some text to EnCrYpT!", "pwd!231251@@@");
    }


    private void genericAESTest( String plainText, String password ) throws IOException {
        String encryptedText = aes.encrypt(plainText, password);
        String decryptedText = aes.decrypt(encryptedText, password);
        Assert.assertTrue(decryptedText != null && decryptedText.equals(plainText));
    }
}
