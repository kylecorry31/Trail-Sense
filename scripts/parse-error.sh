#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Use provided mapping file path or default to playStore
if [ -n "$1" ]; then
    MAPPING_FILE="$1"
else
    MAPPING_FILE="$SCRIPT_DIR/../app/build/outputs/mapping/playStore/mapping.txt"
fi

echo "$SCRIPT_DIR/error.txt"

$ANDROID_HOME/cmdline-tools/latest/bin/retrace "$MAPPING_FILE" "$SCRIPT_DIR/error.txt"