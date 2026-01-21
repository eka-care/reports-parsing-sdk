#!/usr/bin/env python3
"""
Command-line interface for Eka Care SDK.

Usage:
    ekacare-cli --client-id YOUR_ID --client-secret YOUR_SECRET --file /path/to/file.jpg
    ekacare-cli --help
"""

import argparse
import sys
import time
from pathlib import Path
from typing import Optional

from .sdk import EkaCareSDK
from .config import EkaCareConfig


def create_parser() -> argparse.ArgumentParser:
    """Create command-line argument parser."""
    parser = argparse.ArgumentParser(
        description="Eka Care Medical Records API CLI",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Process a document with credentials
  %(prog)s --client-id YOUR_ID --client-secret YOUR_SECRET --file lab_report.jpg
  
  # Use environment variables for credentials
  export EKACARE_CLIENT_ID=your_id
  export EKACARE_CLIENT_SECRET=your_secret
  %(prog)s --file lab_report.jpg
  
  # Process with PII detection
  %(prog)s --file document.pdf --task pii
  
  # Process and don't wait for results
  %(prog)s --file document.jpg --no-wait
        """
    )
    
    # Authentication
    auth_group = parser.add_argument_group("Authentication")
    auth_group.add_argument(
        "--client-id",
        help="Client ID for authentication (or set EKACARE_CLIENT_ID env var)"
    )
    auth_group.add_argument(
        "--client-secret",
        help="Client secret for authentication (or set EKACARE_CLIENT_SECRET env var)"
    )
    auth_group.add_argument(
        "--base-url",
        default="https://api.eka.care",
        help="Base URL for the API (default: https://api.eka.care)"
    )
    
    # Document processing
    doc_group = parser.add_argument_group("Document Processing")
    doc_group.add_argument(
        "-f", "--file",
        type=Path,
        help="Path to the file to process"
    )
    doc_group.add_argument(
        "-t", "--task",
        choices=["smart", "pii", "both"],
        default="smart",
        help="Processing task (default: smart)"
    )
    doc_group.add_argument(
        "--doc-type",
        default="lr",
        help="Document type (default: lr for lab report)"
    )
    
    # Polling options
    poll_group = parser.add_argument_group("Polling Options")
    poll_group.add_argument(
        "--no-wait",
        action="store_true",
        help="Don't wait for processing to complete"
    )
    poll_group.add_argument(
        "--poll-interval",
        type=int,
        default=10,
        help="Seconds between polling attempts (default: 10)"
    )
    poll_group.add_argument(
        "--timeout",
        type=int,
        default=300,
        help="Maximum seconds to wait for completion (default: 300)"
    )
    
    # Output options
    output_group = parser.add_argument_group("Output Options")
    output_group.add_argument(
        "-v", "--verbose",
        action="store_true",
        help="Verbose output"
    )
    output_group.add_argument(
        "-q", "--quiet",
        action="store_true",
        help="Quiet mode (only show errors)"
    )
    output_group.add_argument(
        "--json",
        action="store_true",
        help="Output results as JSON"
    )
    
    return parser


def print_result(result: dict, verbose: bool = False, as_json: bool = False) -> None:
    """Print processing result."""
    if as_json:
        import json
        print(json.dumps(result, indent=2))
        return
    
    data = result.get("data", {})
    
    if verbose:
        print("\n" + "="*60)
        print("PROCESSING RESULT")
        print("="*60)
        print(f"Status: {result.get('status', 'unknown')}")
        
        if data.get("fhir"):
            print("\nFHIR Data:")
            print("-" * 60)
            import json
            print(json.dumps(data["fhir"], indent=2))
        
        if data.get("output"):
            print("\nOutput Data:")
            print("-" * 60)
            print(json.dumps(data["output"], indent=2))
    else:
        print(f"\n✓ Processing completed successfully!")
        if data.get("fhir"):
            print(f"  FHIR data available: Yes")
        if data.get("output"):
            print(f"  Output data available: Yes")


def main(argv: Optional[list] = None) -> int:
    """Main CLI entry point."""
    parser = create_parser()
    args = parser.parse_args(argv)
    
    # Get credentials from args or environment
    try:
        if args.client_id and args.client_secret:
            client_id = args.client_id
            client_secret = args.client_secret
        else:
            config = EkaCareConfig.from_env(base_url=args.base_url)
            client_id = config.client_id
            client_secret = config.client_secret
    except ValueError as e:
        print(f"Error: {e}", file=sys.stderr)
        print("\nProvide credentials via arguments or environment variables:", file=sys.stderr)
        print("  --client-id and --client-secret", file=sys.stderr)
        print("  OR", file=sys.stderr)
        print("  EKACARE_CLIENT_ID and EKACARE_CLIENT_SECRET", file=sys.stderr)
        return 1
    
    # Check if file is provided
    if not args.file:
        print("Error: --file argument is required", file=sys.stderr)
        parser.print_help()
        return 1
    
    # Validate file exists
    if not args.file.exists():
        print(f"Error: File not found: {args.file}", file=sys.stderr)
        return 1
    
    try:
        # Initialize SDK
        if not args.quiet:
            print("Authenticating...", end=" ", flush=True)
        
        sdk = EkaCareSDK(client_id, client_secret, args.base_url)
        
        if not args.quiet:
            print("✓")
        
        # Process document
        if not args.quiet:
            print(f"Processing document: {args.file.name}...", end=" ", flush=True)
        
        result = sdk.process_document(
            str(args.file),
            doc_type=args.doc_type,
            task=args.task
        )
        
        document_id = result["document_id"]
        
        if not args.quiet:
            print("✓")
            print(f"Document ID: {document_id}")
        
        # Wait for results if requested
        if not args.no_wait:
            if not args.quiet:
                print("\nWaiting for processing to complete", end="", flush=True)
            
            start_time = time.time()
            while True:
                # Check timeout
                elapsed = time.time() - start_time
                if elapsed > args.timeout:
                    print(f"\n\nError: Processing timed out after {args.timeout} seconds", file=sys.stderr)
                    return 1
                
                # Get result
                result = sdk.get_document_result(document_id)
                data = result.get("data", {})
                
                # Check if complete
                if data.get("fhir") and data.get("output"):
                    if not args.quiet:
                        print()  # New line after dots
                    print_result(result, verbose=args.verbose, as_json=args.json)
                    break
                
                # Check if failed
                if result.get("status") == "failed":
                    print(f"\n\nError: Document processing failed", file=sys.stderr)
                    return 1
                
                # Wait and show progress
                if not args.quiet:
                    print(".", end="", flush=True)
                time.sleep(args.poll_interval)
        else:
            # Just print document ID
            if args.json:
                import json
                print(json.dumps({"document_id": document_id}, indent=2))
            else:
                print(f"\nDocument submitted. Use document ID to check status: {document_id}")
        
        return 0
        
    except KeyboardInterrupt:
        print("\n\nInterrupted by user", file=sys.stderr)
        return 130
    except Exception as e:
        print(f"\nError: {e}", file=sys.stderr)
        if args.verbose:
            import traceback
            traceback.print_exc()
        return 1
    finally:
        sdk.close()


if __name__ == "__main__":
    sys.exit(main())
