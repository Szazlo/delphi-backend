#!/bin/bash
for zipfile in *.zip; do
    if [ -f "$zipfile" ]; then
        echo "Unzipping $zipfile..."
        unzip -o "$zipfile" -d "${zipfile%.*}"  # Unzip to a directory named after the zip file
    fi
done

# Find and execute main.py
if [ -f "*/main.py" ]; then
    echo "Running main.py..."
    python */main.py
else
    echo "main.py not found!"
    exit 1
fi
