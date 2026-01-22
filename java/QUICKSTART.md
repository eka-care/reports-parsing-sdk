# 🚀 Quick Start Guide for Java Beginners

## What You'll Learn
By following this guide, you'll learn how to:
- Set up a Java Spring Boot project
- Use the Eka Care SDK to process medical documents
- Create a simple REST API

**Estimated Time:** 30 minutes

---

## Step-by-Step Setup

### 1️⃣ Install Java (if you haven't already)

**For Windows:**
1. Download Java JDK 17 from: https://www.oracle.com/java/technologies/downloads/#java17
2. Run the installer
3. Open Command Prompt and type: `java -version`
4. You should see: `java version "17.x.x"`

**For Mac:**
```bash
brew install openjdk@17
```

**For Linux:**
```bash
sudo apt install openjdk-17-jdk
```

### 2️⃣ Install Maven

**For Windows:**
1. Download Maven from: https://maven.apache.org/download.cgi
2. Extract to `C:\Program Files\Maven`
3. Add to PATH environment variable
4. Test: `mvn -version`

**For Mac:**
```bash
brew install maven
```

**For Linux:**
```bash
sudo apt install maven
```

### 3️⃣ Choose Your IDE

**Recommended: IntelliJ IDEA Community Edition (FREE)**
1. Download from: https://www.jetbrains.com/idea/download/
2. Install and open IntelliJ IDEA

---

## Create Your First Project

### Option 1: Using IntelliJ IDEA (Easiest!)

1. **Open IntelliJ IDEA**

2. **Create New Project**
   - Click "New Project"
   - Select "Spring Initializr" from the left menu
   - Click "Next"

3. **Configure Project**
   - Name: `ekacare-demo`
   - Group: `com.example`
   - Artifact: `ekacare-demo`
   - Type: Maven
   - Language: Java
   - Java Version: 17
   - Click "Next"

4. **Add Dependencies**
   - Search and add: "Spring Web"
   - Click "Create"

5. **Add SDK Files**
   - Right-click on `src/main/java/com/example/ekacaredemo`
   - Select "New" → "Package"
   - Create packages: `sdk`, `service`, `controller`
   - Copy the Java files into these packages

### Option 2: Using Command Line

```bash
# 1. Create project directory
mkdir ekacare-demo
cd ekacare-demo

# 2. Create standard Maven structure
mkdir -p src/main/java/com/example/ekacare
mkdir -p src/main/resources

# 3. Copy pom.xml to project root

# 4. Copy all Java files to src/main/java/com/example/ekacare

# 5. Build the project
mvn clean install
```

---

## Your First API Call

### Method 1: Using the Main Class (Simplest)

Create `Main.java`:

```java
package com.example.ekacare;

import com.example.ekacare.sdk.EkaCareSDK;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {
        // Use try-with-resources for automatic cleanup
        try (EkaCareSDK sdk = new EkaCareSDK(
                "YOUR_CLIENT_ID",
                "YOUR_CLIENT_SECRET")) {

            System.out.println("Authenticating... Done!");

            // Process document and wait for completion
            System.out.println("Processing document...");
            Map<String, Object> result = sdk.processAndWait(
                "/path/to/your/document.jpg",
                "smart",
                10,   // poll every 10 seconds
                300   // timeout after 5 minutes
            );

            // Print result
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            System.out.println("Processing complete!");
            System.out.println("FHIR data: " + data.get("fhir"));
            System.out.println("Output data: " + data.get("output"));
        }
    }
}
```

**Run it:**
- Right-click on `Main.java`
- Select "Run 'Main.main()'"

### Method 2: Using Spring Boot Application

1. **Update application.properties**
   ```properties
   ekacare.client.id=YOUR_CLIENT_ID
   ekacare.client.secret=YOUR_CLIENT_SECRET
   server.port=8080
   ```

2. **Run the application**
   - Right-click on `EkaCareExampleApplication.java`
   - Select "Run 'EkaCareExampleApplication'"
   - Or in terminal: `mvn spring-boot:run`

3. **Test with curl or Postman**
   ```bash
   curl -X POST http://localhost:8080/api/documents/process?task=smart \
     -F "file=@/path/to/document.jpg"
   ```

---

## Understanding the Code

### The SDK Class (EkaCareSDK.java)

```java
// This is the main class that talks to Eka Care API
// It automatically handles authentication and implements AutoCloseable
try (EkaCareSDK sdk = new EkaCareSDK("client_id", "client_secret")) {

    // Option 1: Process and wait for completion (recommended)
    Map<String, Object> result = sdk.processAndWait(filePath, "smart", 10, 300);

    // Option 2: Upload a document manually
    Map<String, Object> submitResult = sdk.processDocument(filePath, "smart");
    String documentId = (String) submitResult.get("document_id");

    // Then check the result
    Map<String, Object> status = sdk.getDocumentResult(documentId);
}
```

### The Service Class (EkaCareService.java)

```java
// This wraps the SDK for use in Spring Boot
@Autowired
private EkaCareService service;

// Use it in your code
Map<String, Object> result = service.submitDocument(filePath, "smart");
```

### The Controller Class (DocumentController.java)

```java
// This creates REST API endpoints
// POST /api/documents/process
// GET /api/documents/{id}/result
```

---

## Common Beginner Mistakes & Solutions

### ❌ Mistake 1: Wrong File Path
```java
// WRONG
sdk.processDocument("document.jpg", "smart");

// CORRECT - Use absolute path
sdk.processDocument("/Users/john/Desktop/document.jpg", "smart");

// OR - Use relative path from project root
sdk.processDocument("./files/document.jpg", "smart");
```

### ❌ Mistake 2: Forgot to Update Credentials
```java
// Check your application.properties file
// Make sure the credentials are correct
ekacare.client.id=YOUR_REAL_CLIENT_ID
ekacare.client.secret=YOUR_REAL_CLIENT_SECRET
```

### ❌ Mistake 3: File Not Found
```java
// Always check if file exists first
File file = new File(filePath);
if (file.exists()) {
    sdk.processDocument(filePath, "smart");
} else {
    System.out.println("File not found!");
}
```

---

## Testing Your API

### Using curl (Command Line)

```bash
# Process a document
curl -X POST http://localhost:8080/api/documents/process?task=smart \
  -F "file=@./test.jpg"

# Check result (replace abc123 with your document ID)
curl http://localhost:8080/api/documents/abc123/result
```

### Using Postman (GUI Tool)

1. Download Postman: https://www.postman.com/downloads/
2. Create new POST request
3. URL: `http://localhost:8080/api/documents/process?task=smart`
4. Body → form-data
5. Key: `file`, Type: File
6. Select your file
7. Click "Send"

### Using Browser (for testing)

Create `test.html` in your project:
```html
<!DOCTYPE html>
<html>
<body>
    <h2>Upload Document</h2>
    <form action="http://localhost:8080/api/documents/process?task=smart" 
          method="post" 
          enctype="multipart/form-data">
        <input type="file" name="file">
        <button type="submit">Upload</button>
    </form>
</body>
</html>
```
Open this file in your browser!

---

## Next Steps

1. ✅ **Understand the basics** - Read through the code comments
2. ✅ **Modify the examples** - Try changing parameters
3. ✅ **Build a simple UI** - Create an HTML form to upload files
4. ✅ **Add error handling** - Make your code more robust
5. ✅ **Learn more Spring Boot** - https://spring.io/guides

---

## Need Help?

**Java Basics:**
- [Java for Complete Beginners](https://www.youtube.com/watch?v=eIrMbAQSU34)
- [W3Schools Java Tutorial](https://www.w3schools.com/java/)

**Spring Boot:**
- [Spring Boot in 100 Seconds](https://www.youtube.com/watch?v=msXL2oDexqw)
- [Spring Boot Tutorial](https://spring.io/guides/gs/spring-boot/)

**REST APIs:**
- [REST API Crash Course](https://www.youtube.com/watch?v=-mN3VyJuCjM)

---

## Congratulations! 🎉

You now have a working Eka Care SDK in Java Spring Boot!
