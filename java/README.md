# Eka Care Medical Records API SDK - Java Spring Boot

A Java Spring Boot SDK for interacting with the Eka Care API for medical document processing.

## 📋 Table of Contents
- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Installation & Setup](#installation--setup)
- [How to Use](#how-to-use)
- [API Endpoints](#api-endpoints)
- [Code Examples](#code-examples)
- [Troubleshooting](#troubleshooting)

## 🔧 Prerequisites

Before you start, make sure you have:

1. **Java Development Kit (JDK) 17 or higher**
   - Download from: https://www.oracle.com/java/technologies/downloads/
   - Verify installation: `java -version`

2. **Maven** (Build tool)
   - Download from: https://maven.apache.org/download.cgi
   - Verify installation: `mvn -version`

3. **An IDE (Integrated Development Environment)**
   - IntelliJ IDEA (Recommended): https://www.jetbrains.com/idea/download/
   - Eclipse: https://www.eclipse.org/downloads/
   - VS Code with Java extensions

4. **Eka Care API Token**
   - You need a valid bearer token from Eka Care

## 📁 Project Structure

```
ekacare-sdk/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── example/
│       │           └── ekacare/
│       │               ├── sdk/
│       │               │   └── EkaCareSDK.java          # Main SDK class
│       │               ├── service/
│       │               │   └── EkaCareService.java      # Spring service wrapper
│       │               ├── controller/
│       │               │   └── DocumentController.java  # REST API controller
│       │               └── example/
│       │                   └── EkaCareExampleApplication.java
│       └── resources/
│           └── application.properties                    # Configuration
└── pom.xml                                               # Maven dependencies
```

## 🚀 Installation & Setup

### Step 1: Create a New Spring Boot Project

#### Option A: Using Spring Initializr (Easiest for beginners)
1. Go to https://start.spring.io/
2. Configure:
   - Project: **Maven**
   - Language: **Java**
   - Spring Boot: **3.2.0**
   - Java: **17**
   - Dependencies: **Spring Web**
3. Click "Generate" and download the zip
4. Extract the zip file

#### Option B: Using IntelliJ IDEA
1. File → New → Project
2. Select "Spring Initializr"
3. Choose Java 17 and Spring Boot 3.2.0
4. Add dependency: "Spring Web"
5. Create the project

### Step 2: Add the SDK Files

1. Copy all the Java files to your project:
   - `EkaCareSDK.java` → `src/main/java/com/example/ekacare/sdk/`
   - `EkaCareService.java` → `src/main/java/com/example/ekacare/service/`
   - `DocumentController.java` → `src/main/java/com/example/ekacare/controller/`
   - `EkaCareExampleApplication.java` → `src/main/java/com/example/ekacare/example/`

2. Copy configuration files:
   - `application.properties` → `src/main/resources/`
   - `pom.xml` → project root (replace existing file)

### Step 3: Configure Your API Credentials

Open `src/main/resources/application.properties` and update:
```properties
ekacare.client.id=YOUR_CLIENT_ID_HERE
ekacare.client.secret=YOUR_CLIENT_SECRET_HERE
```

**Note:** The SDK will automatically authenticate and obtain an access token using these credentials.

### Step 4: Build the Project

Open terminal in project directory and run:
```bash
mvn clean install
```

## 💻 How to Use

### Method 1: Using the SDK Directly (Simple)

```java
package com.example.ekacare;

import com.example.ekacare.sdk.EkaCareSDK;
import java.util.Map;

public class SimpleExample {
    public static void main(String[] args) throws Exception {
        // 1. Initialize the SDK with client credentials
        EkaCareSDK sdk = new EkaCareSDK(
            "YOUR_CLIENT_ID", 
            "YOUR_CLIENT_SECRET"
        );
        
        // 2. Process a document
        Map<String, Object> result = sdk.processDocument(
            "/path/to/your/lab-report.jpg",
            "smart"
        );
        
        // 3. Get the document ID
        String documentId = (String) result.get("document_id");
        System.out.println("Document ID: " + documentId);
        
        // 4. Poll for results
        while (true) {
            Map<String, Object> response = sdk.getDocumentResult(documentId);
            
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data != null && data.containsKey("fhir") && data.containsKey("output")) {
                System.out.println("Processing complete!");
                System.out.println("FHIR data: " + data.get("fhir"));
                System.out.println("Output data: " + data.get("output"));
                break;
            }
            
            Thread.sleep(10000); // Wait 10 seconds
        }
    }
}
```

### Method 2: Using Spring Boot Service (Recommended)

```java
package com.example.ekacare;

import com.example.ekacare.service.EkaCareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MyDocumentProcessor {
    
    @Autowired
    private EkaCareService ekaCareService;
    
    public void processMyDocument() throws InterruptedException {
        // Process and wait for completion
        Map<String, Object> result = ekaCareService.processDocumentAndWait(
            "/path/to/document.jpg",
            "smart"
        );
        
        System.out.println("Result: " + result);
    }
}
```

### Method 3: Using REST API (For Web Applications)

Start your application:
```bash
mvn spring-boot:run
```

Then use these endpoints:

#### Process a Document (Async)
```bash
curl -X POST http://localhost:8080/api/documents/process?task=smart \
  -F "file=@/path/to/your/document.jpg"
```

Response:
```json
{
  "document_id": "abc123",
  "status": "processing"
}
```

#### Get Document Result
```bash
curl http://localhost:8080/api/documents/abc123/result
```

#### Process and Wait (Sync)
```bash
curl -X POST http://localhost:8080/api/documents/process-sync?task=smart \
  -F "file=@/path/to/your/document.jpg"
```

## 🌐 API Endpoints

When your Spring Boot app is running, you can use these REST endpoints:

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/documents/process?task=smart` | Upload and process document (async) |
| GET | `/api/documents/{id}/result` | Get processing result |
| POST | `/api/documents/process-sync?task=smart` | Upload and process document (wait for completion) |

**Task Options:**
- `smart` - Smart processing (default)
- `pii` - PII (Personal Identifiable Information) detection
- `both` - Both smart and PII processing

## 📝 Code Examples

### Example 1: Process a Single Document

```java
@RestController
public class MyController {
    
    @Autowired
    private EkaCareService ekaCareService;
    
    @GetMapping("/process-lab-report")
    public String processLabReport() {
        try {
            Map<String, Object> result = ekaCareService.submitDocument(
                "/Users/yourname/documents/lab-report.jpg",
                "smart"
            );
            return "Document submitted: " + result.get("document_id");
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
```

### Example 2: Process Multiple Documents

```java
public class BatchProcessor {
    
    @Autowired
    private EkaCareService ekaCareService;
    
    public void processMultipleDocuments(List<String> filePaths) {
        for (String filePath : filePaths) {
            try {
                Map<String, Object> result = ekaCareService.submitDocument(filePath, "smart");
                System.out.println("Submitted: " + result.get("document_id"));
            } catch (Exception e) {
                System.err.println("Failed to process " + filePath + ": " + e.getMessage());
            }
        }
    }
}
```

### Example 3: Web Form Upload

Create an HTML form:
```html
<!DOCTYPE html>
<html>
<body>
    <h2>Upload Medical Document</h2>
    <form action="http://localhost:8080/api/documents/process?task=smart" 
          method="post" 
          enctype="multipart/form-data">
        <input type="file" name="file" accept=".jpg,.jpeg,.png,.pdf">
        <button type="submit">Upload</button>
    </form>
</body>
</html>
```

## 🔍 Troubleshooting

### Common Issues

#### 1. "File not found" Error
```java
// Make sure to use absolute paths
sdk.processDocument("/Users/yourname/documents/file.jpg", "smart");

// Or use relative paths correctly
sdk.processDocument("./src/main/resources/test-file.jpg", "smart");
```

#### 2. "Unauthorized" Error (401)
- Check that your client_id and client_secret are correct in `application.properties`
- Verify your credentials are active and not expired
- Check that authentication endpoint is accessible

#### 3. Maven Build Fails
```bash
# Clean and rebuild
mvn clean install -U
```

#### 4. Port 8080 Already in Use
Change port in `application.properties`:
```properties
server.port=8081
```

#### 5. Large File Upload Fails
Increase limits in `application.properties`:
```properties
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
```

## 🎓 Learning Resources

**New to Java?**
- [Java Tutorial for Beginners](https://www.w3schools.com/java/)
- [Official Java Documentation](https://docs.oracle.com/javase/tutorial/)

**New to Spring Boot?**
- [Spring Boot Getting Started](https://spring.io/guides/gs/spring-boot/)
- [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)

**Understanding REST APIs:**
- [REST API Tutorial](https://restfulapi.net/)

## 📞 Support

- For Eka Care API issues: Contact Eka Care support
- For SDK issues: Check the code examples above
- For Spring Boot issues: Visit https://spring.io/guides

## 📄 License

This SDK is provided as-is for use with Eka Care API services.
