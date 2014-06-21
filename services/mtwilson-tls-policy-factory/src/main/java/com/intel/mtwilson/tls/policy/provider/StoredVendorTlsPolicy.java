/*
 * Copyright (C) 2014 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.tls.policy.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.mtwilson.jaxrs2.provider.JacksonObjectMapperProvider;
import com.intel.mtwilson.tls.policy.TlsPolicyChoice;
import com.intel.mtwilson.tls.policy.TlsPolicyDescriptor;
import com.intel.mtwilson.tls.policy.factory.TlsPolicyProvider;
import com.intel.mtwilson.tls.policy.jdbi.TlsPolicyDAO;
import com.intel.mtwilson.tls.policy.jdbi.TlsPolicyJdbiFactory;
import com.intel.mtwilson.tls.policy.jdbi.TlsPolicyRecord;
import java.io.IOException;

/**
 * This policy strategy is able to load a different policy depending on the
 * type of host being considered, for example one could define separate 
 * TLS Policies for vCenter servers and XenCenter servers and apply them
 * automatically using this strategy where the client does not need to know
 * about them.
 * 
 * @author jbuhacoff
 */
public class StoredVendorTlsPolicy implements TlsPolicyProvider {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StoredVendorTlsPolicy.class);
    private String vendor;

    public StoredVendorTlsPolicy(String vendor) {
        this.vendor = vendor;
    }

    public StoredVendorTlsPolicy(VendorDescriptor vendorDescriptor) {
        this.vendor = vendorDescriptor.getVendorProtocol();
    }
    
    @Override
    public TlsPolicyChoice getTlsPolicyChoice() {
        if( vendor == null ) { return null; }
        try (TlsPolicyDAO dao = TlsPolicyJdbiFactory.tlsPolicyDAO()) {
            TlsPolicyRecord tlsPolicyRecord = dao.findTlsPolicyByName("auto vendor:"+vendor);
            if( tlsPolicyRecord == null ) { return null; }
            if( tlsPolicyRecord.isPrivate() ) { log.debug("Ignoring private vendor policy {}", tlsPolicyRecord.getId()); return null; }
            try {
                TlsPolicyDescriptor tlsPolicyDescriptor = getTlsPolicyDescriptorFromTlsPolicyRecord(tlsPolicyRecord);
                TlsPolicyChoice tlsPolicyChoice = new TlsPolicyChoice();
                tlsPolicyChoice.setTlsPolicyDescriptor(tlsPolicyDescriptor);
                return tlsPolicyChoice;
            }
            catch(IOException e) {
                log.error("Cannot read tls policy from table", e);
                return null;
            }
        }
        catch(IOException e) {
            log.error("Cannot close DAO", e);
        }
        return null;
    }
    
    private TlsPolicyDescriptor getTlsPolicyDescriptorFromTlsPolicyRecord(TlsPolicyRecord tlsPolicyRecord) throws IOException {
        if( tlsPolicyRecord.getContentType() == null || tlsPolicyRecord.getContentType().isEmpty() ) {
            return null;
        }
        JacksonObjectMapperProvider mapperProvider = new JacksonObjectMapperProvider();
        ObjectMapper mapper = mapperProvider.createDefaultMapper();
        TlsPolicyDescriptor tlsPolicyDescriptor = mapper.readValue(tlsPolicyRecord.getContent(), TlsPolicyDescriptor.class);
        return tlsPolicyDescriptor;
    }
    
    public static interface VendorDescriptor {
        String getVendorProtocol();
    }
    
}
