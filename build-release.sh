#!/bin/bash

# Define variables
ZIP_FILE="./build/distributions/hag-api-testing-katalog-shadow-0.0.1.zip"
EXTRACT_DIR="./build/distributions/hag-api-testing-katalog-shadow-0.0.1"
SOURCE_JAR="$EXTRACT_DIR/lib/hag-api-testing-katalog-all.jar"
DEST_DIR="./release/lib/"

# Unzip the file
unzip "$ZIP_FILE" -d "./build/distributions/"

# Create destination directory if it doesn't exist
mkdir -p "$DEST_DIR"

# Move the JAR file
mv "$SOURCE_JAR" "$DEST_DIR"

# Delete the extracted folder
rm -rf "$EXTRACT_DIR"

echo "Unzipped, JAR moved to $DEST_DIR, and source directory deleted."