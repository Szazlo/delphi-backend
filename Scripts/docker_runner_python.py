#!/usr/bin/env python3

import os
import sys
import docker
from docker.errors import ImageNotFound, ContainerError
import io

# Constants
IMAGE_NAME = "python-runner"
CONTAINER_NAME = "python_container"
HOST_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "../uploads"))
CONTAINER_DIR = "/app"
TEMP_DIR = "/tmp/unzipped"
PYTHON_SCRIPT = "helloworld.py"

def build_image(client):
    # Build image if not existing
    try:
        client.images.get(IMAGE_NAME)
        print(f"Docker image '{IMAGE_NAME}' already exists.")
    except ImageNotFound:
        print("Building Docker image...")
        dockerfile_content = f"""
        FROM python:3.13-slim
        RUN apt-get update && apt-get install -y unzip
        RUN pip install pylint
        WORKDIR {CONTAINER_DIR}
        CMD ["python", "{PYTHON_SCRIPT}"]
        """
        client.images.build(fileobj=io.BytesIO(dockerfile_content.encode('utf-8')), tag=IMAGE_NAME)
        print(f"Docker image '{IMAGE_NAME}' built successfully.")

def run_container(client, zip_file_name):
    if not zip_file_name:
        print("Error: No zip file specified.")
        sys.exit(1)

    # Unzip archive and run Python script and pylint
    container_volumes = {HOST_DIR: {"bind": CONTAINER_DIR, "mode": "rw"}}
    command = f"""
    sh -c 'mkdir -p {TEMP_DIR} && \
           unzip -q {CONTAINER_DIR}/{zip_file_name} -d {TEMP_DIR} && \
           echo "LINTING" && \
           pylint {TEMP_DIR}/*.py || true && \
           echo "EXECUTION" && \
           python {TEMP_DIR}/{PYTHON_SCRIPT} 2>&1; exit $?'
    """  # Redirect both stdout and stderr

    try:
        print("Starting Docker container...")
        container = client.containers.run(
            IMAGE_NAME,
            name=CONTAINER_NAME,
            command=command,
            volumes=container_volumes,
            detach=True,
            remove=True
        )

        logs = container.logs(stream=True)
        output = ""
        for log in logs:
            output += log.decode('utf-8')

        exit_code = container.wait()['StatusCode']
        if exit_code != 0:
            print(f"Container executed with errors (exit code {exit_code}).")
            return output.strip(), exit_code

        print("Container executed successfully.")
        return output.strip(), 0

    except ContainerError as e:
        error_message = f"Error: Docker container execution failed.\n{e}"
        print(error_message)
        return error_message, 1

def main():
    if len(sys.argv) < 2:
        print("Error: No zip file specified.")
        sys.exit(1)

    zip_file_name = sys.argv[1]

    client = docker.from_env()
    build_image(client)
    output, exit_code = run_container(client, zip_file_name)

    print("Execution output:")
    print(output)
    sys.exit(exit_code)

if __name__ == "__main__":
    main()