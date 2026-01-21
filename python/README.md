# Eka Care Medical Records API SDK - Python

A well-structured Python SDK for interacting with the Eka Care API for medical document processing.

[![Python Version](https://img.shields.io/badge/python-3.8%2B-blue)](https://www.python.org/downloads/)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

## 📋 Table of Contents

- [Features](#features)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Usage Examples](#usage-examples)
- [CLI Usage](#cli-usage)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Development](#development)
- [Testing](#testing)
- [Contributing](#contributing)

## ✨ Features

- 🔐 **Automatic Authentication** - Handles OAuth token management automatically
- 📄 **Document Processing** - Upload and process medical documents
- 🔄 **Polling Support** - Built-in polling for processing results
- ⚙️ **Flexible Configuration** - Support for environment variables and config files
- 🖥️ **CLI Tool** - Command-line interface for quick operations
- 📦 **Type Hints** - Full type annotation support
- 🧪 **Context Manager** - Proper resource management with `with` statement
- 🎯 **Error Handling** - Custom exceptions for different error cases

## 📦 Installation

### Using pip (Recommended)

```bash
pip install ekacare-sdk
```

### From source

```bash
git clone https://github.com/eka-care/ekacare-python-sdk.git
cd ekacare-python-sdk
pip install -e .
```

### Development installation

```bash
pip install -e ".[dev]"
```

## 🚀 Quick Start

### Basic Usage

```python
from ekacare_sdk import EkaCareSDK

# Initialize SDK
sdk = EkaCareSDK(
    client_id="your_client_id",
    client_secret="your_client_secret"
)

# Process a document
result = sdk.process_document(
    "/path/to/lab_report.jpg",
    task="smart"
)

print(f"Document ID: {result['document_id']}")

# Get results
response = sdk.get_document_result(result['document_id'])
print(response['data'])

# Clean up
sdk.close()
```

### Using Context Manager (Recommended)

```python
from ekacare_sdk import EkaCareSDK

with EkaCareSDK("client_id", "client_secret") as sdk:
    # Process and wait for completion
    result = sdk.process_and_wait(
        "/path/to/document.jpg",
        task="smart"
    )
    print(result['data'])

# SDK automatically closed here
```

## 📚 Usage Examples

### 1. Simple Document Processing

```python
from ekacare_sdk import EkaCareSDK

sdk = EkaCareSDK("client_id", "client_secret")

# Submit document
result = sdk.process_document("/path/to/file.jpg", task="smart")
document_id = result["document_id"]

# Check status
status = sdk.get_document_result(document_id)
print(status)
```

### 2. Using Environment Variables

```python
import os
from ekacare_sdk import EkaCareSDK
from ekacare_sdk.config import EkaCareConfig

# Set environment variables
os.environ["EKACARE_CLIENT_ID"] = "your_id"
os.environ["EKACARE_CLIENT_SECRET"] = "your_secret"

# Load from environment
config = EkaCareConfig.from_env()
sdk = EkaCareSDK(config.client_id, config.client_secret)

# Use SDK
result = sdk.process_document("/path/to/file.jpg")
```

## 🖥️ CLI Usage

The SDK includes a command-line interface for quick operations.

### Installation

After installing the package, the `ekacare-cli` command will be available:

```bash
pip install ekacare-sdk
```

### Basic Commands

```bash
# Process a document
ekacare-cli --client-id YOUR_ID --client-secret YOUR_SECRET --file document.jpg

# Use environment variables
export EKACARE_CLIENT_ID=your_id
export EKACARE_CLIENT_SECRET=your_secret
ekacare-cli --file document.jpg

# Different tasks
ekacare-cli --file document.pdf --task pii
ekacare-cli --file document.jpg --task both

# Submit without waiting
ekacare-cli --file document.jpg --no-wait

# Verbose output
ekacare-cli --file document.jpg --verbose

# JSON output
ekacare-cli --file document.jpg --json

# Custom polling
ekacare-cli --file document.jpg --poll-interval 5 --timeout 120
```

### CLI Help

```bash
ekacare-cli --help
```

## 📁 Project Structure

```
ekacare-python-sdk/
├── ekacare_sdk/
│   ├── __init__.py          # Package initialization
│   ├── sdk.py               # Main SDK class
│   ├── config.py            # Configuration management
│   ├── exceptions.py        # Custom exceptions
│   └── cli.py               # Command-line interface
├── examples/
│   └── usage_examples.py    # Example scripts
├── tests/
│   ├── test_sdk.py          # SDK tests
│   ├── test_config.py       # Config tests
│   └── test_cli.py          # CLI tests
├── docs/
│   └── api.md               # API documentation
├── .env.example             # Environment variables template
├── .gitignore               # Git ignore rules
├── setup.py                 # Package setup
├── requirements.txt         # Dependencies
├── requirements-dev.txt     # Development dependencies
├── README.md                # This file
└── LICENSE                  # License file
```

## ⚙️ Configuration

### Environment Variables

Create a `.env` file (copy from `.env.example`):

```bash
EKACARE_CLIENT_ID=your_client_id
EKACARE_CLIENT_SECRET=your_client_secret
EKACARE_BASE_URL=https://api.eka.care  # Optional
```

### Using Configuration Class

```python
from ekacare_sdk.config import EkaCareConfig

# From environment
config = EkaCareConfig.from_env()

# Manual configuration
config = EkaCareConfig(
    client_id="your_id",
    client_secret="your_secret",
    base_url="https://api.eka.care"
)
```

## 📖 API Reference

### EkaCareSDK

Main SDK class for interacting with Eka Care API.

#### Methods

**`__init__(client_id, client_secret, base_url="https://api.eka.care")`**
- Initialize SDK with credentials
- Automatically authenticates and obtains access token

**`process_document(file_path, doc_type="lr", task="smart")`**
- Upload and process a document
- Returns: `dict` with `document_id` and `status`
- Raises: `FileNotFoundError`, `ValueError`

**`get_document_result(document_id)`**
- Get processing result for a document
- Returns: `dict` with processing status and data
- Raises: `requests.exceptions.RequestException`

**`close()`**
- Close the underlying HTTP session

### Task Options

- `"smart"` - Smart processing (default)
- `"pii"` - PII (Personal Identifiable Information) detection
- `"both"` - Both smart and PII processing

## 🛠️ Development

### Setup Development Environment

```bash
# Clone repository
git clone https://github.com/eka-care/reports-parsing-sdk.git
cd reports-parsing-sdk/python

# Create virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install development dependencies
pip install -e ".[dev]"
```

### Code Quality

```bash
# Format code
black ekacare_sdk/

# Sort imports
isort ekacare_sdk/

# Lint
flake8 ekacare_sdk/
pylint ekacare_sdk/

# Type check
mypy ekacare_sdk/
```

## 🧪 Testing

```bash
# Run all tests
pytest

# Run with coverage
pytest --cov=ekacare_sdk --cov-report=html

# Run specific test file
pytest tests/test_sdk.py

# Run with verbose output
pytest -v
```

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Run tests and linting
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Documentation**: [https://docs.eka.care](https://docs.eka.care)
- **Issues**: [GitHub Issues](https://github.com/eka-care/ekacare-python-sdk/issues)
- **Email**: support@eka.care

## 🙏 Acknowledgments

- Eka Care team for API development
- Contributors and users of this SDK

---

Made with ❤️ by [Eka Care](https://eka.care)
