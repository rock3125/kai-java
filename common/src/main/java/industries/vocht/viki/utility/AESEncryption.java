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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * use JAVA STRONG ENCRYPTION (must be installed!) to
 * encrypt/decrypt strings using AES256
 * create the viki-aes jks for AES hidden key
 *
 *  openssl genrsa -des3 -out kai.key 4096
 *  openssl req -new -key kai.key -out kai.csr
 *  openssl x509 -req -days 36500 -in kai.csr -signkey kai.key -out kai.crt
 *  keytool -importcert -trustcacerts -file kai.crt -alias pg-aes-cert -keystore viki-aes.jks
 *
 */
@Component
public class AESEncryption {

    private static final int kMaxStringSize = 100_000; // max string size

    private String certificatePassword; // the password from the store for certs

    private static Logger logger = LoggerFactory.getLogger(AESEncryption.class);

    @Value("${aes.keystore.file:/viki-aes.jks}")
    private String keyStoreFile;

    @Value("${aes.cert.alias:viki}")
    private String certAlias;

    @Value("${aes.keystore.password:not-set}")
    private String keyStorePassword;

    public AESEncryption() {
    }

    public void init() throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException {
        certificatePassword = getKeyStoreCertificate(keyStoreFile, keyStorePassword, certAlias);
    }

    /**
     * encrypt string with password and return the encrypted BASE64 string
     * @param str a plain text piece of data.  This is encrypted using password (also plaintext, transformed to sha256 hash)
     * @return base64 encrypted string
     * @throws IOException cannot initialise
     */
    public String encrypt(String str) throws IOException {
        if ( certificatePassword == null )
            throw new InvalidParameterException("key-store not initialised");
        return encrypt(str, certificatePassword);
    }

    /**
     * decrypt a string with password and return the plain text decrypted string
     * @param str a plain text piece of data.  This is encrypted using password (also plaintext, transformed to sha256 hash)
     * @return base64 encrypted string
     * @throws IOException
     */
    public String decrypt(String str) throws IOException {
        if ( certificatePassword == null )
            throw new InvalidParameterException("key-store not initialised");
        return decrypt(str, certificatePassword);
    }

    /**
     * get the public key's encoded data from the standard keystore as a base64 string
     * this key can then be used to encrypt/decrypt data
     * @param certificateAlias the alias of the certificate in the default java keystore
     *                         keytool -list -keystore $JAVA_HOME/jre/lib/security/cacerts -v
     * @return null if dne, or otherwise the base64 string of the public key matching certificateAlias
     * @throws IOException
     */
    private String getKeyStoreCertificate(String keyStorePath, String keyStorePassword, String certificateAlias)
            throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException {
        try {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

            if ( new File(keyStorePath).exists() ) {
                try (FileInputStream fis = new java.io.FileInputStream(keyStorePath)) {
                    ks.load(fis, keyStorePassword.toCharArray());
                }
            } else {
                try (InputStream is = getClass().getResourceAsStream(keyStorePath)) {
                    ks.load(is, keyStorePassword.toCharArray());
                }
            }

            Certificate certificate = ks.getCertificate(certificateAlias);
            if ( certificate != null ) {
                byte[] keyData = certificate.getPublicKey().getEncoded();
                return new String(Base64.getEncoder().encode(keyData));
            }
        } catch ( KeyStoreException | CertificateException | NoSuchAlgorithmException ex ) {
            logger.debug("getKeystoreCerticate()", ex);
            throw new IOException(ex.getMessage());
        }
        return null;
    }

    /**
     * encrypt string with password and return the encrypted BASE64 string
     * @param str a plain text piece of data.  This is encrypted using password (also plaintext, transformed to sha256 hash)
     * @param password your password - whatever
     * @return base64 encrypted string
     * @throws IOException
     */
    public String encrypt(String str, String password) throws IOException {

        if ( str == null ) {
            throw new InvalidParameterException("cannot encrypt null message");
        }

        if ( password == null || password.length() == 0 ) {
            throw new InvalidParameterException("invalid symmetric password");
        }

        try {
            BinarySerializer binarySerializer = new BinarySerializer(4096);

            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            sha256.reset();
            sha256.update(password.getBytes("UTF-8"));
            byte[] passwordHash = sha256.digest();
            SecretKeySpec spec = new SecretKeySpec(passwordHash, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, spec);
            align8Bytes(binarySerializer, str.length(), str.getBytes("UTF-8")); // align and embed string size as an int
            byte[] encBytes = cipher.doFinal(binarySerializer.getData());
            return new String(Base64.getEncoder().encode(encBytes)); // convert byte[] to string
        } catch (Exception ex) {
            throw new IOException("security algorithm failed:" + ex.getMessage());
        }
    }


    /**
     * decrypt a base64 encrypted string with password and return the decrypted string
     * @param str a plain text piece of data.  This is decrypted using password.
     * @param password your password - whatever
     * @return decrypted plain text string - or null on failure
     * @throws IOException
     */
    public String decrypt(String str, String password) throws IOException {

        if ( str == null ) {
            throw new InvalidParameterException("cannot decrypt null message");
        }

        if ( password == null || password.length() == 0 ) {
            throw new InvalidParameterException("invalid symmetric password");
        }

        try {
            BinarySerializer binarySerializer = new BinarySerializer(4096);

            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            sha256.update(password.getBytes("UTF-8"));
            byte[] passwordHash = sha256.digest();
            SecretKeySpec key = new SecretKeySpec(passwordHash, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            byte[] encBytes = Base64.getDecoder().decode(str);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decBytes = cipher.doFinal(encBytes); // decrypt
            if (decBytes != null && decBytes.length > 4) { // do we have a string?
                binarySerializer.writeRawByteArray(decBytes, 0, decBytes.length);
                int size = binarySerializer.readInt();
                if ( size >= 0 && size < kMaxStringSize && size <= binarySerializer.getSize() ) {
                    byte[] data = binarySerializer.readByteArray();
                    return new String(data, 0, size);
                } else {
                    throw new IOException("security algorithm failed: invalid string length " + size);
                }
            } else {
                throw new IOException("security algorithm failed: length wrong");
            }
        } catch (Exception ex) {
            throw new IOException("security algorithm failed:" + ex.getMessage());
        }
    }

    /**
     * align the string data along with its original size
     * into a memory block (8 aligned for encryption/decryption)
     * @param binarySerializer the serialiser this data is written into
     * @param strSize the size of the string to write
     * @param data the string utf8 bytes
     */
    private void align8Bytes(BinarySerializer binarySerializer, int strSize, byte[] data)
    {
        if (data != null) {

            // prefix string with byte length
            int length = data.length + 4; // + 4 is the int for the size
            int mod = (8 - (length % 8));
            int newLength = length + mod;

            binarySerializer.writeInt(strSize);
            binarySerializer.writeByteArray(data, 0, data.length);
            for (int i = data.length + 4; i < newLength; i++) { // fill the remainder of the block with zeros
                binarySerializer.writeByte(0);
            }
        }
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getKeyStoreFile() {
        return keyStoreFile;
    }

    public void setKeyStoreFile(String keyStoreFile) {
        this.keyStoreFile = keyStoreFile;
    }

    public String getCertAlias() {
        return certAlias;
    }

    public void setCertAlias(String certAlias) {
        this.certAlias = certAlias;
    }
}

