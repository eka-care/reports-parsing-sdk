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
        
        // Initialize the SDK with client credentials using try-with-resources
        System.out.println("Authenticating...");

        try (EkaCareSDK sdk = new EkaCareSDK(clientId, clientSecret)) {
            // Example 1: Process and wait for completion (recommended)
            System.out.println("\n=== Example 1: Process and wait for completion ===");
            System.out.println("Processing document...");

            Map<String, Object> result = sdk.processAndWait(
                    "/path/to/your/document.jpg",
                    "smart",
                    10,  // poll interval in seconds
                    300  // timeout in seconds
            );

            System.out.println("Processing completed!");
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            if (data != null) {
                System.out.println("FHIR data: " + data.get("fhir"));
                System.out.println("Output data: " + data.get("output"));
            }

            // Example 2: Manual submission and polling (if needed)
            System.out.println("\n=== Example 2: Manual submission and polling ===");
            Map<String, Object> submitResult = sdk.processDocument(
                    "/path/to/another/document.jpg",
                    "smart"
            );

            String documentId = (String) submitResult.get("document_id");
            System.out.println("Document submitted. ID: " + documentId);

            // Poll manually if needed
            Map<String, Object> pollResult = sdk.getDocumentResult(documentId);
            System.out.println("Current status: " + pollResult.get("status"));

        } catch (IllegalArgumentException e) {
            System.err.println("Validation error: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Processing interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
