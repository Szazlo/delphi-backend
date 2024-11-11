#!/bin/bash

# Set default values for variables
IMAGE_NAME="python-runner"
CONTAINER_NAME="python_container"
HOST_DIR="$(pwd)/uploads"  # Absolute path to uploads
CONTAINER_DIR="/app"              # Directory inside the container where files will be mounted
TEMP_DIR="/tmp/unzipped"          # Temporary directory inside the container for unzipping
ZIP_FILE_NAME="$1"                # Zip file name passed as an argument
PYTHON_SCRIPT="helloworld.py"

# Ensure the zip file is provided
if [[ -z "$ZIP_FILE_NAME" ]]; then
    echo "Error: No zip file specified."
    exit 1
fi

# Step 1: Build the Docker image if it doesn't exist
if ! docker image inspect "$IMAGE_NAME" > /dev/null 2>&1; then
    echo "Building Docker image..."
    docker build -t "$IMAGE_NAME" - <<EOF
FROM python:3.8-slim
RUN apt-get update && apt-get install -y unzip
WORKDIR $CONTAINER_DIR
CMD ["python", "$PYTHON_SCRIPT"]
EOF
    echo "Docker image '$IMAGE_NAME' built successfully."
else
    echo "Docker image '$IMAGE_NAME' already exists."
fi

# Step 2: Log the host directory and the zip file name
echo "Host directory: $HOST_DIR"
echo "Looking for zip file: $ZIP_FILE_NAME"

# Step 3: Start a Docker container with the specified mounted directory
echo "Starting Docker container '$CONTAINER_NAME'..."
EXECUTION_OUTPUT=$(docker run --rm --name "$CONTAINER_NAME" -v "$HOST_DIR:$CONTAINER_DIR" "$IMAGE_NAME" sh -c "
    # Create temporary directory for unzipping
    mkdir -p $TEMP_DIR && \
    # List files in /app to debug
    echo 'Files in container directory:' && ls $CONTAINER_DIR && \
    # Unzip the file into the temporary directory
    unzip -o \"$CONTAINER_DIR/$ZIP_FILE_NAME\" -d \"$TEMP_DIR\" && \
    # Run the Python script from the temporary directory and capture its output
    python \"$TEMP_DIR/$PYTHON_SCRIPT\"
")

# Capture the exit status of the Docker command
EXIT_STATUS=$?

# Check if the container ran successfully
if [[ $EXIT_STATUS -ne 0 ]]; then
    echo "Error: Docker container execution failed."
    echo "Exit status: $EXIT_STATUS"
    echo "Check output.log for details."
    echo "$EXECUTION_OUTPUT" >> "$HOST_DIR/output.log"  # Save output to log file
    exit 1
fi

# Output the execution result
echo "$EXECUTION_OUTPUT"  # This will only print the relevant output from the Python script
exit 0
