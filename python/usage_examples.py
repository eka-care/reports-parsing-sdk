"""
Example usage scripts for Eka Care SDK.
"""

import time
from ekacare_sdk import EkaCareSDK


def example_basic_usage(client_id: str, client_secret: str, file_path: str):
    """Basic usage example."""
    print("="*60)
    print("Example 1: Basic Document Processing")
    print("="*60)
    
    # Initialize SDK
    sdk = EkaCareSDK(
        client_id=client_id,
        client_secret=client_secret,
    )
    
    # Process a document
    result = sdk.process_document(
        file_path,
        task="smart"
    )
    
    print(f"Document ID: {result['document_id']}")
    print(f"Status: {result.get('status', 'unknown')}")


def example_with_polling(client_id: str, client_secret: str, file_path: str):
    """Example with polling for results."""
    print("\n" + "="*60)
    print("Example 2: Process and Wait for Results")
    print("="*60)
    
    sdk = EkaCareSDK(
        client_id=client_id,
        client_secret=client_secret,
    )
    
    # Submit document
    result = sdk.process_document(file_path, task="smart")
    document_id = result["document_id"]
    
    print(f"Document submitted: {document_id}")
    print("Polling for results...\n")
    
    # Poll for results
    while True:
        response = sdk.get_document_result(document_id)
        data = response.get("data", {})
        
        if data.get("fhir") and data.get("output"):
            print("✓ Processing completed!")
            print(f"\nFHIR data: {data['fhir']}")
            print(f"Output data: {data['output']}")
            break
        
        print("Still processing...", end="\r")
        time.sleep(10)
    
    sdk.close()


if __name__ == "__main__":
    print("\nEka Care SDK - Example Usage\n")
    print("These are example snippets. Update credentials and file paths before running.\n")
    
    import argparse

    parser = argparse.ArgumentParser(description="Eka Care SDK Example")
    parser.add_argument("--client-id", required=True, help="Client ID for authentication")
    parser.add_argument("--client-secret", required=True, help="Client secret for authentication")
    parser.add_argument("--file-path", required=True, help="file path")
    args = parser.parse_args()

    # Uncomment the example you want to run:
    # example_basic_usage(args.client_id, args.client_secret, args.file_path)
    example_with_polling(args.client_id, args.client_secret, args.file_path)
