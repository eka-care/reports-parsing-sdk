package com.example.ekacare.sdk;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * SDK for Eka Care Medical Records API
 * 
 * This class provides methods to interact with the Eka Care API
 * for processing medical documents.
 */
public class EkaCareSDK {
    
    private final String bearerToken;
    private final String baseUrl;
    private final RestTemplate restTemplate;
    
    /**
     * Constructor for EkaCareSDK with client credentials
     * 
     * @param clientId The client ID for authentication
     * @param clientSecret The client secret for authentication
     * @param baseUrl The base URL for the API (default: https://api.eka.care)
     */
    public EkaCareSDK(String clientId, String clientSecret, String baseUrl) {
        this.bearerToken = getAccessToken(clientId, clientSecret);
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Constructor with default base URL
     * 
     * @param clientId The client ID for authentication
     * @param clientSecret The client secret for authentication
     */
    public EkaCareSDK(String clientId, String clientSecret) {
        this(clientId, clientSecret, "https://api.eka.care");
    }
    
    /**
     * Get access token from the authentication API
     * 
     * @param clientId The client ID
     * @param clientSecret The client secret
     * @return The access token
     * @throws RuntimeException if authentication fails
     */
    private String getAccessToken(String clientId, String clientSecret) {
        String authUrl = "https://api.eka.care/connect-auth/v1/account/login";
        
        // Prepare request body
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("client_id", clientId);
        requestBody.put("client_secret", clientSecret);
        
        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);
        
        try {
            // Make authentication request
            ResponseEntity<Map> response = restTemplate.exchange(
                authUrl,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("access_token")) {
                return (String) responseBody.get("access_token");
            } else {
                throw new RuntimeException("Access token not found in response");
            }
        } catch (Exception e) {
            throw new RuntimeException("Authentication failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Process a medical document using the Eka Care API
     * 
     * @param filePath Path to the file to upload
     * @param docType Document type (default: "lr" for lab report)
     * @param task Processing task - one of "smart", "pii", or "both"
     * @return Map containing the API response
     * @throws IllegalArgumentException if file doesn't exist or task is invalid
     */
    public Map<String, Object> processDocument(String filePath, String docType, String task) {
        // Validate file exists
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("File not found: " + filePath);
        }
        
        // Validate task parameter
        if (!task.equals("smart") && !task.equals("pii") && !task.equals("both")) {
            throw new IllegalArgumentException("Invalid task. Must be one of: smart, pii, both");
        }
        
        // Build URL with query parameters
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/mr/api/v2/docs")
                .queryParam("dt", docType);
        
        // Handle 'both' case with multiple task parameters
        if (task.equals("both")) {
            uriBuilder.queryParam("task", "smart");
            uriBuilder.queryParam("task", "pii");
        } else {
            uriBuilder.queryParam("task", task);
        }
        
        String url = uriBuilder.build().toUriString();
        
        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(bearerToken);
        
        // Prepare file upload
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file));
        
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        
        // Make the API request
        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                Map.class
        );
        
        return response.getBody();
    }
    
    /**
     * Overload with default docType
     */
    public Map<String, Object> processDocument(String filePath, String task) {
        return processDocument(filePath, "lr", task);
    }
    
    /**
     * Retrieve the processing result for a previously submitted document
     * 
     * @param documentId The unique document ID returned from processDocument
     * @return Map containing the processing result
     */
    public Map<String, Object> getDocumentResult(String documentId) {
        String url = baseUrl + "/mr/api/v1/docs/" + documentId + "/result";
        
        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        
        // Make the API request
        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                Map.class
        );
        
        return response.getBody();
    }
}
