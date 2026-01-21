package com.example.ekacare.service;

import com.example.ekacare.sdk.EkaCareSDK;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service class to use EkaCareSDK in a Spring Boot application
 * This demonstrates how to use the SDK as a Spring service
 */
@Service
public class EkaCareService {
    
    private final EkaCareSDK sdk;
    
    /**
     * Constructor that initializes the SDK using application.properties
     * 
     * @param clientId Client ID from application.properties
     * @param clientSecret Client Secret from application.properties
     * @param baseUrl Base URL from application.properties (optional)
     */
    public EkaCareService(
            @Value("${ekacare.client.id}") String clientId,
            @Value("${ekacare.client.secret}") String clientSecret,
            @Value("${ekacare.base.url:https://api.eka.care}") String baseUrl) {
        this.sdk = new EkaCareSDK(clientId, clientSecret, baseUrl);
    }
    
    /**
     * Process a document and return the initial response
     */
    public Map<String, Object> submitDocument(String filePath, String task) {
        return sdk.processDocument(filePath, task);
    }
    
    /**
     * Get the processing result for a document
     */
    public Map<String, Object> getResult(String documentId) {
        return sdk.getDocumentResult(documentId);
    }
    
    /**
     * Process a document and wait for completion
     * This is a blocking method that polls until the document is processed
     */
    public Map<String, Object> processDocumentAndWait(String filePath, String task, int pollIntervalSeconds) 
            throws InterruptedException {
        
        // Submit document
        Map<String, Object> submitResult = sdk.processDocument(filePath, task);
        String documentId = (String) submitResult.get("document_id");
        
        // Poll for results
        while (true) {
            Map<String, Object> result = sdk.getDocumentResult(documentId);
            String status = (String) result.get("status");
            
            if ("completed".equals(status)) {
                return result;
            }
            
            if ("failed".equals(status)) {
                throw new RuntimeException("Document processing failed");
            }
            
            Thread.sleep(pollIntervalSeconds * 1000L);
        }
    }
    
    /**
     * Process a document and wait with default 10 second interval
     */
    public Map<String, Object> processDocumentAndWait(String filePath, String task) 
            throws InterruptedException {
        return processDocumentAndWait(filePath, task, 10);
    }
}
