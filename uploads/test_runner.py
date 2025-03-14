#!/usr/bin/env python3
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
        test_case_texts = content.split("\n---\n")
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
            
            # Execute the test
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
                
                # Send each input line
                actual_output, errors = proc.communicate(input=test_input + "\n", timeout=5)
                test_end = time.time() * 1000
                
                print(f"Raw output: {actual_output!r}", file=sys.stderr)
                if errors:
                    print(f"Errors: {errors!r}", file=sys.stderr)
                
                # Calculate metrics
                test_runtime = int(test_end - test_start)
                
                # Get memory usage
                memory_usage = 0
                try:
                    pid_str = str(proc.pid)
                    memory_usage = int(os.popen("ps -o rss= -p " + pid_str).read().strip())
                except Exception as e:
                    print(f"Error getting memory usage: {str(e)}", file=sys.stderr)
                
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
                    "memory": memory_usage,
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
