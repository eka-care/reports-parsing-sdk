# Running with Command Line Arguments

The Eka Care SDK supports running with command-line arguments for client credentials, similar to the Python version.

## Running the Example Application

### Using Maven

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--client-id=YOUR_CLIENT_ID --client-secret=YOUR_CLIENT_SECRET"
```

### Using Java Directly

First, build the JAR:
```bash
mvn clean package
```

Then run:
```bash
java -jar target/ekacare-sdk-1.0.0.jar --client-id=YOUR_CLIENT_ID --client-secret=YOUR_CLIENT_SECRET
```

### Using IntelliJ IDEA

1. Right-click on `EkaCareExampleApplication.java`
2. Select "Modify Run Configuration..."
3. In "Program arguments", enter:
   ```
   --client-id=YOUR_CLIENT_ID --client-secret=YOUR_CLIENT_SECRET
   ```
4. Click "OK"
5. Run the application

## Creating a Standalone Command-Line Tool

Create `CliRunner.java`:

```java
package com.example.ekacare;

import com.example.ekacare.sdk.EkaCareSDK;

import java.util.Map;

/**
 * Command-line interface for Eka Care SDK
 * Usage: java CliRunner --client-id=<id> --client-secret=<secret> --file=<path>
 */
public class CliRunner {
    
    public static void main(String[] args) {
        // Parse command-line arguments
        String clientId = null;
        String clientSecret = null;
        String filePath = null;
        String task = "smart"; // default
        
        for (String arg : args) {
            if (arg.startsWith("--client-id=")) {
                clientId = arg.substring("--client-id=".length());
            } else if (arg.startsWith("--client-secret=")) {
                clientSecret = arg.substring("--client-secret=".length());
            } else if (arg.startsWith("--file=")) {
                filePath = arg.substring("--file=".length());
            } else if (arg.startsWith("--task=")) {
                task = arg.substring("--task=".length());
            }
        }
        
        // Validate required arguments
        if (clientId == null || clientSecret == null) {
            System.err.println("Usage: java CliRunner --client-id=<id> --client-secret=<secret> [--file=<path>] [--task=<smart|pii|both>]");
            System.exit(1);
        }
        
        try {
            // Initialize SDK
            System.out.println("Authenticating...");
            EkaCareSDK sdk = new EkaCareSDK(clientId, clientSecret);
            System.out.println("Authentication successful!");
            
            // If file provided, process it
            if (filePath != null) {
                System.out.println("Processing file: " + filePath);
                Map<String, Object> result = sdk.processDocument(filePath, task);
                
                String documentId = (String) result.get("document_id");
                System.out.println("Document submitted successfully!");
                System.out.println("Document ID: " + documentId);
                
                // Poll for results
                System.out.println("Polling for results...");
                while (true) {
                    Map<String, Object> response = sdk.getDocumentResult(documentId);
                    
                    Map<String, Object> data = (Map<String, Object>) response.get("data");
                    if (data != null && data.containsKey("fhir") && data.containsKey("output")) {
                        System.out.println("\nProcessing completed!");
                        System.out.println("FHIR data: " + data.get("fhir"));
                        System.out.println("Output data: " + data.get("output"));
                        break;
                    }
                    
                    System.out.print(".");
                    Thread.sleep(10000);
                }
            } else {
                System.out.println("No file specified. Use --file=<path> to process a document.");
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
```

## Using Apache Commons CLI (Advanced)

For more sophisticated command-line argument parsing, add this dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>commons-cli</groupId>
    <artifactId>commons-cli</artifactId>
    <version>1.5.0</version>
</dependency>
```

Then create `AdvancedCliRunner.java`:

```java
package com.example.ekacare;

import com.example.ekacare.sdk.EkaCareSDK;
import org.apache.commons.cli.*;

import java.util.Map;

public class AdvancedCliRunner {
    
    public static void main(String[] args) {
        Options options = new Options();
        
        // Define command-line options
        options.addOption(Option.builder("i")
                .longOpt("client-id")
                .hasArg()
                .required()
                .desc("Client ID for authentication")
                .build());
        
        options.addOption(Option.builder("s")
                .longOpt("client-secret")
                .hasArg()
                .required()
                .desc("Client secret for authentication")
                .build());
        
        options.addOption(Option.builder("f")
                .longOpt("file")
                .hasArg()
                .desc("File path to process")
                .build());
        
        options.addOption(Option.builder("t")
                .longOpt("task")
                .hasArg()
                .desc("Task type: smart, pii, or both (default: smart)")
                .build());
        
        options.addOption(Option.builder("h")
                .longOpt("help")
                .desc("Print this help message")
                .build());
        
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        
        try {
            CommandLine cmd = parser.parse(options, args);
            
            // Show help if requested
            if (cmd.hasOption("help")) {
                formatter.printHelp("ekacare-cli", options);
                return;
            }
            
            String clientId = cmd.getOptionValue("client-id");
            String clientSecret = cmd.getOptionValue("client-secret");
            String filePath = cmd.getOptionValue("file");
            String task = cmd.getOptionValue("task", "smart");
            
            // Initialize SDK
            System.out.println("Authenticating...");
            EkaCareSDK sdk = new EkaCareSDK(clientId, clientSecret);
            System.out.println("✓ Authentication successful!");
            
            if (filePath != null) {
                System.out.println("Processing: " + filePath);
                Map<String, Object> result = sdk.processDocument(filePath, task);
                
                String documentId = (String) result.get("document_id");
                System.out.println("✓ Document submitted: " + documentId);
                
                // Poll for results
                System.out.print("Waiting for results");
                while (true) {
                    Map<String, Object> response = sdk.getDocumentResult(documentId);
                    
                    Map<String, Object> data = (Map<String, Object>) response.get("data");
                    if (data != null && data.containsKey("fhir") && data.containsKey("output")) {
                        System.out.println("\n✓ Processing completed!");
                        System.out.println("\nFHIR data: " + data.get("fhir"));
                        System.out.println("\nOutput data: " + data.get("output"));
                        break;
                    }
                    
                    System.out.print(".");
                    Thread.sleep(10000);
                }
            }
            
        } catch (ParseException e) {
            System.err.println("Error parsing arguments: " + e.getMessage());
            formatter.printHelp("ekacare-cli", options);
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
```

Run with:
```bash
java AdvancedCliRunner -i YOUR_CLIENT_ID -s YOUR_CLIENT_SECRET -f /path/to/file.jpg -t smart
```

Or get help:
```bash
java AdvancedCliRunner --help
```

## Environment Variables (Recommended for Production)

Instead of passing credentials on command line, use environment variables:

```java
package com.example.ekacare;

import com.example.ekacare.sdk.EkaCareSDK;

public class EnvRunner {
    public static void main(String[] args) {
        // Read from environment variables
        String clientId = System.getenv("EKACARE_CLIENT_ID");
        String clientSecret = System.getenv("EKACARE_CLIENT_SECRET");
        
        if (clientId == null || clientSecret == null) {
            System.err.println("Please set EKACARE_CLIENT_ID and EKACARE_CLIENT_SECRET environment variables");
            System.exit(1);
        }
        
        EkaCareSDK sdk = new EkaCareSDK(clientId, clientSecret);
        // ... rest of your code
    }
}
```

Set environment variables:

**Linux/Mac:**
```bash
export EKACARE_CLIENT_ID="your_client_id"
export EKACARE_CLIENT_SECRET="your_client_secret"
java EnvRunner
```

**Windows:**
```cmd
set EKACARE_CLIENT_ID=your_client_id
set EKACARE_CLIENT_SECRET=your_client_secret
java EnvRunner
```

## Creating an Executable JAR

To create a standalone executable JAR, update your `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <mainClass>com.example.ekacare.CliRunner</mainClass>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Build:
```bash
mvn clean package
```

Run:
```bash
java -jar target/ekacare-sdk-1.0.0.jar --client-id=YOUR_ID --client-secret=YOUR_SECRET --file=/path/to/file.jpg
```
