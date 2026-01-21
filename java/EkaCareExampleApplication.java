package com.example.ekacare.example;

import com.example.ekacare.sdk.EkaCareSDK;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

/**
 * Example Spring Boot application demonstrating usage of EkaCareSDK
 */
@SpringBootApplication
public class EkaCareExampleApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(EkaCareExampleApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Parse command line arguments
        if (args.length < 2) {
            System.err.println("Usage: --client-id=<id> --client-secret=<secret>");
            return;
        }
        
        String clientId = null;
        String clientSecret = null;
        
        for (String arg : args) {
            if (arg.startsWith("--client-id=")) {
                clientId = arg.substring("--client-id=".length());
            } else if (arg.startsWith("--client-secret=")) {
                clientSecret = arg.substring("--client-secret=".length());
            }
        }
        
        if (clientId == null || clientSecret == null) {
            System.err.println("Both --client-id and --client-secret are required");
            return;
        }
        
        // Initialize the SDK with client credentials
        System.out.println("Authenticating...");
        EkaCareSDK sdk = new EkaCareSDK(clientId, clientSecret);

        try {
            // Process a document
            System.out.println("Submitting document for processing...");
            Map<String, Object> result = sdk.processDocument(
                "/path/to/your/document.jpg",
                "smart"
            );
            
            System.out.println("Document submitted: " + result);

            // Get document_id from response
            String documentId = (String) result.get("document_id");
            System.out.println("Polling for document: " + documentId);

            // Poll for results
            while (true) {
                Map<String, Object> response = sdk.getDocumentResult(documentId);
                
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null && data.containsKey("fhir") && data.containsKey("output")) {
                    System.out.println("Processing completed!");
                    System.out.println("FHIR data: " + data.get("fhir"));
                    System.out.println("Output data: " + data.get("output"));
                    break;
                }

                System.out.println("Waiting 10 seconds before next poll...");
                Thread.sleep(10000); // Wait 10 seconds
            }

        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("API Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
