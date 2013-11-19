/*
 * Copyright (C) 2013 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson;

import com.intel.mtwilson.api.ClientFactory;
import com.intel.mtwilson.api.MtWilson;
import com.intel.mtwilson.crypto.Aes128;
import com.intel.mtwilson.crypto.CryptographyException;
import com.intel.mtwilson.io.FileResource;
import com.intel.mtwilson.tls.InsecureTlsPolicy;
import com.intel.mtwilson.util.ASDataCipher;
import com.intel.mtwilson.util.Aes128DataCipher;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience class for instantiating an API CLIENT for your unit tests.  Relies on MyConfiguration for
 * your local settings.
 * 
 * How to use it in your code:
 * 
 * ApiClient client = My.client();
 * 
 * @author jbuhacoff
 */
public class My {
    private transient static Logger log = LoggerFactory.getLogger(My.class);
    private static MyConfiguration config = null;
    private static MtWilson client = null;
    private static MyPersistenceManager pm = null;
    private static MyJdbc jdbc = null;
    private static MyJpa jpa = null;
    private static MyEnvironment env = null;

    public static void initDataEncryptionKey() throws IOException {
        initDataEncryptionKey(My.configuration().getDataEncryptionKeyBase64());
    }

    public static void initDataEncryptionKey(String dekBase64) {
        try {
            //log.info("XXX DEK = {}", dekBase64);
            ASDataCipher.cipher = new Aes128DataCipher(new Aes128(Base64.decodeBase64(dekBase64)));
            //log.info("XXX My ASDataCipher ref = {}", ASDataCipher.cipher.hashCode());
        }
        catch(CryptographyException e) {
            throw new IllegalArgumentException("Cannot initialize data encryption cipher", e);
        }              
    }

    public static void init() throws IOException {
        initDataEncryptionKey();
    }
    
    public static void reset() { config = null; jpa = null; }
    
    public static MyConfiguration configuration() throws IOException { 
        if( config == null ) {
             config = new MyConfiguration();
        }
        return config; 
    }
    
    public static MtWilson client() throws MalformedURLException, IOException {
        if( client == null ) {
            log.debug("Mt Wilson URL: {}", configuration().getMtWilsonURL().toString());
            client = ClientFactory.clientForUserInResource(
                new FileResource(configuration().getKeystoreFile()), 
                configuration().getKeystoreUsername(),
                configuration().getKeystorePassword(),
                configuration().getMtWilsonURL(),
                new InsecureTlsPolicy() // XXX TODO need to load the policy name, then instantiate the right one using the keystore file 
                );
        }
        return client;
    }
    
    public static MyPersistenceManager persistenceManager() throws IOException {
        if( pm == null ) {
            pm = new MyPersistenceManager(configuration().getProperties(
                    "mtwilson.db.protocol", "mtwilson.db.driver",
                    "mtwilson.db.host", "mtwilson.db.port", "mtwilson.db.user", 
                    "mtwilson.db.password", "mtwilson.db.schema", "mtwilson.as.dek"));
        }
        return pm;
    }
    
    public static MyJdbc jdbc() throws IOException {
        if( jdbc == null ) {
            jdbc = new MyJdbc(configuration());
        }
        return jdbc;
    }
    
    public static MyJpa jpa() throws IOException {
        if( jpa == null ) {
            initDataEncryptionKey();
            jpa = new MyJpa(persistenceManager());
        }
        return jpa;
    }
    
    public static MyEnvironment env() throws IOException {
        if( env == null ) {
            env = new MyEnvironment(configuration().getEnvironmentFile());
        }
        return env;
    }
}
