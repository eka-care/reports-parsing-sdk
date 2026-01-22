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
     * Process a document and wait for completion.
     * This is a blocking method that polls until the document is processed.
     *
     * @param filePath Path to the file to upload
     * @param task Processing task - one of "smart", "pii", or "both"
     * @param pollIntervalSeconds Seconds to wait between polling attempts
     * @param timeoutSeconds Maximum seconds to wait for completion
     * @return Map containing the completed processing result
     * @throws InterruptedException if thread is interrupted during polling
     * @throws RuntimeException if processing fails or times out
     */
    public Map<String, Object> processDocumentAndWait(String filePath, String task, int pollIntervalSeconds, int timeoutSeconds)
            throws InterruptedException {
        return sdk.processAndWait(filePath, task, pollIntervalSeconds, timeoutSeconds);
    }

    /**
     * Process a document and wait with default timeout (300 seconds)
     */
    public Map<String, Object> processDocumentAndWait(String filePath, String task, int pollIntervalSeconds)
            throws InterruptedException {
        return sdk.processAndWait(filePath, task, pollIntervalSeconds);
    }

    /**
     * Process a document and wait with default settings (10 second interval, 300 second timeout)
     */
    public Map<String, Object> processDocumentAndWait(String filePath, String task)
            throws InterruptedException {
        return sdk.processAndWait(filePath, task);
    }
}
