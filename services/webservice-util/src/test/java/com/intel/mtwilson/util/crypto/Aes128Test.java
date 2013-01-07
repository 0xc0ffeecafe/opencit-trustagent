/*
 * Copyright (C) 2011-2012 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.crypto;

import com.intel.mtwilson.crypto.CryptographyException;
import com.intel.mtwilson.crypto.Aes128;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import org.apache.commons.codec.binary.Base64;
import static org.junit.Assert.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jbuhacoff
 */
public class Aes128Test {
    private Logger log = LoggerFactory.getLogger(getClass());
    
    @Test
    public void testGenerateAesKey() throws CryptographyException {
        SecretKey key = Aes128.generateKey();
        System.out.println(Base64.encodeBase64String(key.getEncoded()));
    }
    
    @Test
    public void testEncryptDecrypt() throws CryptographyException, UnsupportedEncodingException {
        String input = "hello world";
        log.info("Input: {} (str length: {})", new String[] { input, String.valueOf(input.length()) });
        
        SecretKey secretKey = Aes128.generateKey(); // NoSuchAlgorithmException
        log.info("Secret Key: {} (raw length: {})", new String[] { Base64.encodeBase64String(secretKey.getEncoded()), String.valueOf(secretKey.getEncoded().length) });
        Aes128 aes = new Aes128(secretKey); // NoSuchPaddingException
        
        byte[] plaintext = input.getBytes("UTF-8"); //UnsupportedEncodingException
        byte[] ciphertext = aes.encrypt(plaintext); // InvalidKeyException, IllegalBlockSizeException, BadPaddingException
        
        log.info("Plaintext: {} (raw length: {})", new String[] { Base64.encodeBase64String(plaintext), String.valueOf(plaintext.length) });
        log.info("Ciphertext: {} (raw length: {})", new String[] { Base64.encodeBase64String(ciphertext), String.valueOf(ciphertext.length) });
        
        byte[] decrypted = aes.decrypt(ciphertext);
        log.info("Decrypted Plaintext: {} (raw length: {})", new String[] { Base64.encodeBase64String(decrypted), String.valueOf(decrypted.length) });
        String output = new String(decrypted, "UTF-8");
        log.info("Output: {} (str length: {})", new String[] { output, String.valueOf(output.length()) });
        
        assertEquals(input, output);
        
    }
}