#!/usr/bin/env python3

import os
import sys
import docker
from docker.errors import ImageNotFound, ContainerError
import io
import json
import uuid
import time
import subprocess

IMAGE_NAME = "python-runner"
CONTAINER_NAME = f"python_container_{uuid.uuid4().hex[:8]}"
HOST_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "../uploads"))
CONTAINER_DIR = "/app"
TEMP_DIR = "/tmp/unzipped"
TEST_CASE_FILE = "test_cases.txt"

def build_image(client):
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

    script_base_name = os.path.splitext(zip_file_name)[0]

    test_runner_code = '''#!/usr/bin/env python3
import sys
import json
import subprocess
import time
import os

def run_tests(test_file_path, target_file):
    # Read test cases file
    with open(test_file_path, "r") as f:
        content = f.read()
        # Split on test case separator
        test_case_texts = content.split("\\n---\\n")
        test_cases = []
        for test_case_text in test_case_texts:
            test_case_text = test_case_text.strip()
            if not test_case_text:  # Skip empty test cases
                continue
            # Split on ||| to separate input from expected output
            parts = test_case_text.split("|||")
            if len(parts) == 2:
                test_input, expected_output = parts[0].strip(), parts[1].strip()
                test_cases.append({"input": test_input, "expected": expected_output})

    results = []
    for test_case in test_cases:
        try:
            test_input = test_case["input"]
            expected_output = test_case["expected"]
            
            print(f"Running test with input: {test_input!r}", file=sys.stderr)
            
            test_start = time.time() * 1000
            try:
                proc = subprocess.Popen(
                    ["python3", "-u", target_file],  # Add -u for unbuffered output
                    stdin=subprocess.PIPE,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.PIPE,
                    text=True,
                    universal_newlines=True,
                    bufsize=1  # Line buffered
                )
                
                # Start memory monitoring
                import psutil
                process = psutil.Process(proc.pid)
                max_memory = 0
                
                # Create a thread to monitor memory
                def monitor_memory():
                    nonlocal max_memory
                    while proc.poll() is None:  # While process is running
                        try:
                            current_memory = process.memory_info().rss / 1024  # Convert to KB
                            max_memory = max(max_memory, current_memory)
                        except (psutil.NoSuchProcess, psutil.AccessDenied):
                            break
                        time.sleep(0.1)  # Check every 100ms
                
                import threading
                memory_thread = threading.Thread(target=monitor_memory)
                memory_thread.daemon = True
                memory_thread.start()
                
                # Send each input line
                actual_output, errors = proc.communicate(input=test_input + "\\n", timeout=5)
                test_end = time.time() * 1000
                
                # Wait for memory monitoring to finish
                memory_thread.join(timeout=0.5)
                
                print(f"Raw output: {actual_output!r}", file=sys.stderr)
                if errors:
                    print(f"Errors: {errors!r}", file=sys.stderr)
                
                test_runtime = int(test_end - test_start)
                
                # Check if output matches expected
                actual = actual_output.strip()
                expected = expected_output.strip()
                passed = actual == expected
                
                print(f"Comparing: actual={actual!r} expected={expected!r} passed={passed}", file=sys.stderr)
                
                result = {
                    "input": test_input,
                    "expected": expected_output,
                    "actual": actual,
                    "status": "Passed" if passed else "Failed",
                    "memory": int(max_memory),  # Use the maximum memory observed
                    "runtime": test_runtime,
                    "error": errors.strip() if errors else None
                }
                
                print(f"Result: {result}", file=sys.stderr)
                results.append(result)
                
            except subprocess.TimeoutExpired:
                proc.kill()
                results.append({
                    "input": test_input,
                    "expected": expected_output,
                    "actual": "Timeout",
                    "status": "Failed",
                    "memory": 0,
                    "runtime": 5000,
                    "error": "Execution timed out after 5 seconds"
                })
            except Exception as e:
                print(f"Error during test execution: {str(e)}", file=sys.stderr)
                results.append({
                    "input": test_input,
                    "expected": expected_output,
                    "actual": "",
                    "status": "Failed",
                    "memory": 0,
                    "runtime": 0,
                    "error": str(e)
                })

        except Exception as e:
            print(f"Error processing test case: {str(e)}", file=sys.stderr)
            continue

    print(json.dumps(results))

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: test_runner.py <test_file_path> <target_file>")
        sys.exit(1)
    
    test_file_path = sys.argv[1]
    target_file = sys.argv[2]
    run_tests(test_file_path, target_file)
'''

    # Write the test runner script to a file
    test_runner_path = os.path.join(HOST_DIR, "test_runner.py")
    with open(test_runner_path, "w") as f:
        f.write(test_runner_code)

    if os.path.exists(os.path.join(HOST_DIR, TEST_CASE_FILE)):
        os.remove(os.path.join(HOST_DIR, TEST_CASE_FILE))
    if test_cases:
        test_case_path = os.path.join(HOST_DIR, TEST_CASE_FILE)
        with open(test_case_path, "w") as f:
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
                  python3 {CONTAINER_DIR}/test_runner.py {CONTAINER_DIR}/{TEST_CASE_FILE} "$target_file" > /tmp/test_results.json &&
                  cat /tmp/test_results.json
                else
                  python "$target_file" 2>&1 bo
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

        output = ""
        for log in container.logs(stream=True):
            output += log.decode('utf-8')

        result = container.wait()
        exit_code = result.get('StatusCode', 1)

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