/*
 * Copyright (C) 2014 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.tls.policy.provider;

import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.My;
import com.intel.mtwilson.tls.policy.TlsPolicyChoice;
import com.intel.mtwilson.tls.policy.TlsPolicyDescriptor;
import com.intel.mtwilson.tls.policy.factory.TlsPolicyProvider;

/**
 * Loads default TLS Policy from value of mtwilson.default.tls.policy.id in
 * mtwilson.properties or in database configuration table.
 * 
 * @author jbuhacoff
 */
public class DefaultTlsPolicy implements TlsPolicyProvider {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultTlsPolicy.class);
    
    @Override
    public TlsPolicyChoice getTlsPolicyChoice() {
        String defaultTlsPolicyId = My.configuration().getDefaultTlsPolicyId();
        log.debug("DefaultTlsPolicy: {}", defaultTlsPolicyId);
        if( defaultTlsPolicyId == null || defaultTlsPolicyId.isEmpty()) {
            return null;
        }
        if( defaultTlsPolicyId.equals("INSECURE") || defaultTlsPolicyId.equals("TRUST_FIRST_CERTIFICATE") ) {
            TlsPolicyChoice tlsPolicyChoice = new TlsPolicyChoice();
            tlsPolicyChoice.setTlsPolicyDescriptor(new TlsPolicyDescriptor());
            tlsPolicyChoice.getTlsPolicyDescriptor().setName(defaultTlsPolicyId);
            return tlsPolicyChoice;
        }
        if( UUID.isValid(defaultTlsPolicyId)) {
            TlsPolicyChoice tlsPolicyChoice = new TlsPolicyChoice();
            tlsPolicyChoice.setTlsPolicyId(defaultTlsPolicyId);
            return tlsPolicyChoice;
        }
        log.debug("DefaultTlsPolicy not found");
        return null;
    }
    
}
