/*
 * Copyright (C) 2014 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.trustagent.ws.v2;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.intel.dcsg.cpg.io.UUID;
import com.intel.dcsg.cpg.xml.JAXB;
import com.intel.mountwilson.common.TAException;
import com.intel.mountwilson.trustagent.commands.hostinfo.HostInfoCmd;
import com.intel.mountwilson.trustagent.data.TADataContext;
import com.intel.mtwilson.launcher.ws.ext.V2;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.intel.mtwilson.trustagent.model.VMQuoteResponse;
import com.intel.mtwilson.trustagent.model.HostInfo;
import com.intel.mtwilson.trustagent.model.VMAttestationRequest;
import com.intel.mtwilson.trustagent.model.VMAttestationResponse;
import com.intel.mtwilson.trustagent.vrtmclient.TCBuffer;
import com.intel.mtwilson.trustagent.vrtmclient.Factory;
import com.intel.mtwilson.trustagent.vrtmclient.RPCCall;
import com.intel.mtwilson.trustagent.vrtmclient.RPClient;
import com.intel.mtwilson.trustagent.vrtmclient.xml.MethodResponse;
import com.intel.mtwilson.trustagent.vrtmclient.xml.Param;
import com.intel.mtwilson.trustagent.vrtmclient.xml.Value;
import java.io.File;
import java.io.FileInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.core.Context;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;


/**
 * 
 * XML input “<vm_challenge_request_json><vm_instance_id></ vm_instance_id><vm_challenge_request_json>”
 * JSON input {"vm_challenge_request": {“vm_instance_id":“dcc4a894-869b-479a-a24a-659eef7a54bd"}}
 * JSON output: {“vm_trust_response":{“host_name”:”10.1.1.1”,“vm_instance_id":“dcc4a894-869b-479a-a24a-659eef7a54bd","trust_status":true}}
 * 
 * @author hxia
 */
@V2
@Path("/vrtm")
public class Vrtm {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Vrtm.class);
    private static final String measurementXMLFileName = "measurement.xml";
    private static final String trustPolicyFileName = "TrustPolicy.xml";
    
    @POST
    @Path("/status")
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    public VMAttestationResponse getVMAttestationStatus(VMAttestationRequest vmAttestationRequest) throws TAException, IOException {
        
        String vmInstanceId = vmAttestationRequest.getVmInstanceId();
        VMAttestationResponse vmAttestationResponse = new VMAttestationResponse();        

        RPClient rpcInstance = new RPClient("127.0.0.1", 16005); // create instance of RPClient
        boolean vmstatus = rpcInstance.getVmStatus(vmInstanceId);    // send tcBuffer to rpcore 
        rpcInstance.close();   // close RPClient
        
        //set report
        vmAttestationResponse.setVmInstanceId(vmInstanceId);
        vmAttestationResponse.setTrustStatus(vmstatus);
        
        return vmAttestationResponse;

    }
	
    @POST
    @Path("/report")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    public VMQuoteResponse getVMAttestationReport(VMAttestationRequest vmAttestationRequest) {
        try {
            // Call into the vRTM API and get the path information
            String instanceFolderPath = "/var/lib/nova/instances/" + vmAttestationRequest.getVmInstanceId() + "/";
            
            VMQuoteResponse vmQuoteResponse = new VMQuoteResponse();
            vmQuoteResponse.setVmMeasurements(FileUtils.readFileToByteArray(new File(String.format("%s%s", instanceFolderPath, measurementXMLFileName))));
            vmQuoteResponse.setVmTrustPolicy(FileUtils.readFileToByteArray(new File(String.format("%s%s", instanceFolderPath, trustPolicyFileName))));
            vmQuoteResponse.setVmQuote(FileUtils.readFileToByteArray(new File(String.format("%s%s", instanceFolderPath, trustPolicyFileName))));
            
            return vmQuoteResponse;
            
        } catch (IOException ex) {
            log.error("Error during reading of VM quote information. {}", ex.getMessage());
        }

        return null;
    }	

/*    @POST
    @Path("/report")
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    public String getVMAttestationReport(VMAttestationRequest vmAttestationRequest) throws TAException {
        
        JAXB jaxb = new JAXB();        
        // Call into the vRTM API and get the path information
        String instanceFolderPath = "/var/lib/nova/instances/" + vmAttestationRequest.getVmInstanceId() + "/";
        
        // Build the XML here.
        VMQuoteResponse vmQuoteResponse = new VMQuoteResponse();
        
        // TODO: The below object should be created by reading from the VMQuote.xml file in the instance folder
        try {
            VMQuote vmInstanceQuote = new VMQuote();
            vmInstanceQuote.setCumulativeHash("2284377e7a81243ab4305412669d90ba9253a64a");
            vmInstanceQuote.setVmInstanceId(vmAttestationRequest.getVmInstanceId());
            vmInstanceQuote.setDigestAlg("SHA-256");
            vmInstanceQuote.setNonce(vmAttestationRequest.getNonce());
            
            vmQuoteResponse.setVMQuote(vmInstanceQuote);
            
        } catch (Exception ex) {
            log.error("Error reading the vm quote file. {}", ex.getMessage());
            throw new WebApplicationException(Response.serverError().header("Error", 
                    String.format("%s. %s", "Error reading the vm quote file.", ex.getMessage())).build());
        }
        
        try (FileInputStream measurementXMLFileStream = new FileInputStream(String.format("%s%s", instanceFolderPath, measurementXMLFileName))) {
        
            String measurementXML = IOUtils.toString(measurementXMLFileStream, "UTF-8");
            Measurements readMeasurements = jaxb.read(measurementXML, Measurements.class);
            vmQuoteResponse.setMeasurements(readMeasurements);

        } catch (Exception ex) {
            log.error("Error reading the measurement log. {}", ex.getMessage());
            throw new WebApplicationException(Response.serverError().header("Error", 
                    String.format("%s. %s", "Error reading the measurement log.", ex.getMessage())).build());
        }
                
        try (FileInputStream trustPolicyFileStream = new FileInputStream(String.format("%s%s", instanceFolderPath, trustPolicyFileName))) {
        
            String trustPolicyXML = IOUtils.toString(trustPolicyFileStream, "UTF-8");
            TrustPolicy trustPolicy = jaxb.read(trustPolicyXML, TrustPolicy.class);
            vmQuoteResponse.setTrustPolicy(trustPolicy);

        } catch (Exception ex) {
            log.error("Error reading the Trust policy. {}", ex.getMessage());
            throw new WebApplicationException(Response.serverError().header("Error", 
                    String.format("%s. %s", "Error reading the Trust policy.", ex.getMessage())).build());
        }
        
        try {
            String quoteResponse = jaxb.write(vmQuoteResponse);
            return quoteResponse;
        } catch (JAXBException ex) {
            log.error("Error serializing the VM quote response. {}", ex.getMessage());
            throw new WebApplicationException(Response.serverError().header("Error", 
                    String.format("%s. %s", "Error serializing the VM quote response.", ex.getMessage())).build());
        }        
    }	
*/    

}
