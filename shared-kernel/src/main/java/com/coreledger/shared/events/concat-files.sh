#!/usr/bin/env bash

# Default to current directory if no argument provided
TARGET_DIR="${1:-$(pwd)}"

# Derive folder name for output file
FOLDER_NAME=$(basename "$TARGET_DIR")
OUTPUT_FILE="${FOLDER_NAME}-combined.txt"

# Empty the output file if it exists
> "$OUTPUT_FILE"

# Iterate over files in the target directory
for file in "$TARGET_DIR"/*; do
  if [ -f "$file" ]; then
    relpath=$(realpath --relative-to="$TARGET_DIR" "$file")
    echo "=======================" >> "$OUTPUT_FILE"
    echo "$relpath" >> "$OUTPUT_FILE"
    echo "=======================" >> "$OUTPUT_FILE"
    cat "$file" >> "$OUTPUT_FILE"
    echo -e "\n" >> "$OUTPUT_FILE"
  fi
done

echo "All files from $TARGET_DIR have been combined into $OUTPUT_FILE"
