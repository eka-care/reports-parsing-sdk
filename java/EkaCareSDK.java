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
 *
 * Example usage:
 * <pre>
 * try (EkaCareSDK sdk = new EkaCareSDK("your_client_id", "your_client_secret")) {
 *     Map&lt;String, Object&gt; result = sdk.processDocument("/path/to/file.jpg", "smart");
 *     System.out.println(result.get("document_id"));
 * }
 * </pre>
 */
public class EkaCareSDK implements AutoCloseable {

    // API Endpoints
    private static final String AUTH_ENDPOINT = "/connect-auth/v1/account/login";
    private static final String PROCESS_ENDPOINT = "/mr/api/v2/docs";
    private static final String RESULT_ENDPOINT = "/mr/api/v1/docs/%s/result";

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
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.restTemplate = new RestTemplate();
        this.bearerToken = getAccessToken(clientId, clientSecret, this.baseUrl);
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
     * @param baseUrl The base URL for authentication
     * @return The access token
     * @throws RuntimeException if authentication fails
     */
    private String getAccessToken(String clientId, String clientSecret, String baseUrl) {
        String authUrl = baseUrl + AUTH_ENDPOINT;
        
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
                .fromHttpUrl(baseUrl + PROCESS_ENDPOINT)
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
        String url = baseUrl + String.format(RESULT_ENDPOINT, documentId);
        
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

    /**
     * Process a document and wait for completion.
     *
     * This is a convenience method that combines processDocument and polling
     * for results in a single call.
     *
     * @param filePath Path to the file to upload
     * @param task Processing task - one of "smart", "pii", or "both"
     * @param pollIntervalSeconds Seconds to wait between polling attempts (default: 10)
     * @param timeoutSeconds Maximum seconds to wait for completion (default: 300)
     * @return Map containing the completed processing result
     * @throws IllegalArgumentException if file doesn't exist or task is invalid
     * @throws RuntimeException if processing fails or times out
     * @throws InterruptedException if thread is interrupted during polling
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> processAndWait(String filePath, String task, int pollIntervalSeconds, int timeoutSeconds)
            throws InterruptedException {
        // Submit document
        Map<String, Object> submitResult = processDocument(filePath, task);
        String documentId = (String) submitResult.get("document_id");

        // Poll for results
        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeoutSeconds * 1000L;

        while (true) {
            // Check timeout
            if (System.currentTimeMillis() - startTime > timeoutMillis) {
                throw new RuntimeException("Document processing timed out after " + timeoutSeconds + " seconds");
            }

            Map<String, Object> result = getDocumentResult(documentId);
            Map<String, Object> data = (Map<String, Object>) result.get("data");

            // Check if processing is complete (both fhir and output are present)
            if (data != null && data.containsKey("fhir") && data.containsKey("output")) {
                return result;
            }

            // Check if processing failed
            if ("failed".equals(result.get("status"))) {
                throw new RuntimeException("Document processing failed");
            }

            // Wait before next poll
            Thread.sleep(pollIntervalSeconds * 1000L);
        }
    }

    /**
     * Process a document and wait for completion with default timeout.
     *
     * @param filePath Path to the file to upload
     * @param task Processing task - one of "smart", "pii", or "both"
     * @param pollIntervalSeconds Seconds to wait between polling attempts
     * @return Map containing the completed processing result
     * @throws InterruptedException if thread is interrupted during polling
     */
    public Map<String, Object> processAndWait(String filePath, String task, int pollIntervalSeconds)
            throws InterruptedException {
        return processAndWait(filePath, task, pollIntervalSeconds, 300);
    }

    /**
     * Process a document and wait for completion with default settings.
     *
     * @param filePath Path to the file to upload
     * @param task Processing task - one of "smart", "pii", or "both"
     * @return Map containing the completed processing result
     * @throws InterruptedException if thread is interrupted during polling
     */
    public Map<String, Object> processAndWait(String filePath, String task) throws InterruptedException {
        return processAndWait(filePath, task, 10, 300);
    }

    /**
     * Close resources. This method is provided for compatibility but
     * RestTemplate does not require explicit cleanup.
     */
    @Override
    public void close() {
        // RestTemplate doesn't require explicit cleanup, but this allows
        // the SDK to be used in try-with-resources blocks for consistency
        // with other SDK implementations.
    }
}
