#!/usr/bin/env python3

import os
import sys
import docker
from docker.errors import ImageNotFound, ContainerError
import io
import json
import uuid

# Constants
IMAGE_NAME = "python-runner"
CONTAINER_NAME = f"python_container_{uuid.uuid4().hex[:8]}"
HOST_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "../uploads"))
CONTAINER_DIR = "/app"
TEMP_DIR = "/tmp/unzipped"
TEST_CASE_FILE = "test_cases.txt"

def build_image(client):
    # Build image if not existing
    try:
        client.images.get(IMAGE_NAME)
        print(f"Docker image '{IMAGE_NAME}' already exists.")
    except ImageNotFound:
        print("Building Docker image...")
        dockerfile_content = """
        FROM python:3.13-slim
        RUN apt-get update && apt-get install -y unzip procps time
        RUN pip install pylint psutil memory_profiler
        WORKDIR /app
        CMD ["python"]
        """
        client.images.build(fileobj=io.BytesIO(dockerfile_content.encode('utf-8')), tag=IMAGE_NAME)
        print(f"Docker image '{IMAGE_NAME}' built successfully.")

def run_container(client, zip_file_name, test_cases=None):
    if not zip_file_name:
        print("Error: No zip file specified.")
        sys.exit(1)

    # Extract the base name without .zip extension to use as the script name
    script_base_name = os.path.splitext(zip_file_name)[0]

    if os.path.exists(os.path.join(HOST_DIR, TEST_CASE_FILE)):
        os.remove(os.path.join(HOST_DIR, TEST_CASE_FILE))
    if test_cases:
        test_case_path = os.path.join(HOST_DIR, TEST_CASE_FILE)
        with open(test_case_path, 'w') as f:
            f.write(test_cases)

    container_volumes = {HOST_DIR: {"bind": CONTAINER_DIR, "mode": "rw"}}

    command = f"""
                mkdir -p {TEMP_DIR} &&
                unzip -q {CONTAINER_DIR}/{zip_file_name} -d {TEMP_DIR} &&
                echo 'LINTING' &&
                pylint {TEMP_DIR}/*.py || true &&
                echo 'EXECUTION' &&
                start_time=$(date +%s%3N) &&
                if [ -f {TEMP_DIR}/{script_base_name}.py ]; then
                  target_file="{TEMP_DIR}/{script_base_name}.py"
                elif [ -f {TEMP_DIR}/main.py ]; then
                  target_file="{TEMP_DIR}/main.py"
                else
                  echo 'Error: Neither {script_base_name}.py nor main.py found in the zip file.'
                  exit 1
                fi &&
                if [ -f {CONTAINER_DIR}/{TEST_CASE_FILE} ]; then
                  echo 'TEST_CASES' &&
                  rm -f /tmp/test_results.json &&
                  python3 -c '
import sys
import json
import subprocess
import time
import os

# Read test cases file
with open("{CONTAINER_DIR}/{TEST_CASE_FILE}", "r") as f:
    test_content = f.read().strip()

# Parse test cases - format is:
# first line = number of lines in test case
# remaining lines = input and expected output
# (where the last line has the expected output)
lines = test_content.split("\\n")
results = []
i = 0

while i < len(lines):
    if not lines[i].strip():
        i += 1
        continue

    try:
        # First line indicates number of lines in this test case
        num_lines = int(lines[i].strip())

        if i + num_lines >= len(lines):
            break

        # Extract all lines for this test case
        test_input = "\\n".join(lines[i:i+num_lines])
        expected_output = lines[i+num_lines-1].split(" ", 1)[1] if " " in lines[i+num_lines-1] else ""

        # Prepare the input file
        with open("/tmp/current_input.txt", "w") as f:
            f.write(test_input)

        # Execute the test
        test_start = time.time() * 1000
        proc = subprocess.Popen(
            ["python", "{TEMP_DIR}/{script_base_name}.py"],
            stdin=open("/tmp/current_input.txt", "r"),
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        actual_output, errors = proc.communicate()
        test_end = time.time() * 1000

        # Calculate metrics
        test_runtime = int(test_end - test_start)

        # Get memory usage
        memory_usage = 0
        try:
            pid_str = str(proc.pid)
            memory_usage = int(os.popen("ps -o rss= -p " + pid_str).read().strip())
        except:
            pass

        # Check if output matches expected
        actual = actual_output.strip()
        passed = actual == expected_output.strip()

        results.append({{
            "input": test_input,
            "expected": expected_output,
            "actual": actual,
            "passed": passed,
            "runtime": test_runtime,
            "memory": memory_usage
        }})

        # Move to next test case
        i += num_lines

    except (ValueError, IndexError):
        # Skip invalid lines
        i += 1

print(json.dumps(results))
' > /tmp/test_results.json &&
                  cat /tmp/test_results.json
                else
                  python "$target_file" 2>&1
                fi &&
                end_time=$(date +%s%3N) &&
                runtime=$((end_time - start_time)) &&
                memory=$(ps -o rss= -p $$ | tr -d ' ') &&
                echo 'METRICS' &&
                echo "runtime:$runtime,memory:$memory"
            """

    try:
        print("Starting Docker container...")
        container = client.containers.run(
            IMAGE_NAME,
            name=CONTAINER_NAME,
            command=["sh", "-c", command],
            volumes=container_volumes,
            detach=True
        )

        # Capture the output
        output = ""
        for log in container.logs(stream=True):
            output += log.decode('utf-8')

        # Get exit code
        result = container.wait()
        exit_code = result.get('StatusCode', 1)

        # Clean up
        try:
            container.remove()
        except docker.errors.APIError:
            pass

        if exit_code != 0:
            print(f"Container executed with errors (exit code {exit_code}).")
        else:
            print("Container executed successfully.")

        return output.strip(), exit_code

    except (ContainerError, docker.errors.APIError) as e:
        error_message = f"Error: Docker container execution failed.\n{e}"
        print(error_message)
        return error_message, 1

def main():
    if len(sys.argv) < 2:
        print("Error: No zip file specified.")
        sys.exit(1)

    zip_file_name = sys.argv[1]
    test_cases = None

    # Check if test cases are provided as second argument
    if len(sys.argv) > 2:
        test_cases = sys.argv[2]

    client = docker.from_env()
    build_image(client)
    output, exit_code = run_container(client, zip_file_name, test_cases)

    print("Execution output")
    print(output)
    sys.exit(exit_code)

if __name__ == "__main__":
    main()