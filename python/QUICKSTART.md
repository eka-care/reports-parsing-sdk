# 🚀 Python SDK Quick Start Guide

Get started with the Eka Care Python SDK in 5 minutes!

## Prerequisites

- Python 3.8 or higher
- pip (Python package installer)
- Eka Care API credentials (client_id and client_secret)

## Installation

### Step 1: Install Python (if needed)

**Check if Python is installed:**
```bash
python --version
# or
python3 --version
```

**If not installed:**
- **Windows**: Download from [python.org](https://www.python.org/downloads/)
- **Mac**: `brew install python3`
- **Linux**: `sudo apt-get install python3 python3-pip`

### Step 2: Install the SDK

```bash
pip install ekacare-sdk
```

Or from source:
```bash
git clone https://github.com/eka-care/ekacare-python-sdk.git
cd ekacare-python-sdk
pip install -e .
```

## Your First Script

### Method 1: Simple Script

Create `my_first_script.py`:

```python
from ekacare_sdk import EkaCareSDK

# Initialize SDK
sdk = EkaCareSDK(
    client_id="YOUR_CLIENT_ID",
    client_secret="YOUR_CLIENT_SECRET"
)

# Process a document
result = sdk.process_document(
    "/path/to/your/document.jpg",
    task="smart"
)

print(f"Success! Document ID: {result['document_id']}")

# Clean up
sdk.close()
```

Run it:
```bash
python my_first_script.py
```

### Method 2: With Context Manager (Better!)

Create `better_script.py`:

```python
from ekacare_sdk import EkaCareSDK

# Use 'with' for automatic cleanup
with EkaCareSDK("YOUR_CLIENT_ID", "YOUR_CLIENT_SECRET") as sdk:
    # Process and wait for results
    result = sdk.process_and_wait(
        "/path/to/document.jpg",
        task="smart"
    )
    
    print("✓ Processing complete!")
    print(f"FHIR data: {result['data'].get('fhir')}")
    print(f"Output: {result['data'].get('output')}")

# SDK automatically closed!
```

## Using Environment Variables (Recommended)

### Step 1: Create `.env` file

```bash
# Create the file
touch .env

# Add your credentials
echo "EKACARE_CLIENT_ID=your_client_id" >> .env
echo "EKACARE_CLIENT_SECRET=your_client_secret" >> .env
```

### Step 2: Use in your script

```python
import os
from ekacare_sdk import EkaCareSDK
from ekacare_sdk.config import EkaCareConfig

# Load from environment
config = EkaCareConfig.from_env()

# Create SDK
with EkaCareSDK(config.client_id, config.client_secret) as sdk:
    result = sdk.process_and_wait("/path/to/file.jpg")
    print("Done!", result)
```

**For Linux/Mac:**
```bash
export EKACARE_CLIENT_ID=your_id
export EKACARE_CLIENT_SECRET=your_secret
python your_script.py
```

**For Windows:**
```cmd
set EKACARE_CLIENT_ID=your_id
set EKACARE_CLIENT_SECRET=your_secret
python your_script.py
```

## Using the Command Line

### Quick Command

```bash
ekacare-cli \
  --client-id YOUR_ID \
  --client-secret YOUR_SECRET \
  --file /path/to/document.jpg
```

### With Environment Variables

```bash
# Set once
export EKACARE_CLIENT_ID=your_id
export EKACARE_CLIENT_SECRET=your_secret

# Use many times
ekacare-cli --file document1.jpg
ekacare-cli --file document2.jpg
ekacare-cli --file document3.jpg
```

### Advanced CLI Options

```bash
# Different tasks
ekacare-cli --file doc.jpg --task pii
ekacare-cli --file doc.jpg --task both

# Don't wait for results
ekacare-cli --file doc.jpg --no-wait

# Faster polling
ekacare-cli --file doc.jpg --poll-interval 5

# Verbose output
ekacare-cli --file doc.jpg --verbose

# JSON output
ekacare-cli --file doc.jpg --json > result.json
```

## Common Use Cases

### 1. Process a Single Document

```python
from ekacare_sdk import EkaCareSDK

with EkaCareSDK("client_id", "client_secret") as sdk:
    result = sdk.process_and_wait("document.jpg")
    
    # Access the data
    fhir_data = result['data']['fhir']
    output_data = result['data']['output']
    
    print(f"Processing complete: {fhir_data}")
```

### 2. Process Multiple Documents

```python
from pathlib import Path
from ekacare_sdk import EkaCareSDK

files = ["doc1.jpg", "doc2.jpg", "doc3.jpg"]

with EkaCareSDK("client_id", "client_secret") as sdk:
    for file in files:
        try:
            result = sdk.process_and_wait(file)
            print(f"✓ {file}: Success")
        except Exception as e:
            print(f"✗ {file}: {e}")
```

### 3. Submit and Poll Manually

```python
import time
from ekacare_sdk import EkaCareSDK

with EkaCareSDK("client_id", "client_secret") as sdk:
    # Submit
    result = sdk.process_document("document.jpg")
    doc_id = result['document_id']
    print(f"Submitted: {doc_id}")
    
    # Poll manually
    while True:
        response = sdk.get_document_result(doc_id)
        data = response.get('data', {})
        
        if data.get('fhir') and data.get('output'):
            print("Complete!")
            break
        
        print("Still processing...")
        time.sleep(10)
```

### 4. Error Handling

```python
from ekacare_sdk import EkaCareSDK

try:
    with EkaCareSDK("client_id", "client_secret") as sdk:
        result = sdk.process_and_wait("document.jpg", timeout=60)
        print("Success!")
        
except FileNotFoundError:
    print("Error: File not found")
except TimeoutError:
    print("Error: Processing took too long")
except Exception as e:
    print(f"Error: {e}")
```

## Directory Structure for Your Project

Organize your project like this:

```
my_project/
├── .env                    # Your credentials (don't commit!)
├── .gitignore             # Git ignore file
├── requirements.txt        # Dependencies
├── documents/             # Input documents
│   ├── lab_report1.jpg
│   └── lab_report2.jpg
└── scripts/
    ├── process_single.py  # Process one document
    ├── process_batch.py   # Process multiple documents
    └── check_status.py    # Check processing status
```

**requirements.txt:**
```
ekacare-sdk>=1.0.0
```

**.gitignore:**
```
.env
*.pyc
__pycache__/
documents/*.jpg
results/
```

## Troubleshooting

### Problem: "ModuleNotFoundError: No module named 'ekacare_sdk'"

**Solution:**
```bash
pip install ekacare-sdk
# or
pip install -e .  # if installing from source
```

### Problem: "Authentication failed"

**Solution:**
- Check your client_id and client_secret are correct
- Make sure they're not expired
- Verify you have network connectivity

### Problem: "File not found"

**Solution:**
```python
from pathlib import Path

# Use absolute paths
file_path = Path("/full/path/to/file.jpg")
if not file_path.exists():
    print(f"File not found: {file_path}")
else:
    sdk.process_document(str(file_path))
```

### Problem: "Processing timeout"

**Solution:**
```python
# Increase timeout
result = sdk.process_and_wait(
    "large_file.pdf",
    timeout=600  # 10 minutes instead of default 5
)
```

## Next Steps

1. ✅ Read the full [README](README_PYTHON.md)
2. ✅ Check out [usage examples](examples/usage_examples.py)
3. ✅ Explore the [API documentation](docs/api.md)
4. ✅ Join our community

## Getting Help

- 📚 Documentation: https://docs.eka.care
- 💬 GitHub Issues: https://github.com/eka-care/ekacare-python-sdk/issues
- 📧 Email: support@eka.care

---

Happy coding! 🎉
