"""
Eka Care Medical Records API SDK

A Python SDK for interacting with the Eka Care API for document processing.
"""

import mimetypes
import requests
import time
from typing import Optional, Literal, Dict, Any
from pathlib import Path


class EkaCareSDK:
    """
    SDK for Eka Care Medical Records API
    
    This class provides methods to interact with the Eka Care API
    for processing medical documents.
    
    Args:
        client_id (str): The client ID for authentication
        client_secret (str): The client secret for authentication
        base_url (str): The base URL for the API (default: https://api.eka.care)
        
    Example:
        >>> sdk = EkaCareSDK("your_client_id", "your_client_secret")
        >>> result = sdk.process_document("/path/to/file.jpg", task="smart")
        >>> print(result["document_id"])
    """
    
    AUTH_ENDPOINT = "/connect-auth/v1/account/login"
    PROCESS_ENDPOINT = "/mr/api/v2/docs"
    RESULT_ENDPOINT = "/mr/api/v1/docs/{document_id}/result"
    
    def __init__(
        self, 
        client_id: str, 
        client_secret: str, 
        base_url: str = "https://api.eka.care"
    ):
        """Initialize the SDK with client credentials."""
        self.client_id = client_id
        self.client_secret = client_secret
        self.base_url = base_url.rstrip('/')
        self._bearer_token: Optional[str] = None
        self._session = requests.Session()
        
        # Authenticate and get token
        self._authenticate()
    
    def _authenticate(self) -> None:
        """
        Get access token from the authentication API.
        
        Raises:
            requests.exceptions.RequestException: If authentication fails
        """
        url = f"{self.base_url}{self.AUTH_ENDPOINT}"
        payload = {
            "client_id": self.client_id,
            "client_secret": self.client_secret
        }
        headers = {"Content-Type": "application/json"}
        
        try:
            response = self._session.post(url, json=payload, headers=headers)
            response.raise_for_status()
            
            data = response.json()
            self._bearer_token = data["access_token"]
            
            # Set authorization header for future requests
            self._session.headers.update({
                "Authorization": f"Bearer {self._bearer_token}"
            })
            
        except requests.exceptions.RequestException as e:
            raise Exception(f"Authentication failed: {str(e)}") from e
        except KeyError:
            raise Exception("Access token not found in authentication response")
    
    def process_document(
        self,
        file_path: str,
        doc_type: Literal["lr"] = "lr",
        task: Literal["smart", "pii", "both"] = "smart"
    ) -> Dict[str, Any]:
        """
        Process a medical document using the Eka Care API.
        
        Args:
            file_path (str): Path to the file to upload
            doc_type (str): Document type (default: "lr" for lab report)
            task (str): Processing task - one of "smart", "pii", or "both" (default: "smart")
        
        Returns:
            dict: The API response containing document_id and status
            
        Raises:
            FileNotFoundError: If the specified file doesn't exist
            ValueError: If task parameter is invalid
            requests.exceptions.RequestException: If the API request fails
            
        Example:
            >>> result = sdk.process_document("/path/to/lab_report.jpg", task="smart")
            >>> print(f"Document ID: {result['document_id']}")
        """
        # Validate file exists
        file_path_obj = Path(file_path)
        if not file_path_obj.exists():
            raise FileNotFoundError(f"File not found: {file_path}")
        
        # Validate task parameter
        valid_tasks = ["smart", "pii", "both"]
        if task not in valid_tasks:
            raise ValueError(f"Invalid task. Must be one of: {', '.join(valid_tasks)}")
        
        # Prepare the API endpoint
        url = f"{self.base_url}{self.PROCESS_ENDPOINT}"
        
        # Build params - handle 'both' case with multiple task parameters
        params = [("dt", doc_type)]
        if task == "both":
            params.append(("task", "smart"))
            params.append(("task", "pii"))
        else:
            params.append(("task", task))
        
        # Prepare the file for upload
        content_type, _ = mimetypes.guess_type(file_path)
        
        with open(file_path, "rb") as file:
            files = {
                "file": (
                    file_path_obj.name, 
                    file, 
                    content_type or "application/octet-stream"
                )
            }
            
            # Make the API request
            response = self._session.post(url, files=files, params=params)
            response.raise_for_status()
            
            return response.json()
    
    def get_document_result(self, document_id: str) -> Dict[str, Any]:
        """
        Retrieve the processing result for a previously submitted document.
        
        Args:
            document_id (str): The unique document ID returned from process_document
        
        Returns:
            dict: The processing result containing status, data, fhir, and output
            
        Raises:
            requests.exceptions.RequestException: If the API request fails
            
        Example:
            >>> result = sdk.get_document_result("doc_12345")
            >>> if result["status"] == "completed":
            >>>     print(result["data"]["fhir"])
        """
        url = self.base_url + self.RESULT_ENDPOINT.format(document_id=document_id)
        
        response = self._session.get(url)
        response.raise_for_status()
        
        return response.json()
    
    def process_and_wait(
        self,
        file_path: str,
        task: Literal["smart", "pii", "both"] = "smart",
        poll_interval: int = 10,
        timeout: int = 300
    ) -> Dict[str, Any]:
        """
        Process a document and wait for completion.
        
        This is a convenience method that combines process_document and polling
        for results in a single call.
        
        Args:
            file_path (str): Path to the file to upload
            task (str): Processing task - one of "smart", "pii", or "both"
            poll_interval (int): Seconds to wait between polling attempts (default: 10)
            timeout (int): Maximum seconds to wait for completion (default: 300)
        
        Returns:
            dict: The completed processing result
            
        Raises:
            TimeoutError: If processing doesn't complete within timeout
            FileNotFoundError: If the file doesn't exist
            requests.exceptions.RequestException: If any API request fails
            
        Example:
            >>> result = sdk.process_and_wait("/path/to/file.jpg", poll_interval=5)
            >>> print(result["data"]["output"])
        """
        # Submit document
        submit_result = self.process_document(file_path, task=task)
        document_id = submit_result["document_id"]
        
        # Poll for results
        start_time = time.time()
        while True:
            # Check timeout
            if time.time() - start_time > timeout:
                raise TimeoutError(
                    f"Document processing timed out after {timeout} seconds"
                )
            
            result = self.get_document_result(document_id)
            data = result.get("data", {})
            
            # Check if processing is complete
            if data.get("fhir") and data.get("output"):
                return result
            
            # Check if processing failed
            if result.get("status") == "failed":
                raise Exception("Document processing failed")
            
            # Wait before next poll
            time.sleep(poll_interval)
    
    def close(self) -> None:
        """Close the underlying session."""
        self._session.close()
    
    def __enter__(self):
        """Context manager entry."""
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        """Context manager exit."""
        self.close()


__all__ = ["EkaCareSDK"]
