package com.davidwilson.delphi.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import com.davidwilson.delphi.entities.Submissions;
import com.davidwilson.delphi.entities.TestCase;
import com.davidwilson.delphi.repositories.SubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class FileExecutionService {

    Logger logger = LoggerFactory.getLogger(FileExecutionService.class);
    private static final String SCRIPT_PATH = "Scripts/docker_runner_python.py";

    @Async("submissionExecutor")
    public void runScriptAsync(String zipFileName, Submissions submission, SubmissionRepository submissionRepository, List<TestCase> testCases) {
        try {
            submission.setStatus("Running");
            submissionRepository.save(submission);

            String testCasesString = "";
            if (!testCases.isEmpty()) {
                //prepare test cases to pass onto the script
                StringBuilder sb = new StringBuilder();
                for (TestCase testCase : testCases) {
                    // Format: input<space>expectedOutput
                    sb.append(testCase.getInput()).append(" ").append(testCase.getExpectedOutput()).append("\n");
                }
                testCasesString = sb.toString();
            }

            // Run script
            String scriptOutput = runScript(zipFileName, testCasesString);

            // Extract lint and execution output
            int lintingIndex = scriptOutput.indexOf("LINTING");
            int executionIndex = scriptOutput.indexOf("EXECUTION");
            int metricsIndex = scriptOutput.indexOf("METRICS");

            if (lintingIndex == -1 || executionIndex == -1 || metricsIndex == -1) {
                logger.error("Missing required output markers in script output");
                submission.setStatus("Failed");
                submission.setOutput("Script output format error");
                submissionRepository.save(submission);
                return;
            }

            String lintingOutput = scriptOutput.substring(lintingIndex + 7, executionIndex);

            boolean hasTestCases = scriptOutput.contains("TEST_CASES");

            if (hasTestCases) {
                int testCasesIndex = scriptOutput.indexOf("TEST_CASES");
                String testCaseText = scriptOutput.substring(testCasesIndex + 10, metricsIndex).trim();

                // Find JSON array starting with '[' and ending with ']'
                int jsonArrayStart = testCaseText.indexOf('[');
                int jsonArrayEnd = testCaseText.lastIndexOf(']') + 1;

                if (jsonArrayStart >= 0 && jsonArrayEnd > jsonArrayStart) {
                    String jsonResults = testCaseText.substring(jsonArrayStart, jsonArrayEnd);
                    // Save only the JSON part to test results
                    submission.setTestResults(jsonResults);

                    // For the execution output, include only program output before the JSON
                    if (jsonArrayStart > 0) {
                        submission.setOutput(testCaseText.substring(0, jsonArrayStart).trim());
                    } else {
                        submission.setOutput("Test case execution completed.");
                    }
                } else {
                    // No valid JSON found
                    submission.setTestResults("");
                    submission.setOutput(testCaseText);
                }
            } else {
                // No test cases, just regular execution output
                submission.setOutput(scriptOutput.substring(executionIndex + 10, metricsIndex).trim());
            }

            // Extract metrics values
            String metricsOutput = scriptOutput.substring(metricsIndex + 8).trim();
            try {
                // First try direct parsing of the specific format
                String[] metrics = metricsOutput.split(",");
                if (metrics.length >= 2) {
                    // Clean up and extract runtime value
                    String runtimePart = metrics[0];
                    int runtimeColonIndex = runtimePart.indexOf(":");
                    if (runtimeColonIndex >= 0) {
                        String runtimeStr = runtimePart.substring(runtimeColonIndex + 1).trim();
                        // Extract only the numeric part
                        runtimeStr = runtimeStr.replaceAll("[^0-9]", "");
                        if (!runtimeStr.isEmpty()) {
                            submission.setRuntime(Integer.parseInt(runtimeStr));
                        } else {
                            submission.setRuntime(0);
                        }
                    } else {
                        submission.setRuntime(0);
                    }

                    // Clean up and extract memory value
                    String memoryPart = metrics[1];
                    int memoryColonIndex = memoryPart.indexOf(":");
                    if (memoryColonIndex >= 0) {
                        String memoryStr = memoryPart.substring(memoryColonIndex + 1).trim();
                        // Extract only the numeric part
                        memoryStr = memoryStr.replaceAll("[^0-9]", "");
                        if (!memoryStr.isEmpty()) {
                            submission.setMemoryUsage(Integer.parseInt(memoryStr));
                        } else {
                            submission.setMemoryUsage(0);
                        }
                    } else {
                        submission.setMemoryUsage(0);
                    }
                } else {
                    logger.warn("Invalid metrics format (insufficient parts): {}", metricsOutput);
                    submission.setRuntime(0);
                    submission.setMemoryUsage(0);
                }
            } catch (Exception e) {
                logger.error("Error parsing metrics: {} - Data: {}", e.getMessage(), metricsOutput);
                submission.setRuntime(0);
                submission.setMemoryUsage(0);
            }

            submission.setLintOutput(lintingOutput);
            submission.setStatus("Completed");
            submissionRepository.save(submission);
        } catch (IOException | InterruptedException e) {
            logger.error("Error executing script: ", e);
            submission.setOutput(e.getMessage());
            submission.setStatus("Failed");
            submissionRepository.save(submission);
        }
    }

    private String runScript(String zipFileName, String testCases) throws IOException, InterruptedException {
        ProcessBuilder processBuilder;

        if (testCases != null && !testCases.isEmpty()) {
            processBuilder = new ProcessBuilder("python3", SCRIPT_PATH, zipFileName, testCases);
        } else {
            processBuilder = new ProcessBuilder("python3", SCRIPT_PATH, zipFileName);
        }

        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            logger.error("Console: {}", output);
            throw new IOException("Script execution failed with exit code " + exitCode);
        }
        return output.toString();
    }
}