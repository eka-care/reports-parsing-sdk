"""
Setup script for Eka Care SDK.
"""

from setuptools import setup, find_packages
from pathlib import Path

# Read README
this_directory = Path(__file__).parent
long_description = (this_directory / "README.md").read_text(encoding="utf-8")

setup(
    name="ekacare-sdk",
    version="1.0.0",
    author="Eka Care",
    author_email="support@eka.care",
    description="Python SDK for Eka Care Medical Records API",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/eka-care/ekacare-python-sdk",
    packages=find_packages(exclude=["tests", "examples"]),
    classifiers=[
        "Development Status :: 4 - Beta",
        "Intended Audience :: Developers",
        "Intended Audience :: Healthcare Industry",
        "Topic :: Software Development :: Libraries :: Python Modules",
        "Topic :: Scientific/Engineering :: Medical Science Apps.",
        "License :: OSI Approved :: MIT License",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.8",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: 3.10",
        "Programming Language :: Python :: 3.11",
        "Programming Language :: Python :: 3.12",
    ],
    python_requires=">=3.8",
    install_requires=[
        "requests>=2.28.0",
    ],
    extras_require={
        "dev": [
            "pytest>=7.0.0",
            "pytest-cov>=4.0.0",
            "black>=22.0.0",
            "flake8>=5.0.0",
            "mypy>=0.990",
            "isort>=5.10.0",
        ],
        "cli": [
            "rich>=13.0.0",  # For better CLI output
        ]
    },
    entry_points={
        "console_scripts": [
            "ekacare-cli=ekacare_sdk.cli:main",
        ],
    },
    keywords="eka care medical records healthcare api sdk fhir",
    project_urls={
        "Documentation": "https://docs.eka.care",
        "Source": "https://github.com/eka-care/ekacare-python-sdk",
        "Bug Reports": "https://github.com/eka-care/ekacare-python-sdk/issues",
    },
)
