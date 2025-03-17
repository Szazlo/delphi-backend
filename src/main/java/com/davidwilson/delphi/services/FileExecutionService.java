package com.davidwilson.delphi.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import com.davidwilson.delphi.entities.Submissions;
import com.davidwilson.delphi.entities.TestCase;
import com.davidwilson.delphi.repositories.SubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class FileExecutionService {

    private final Logger logger = LoggerFactory.getLogger(FileExecutionService.class);
    private static final String SCRIPT_PATH = "Scripts/docker_runner_python.py";
    private static final String HOST_DIR = "uploads";  // Directory for uploaded files
    private static final String TEMP_DIR = "tmp/unzipped";  // Directory for unzipped files
    private final AIAnalysisService aiAnalysisService;

    @Autowired
    public FileExecutionService(AIAnalysisService aiAnalysisService) {
        this.aiAnalysisService = aiAnalysisService;
    }

    @Async("submissionExecutor")
    public void runScriptAsync(String zipFileName, Submissions submission, SubmissionRepository submissionRepository, List<TestCase> testCases) {
        Path tempPath = null;
        try {
            submission.setStatus("Running");
            submissionRepository.save(submission);

            // Get absolute paths
            Path currentPath = Paths.get("").toAbsolutePath();
            Path uploadPath = currentPath.resolve(HOST_DIR);
            tempPath = uploadPath.resolve(TEMP_DIR);
            
            logger.info("Upload path: {}", uploadPath);
            logger.info("Temp path: {}", tempPath);

            // Ensure directories exist
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            if (!Files.exists(tempPath)) {
                Files.createDirectories(tempPath);
            }

            // Run test execution in Docker
            String testCasesString = prepareTestCases(testCases);
            String scriptOutput = runScript(zipFileName, testCasesString);
            processScriptOutput(scriptOutput, submission);

            // Unzip the file for AI analysis
            Path zipFilePath = uploadPath.resolve(zipFileName);
            Path projectDir = tempPath.resolve(zipFileName.replace(".zip", ""));
            
            // Create a clean directory for the unzipped contents
            if (Files.exists(projectDir)) {
                deleteDirectory(projectDir);
            }
            Files.createDirectories(projectDir);

            // Unzip the file
            unzipFile(zipFilePath, projectDir);
            logger.info("Unzipped files to: {}", projectDir);

            // Perform AI analysis
            if (Files.exists(projectDir)) {
                String aiAnalysis = aiAnalysisService.analyzeCode(projectDir.toString());
                if (aiAnalysis != null) {
                    submission.setAIOutput(aiAnalysis);
                } else {
                    logger.info("AI analysis is disabled - skipping analysis phase");
                    submission.setAIOutput("AI analysis is disabled");
                }
            } else {
                logger.error("Project directory not found after unzipping: {}", projectDir);
                submission.setAIOutput("Error: Failed to prepare files for analysis.");
            }

            submission.setStatus("Completed");
            submissionRepository.save(submission);

        } catch (Exception e) {
            logger.error("Error executing script: ", e);
            submission.setOutput(e.getMessage());
            submission.setStatus("Failed");
            submissionRepository.save(submission);
        } finally {
            // Cleanup: Delete the temporary directory
            if (tempPath != null) {
                try {
                    deleteDirectory(tempPath);
                    logger.info("Cleaned up temporary directory: {}", tempPath);
                } catch (IOException e) {
                    logger.error("Error cleaning up temporary directory: {}", e.getMessage());
                }
            }
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        logger.error("Error deleting path: " + p, e);
                    }
                });
        }
    }

    private void unzipFile(Path zipFile, Path targetDir) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("unzip", "-q", zipFile.toString(), "-d", targetDir.toString());
        Process process = processBuilder.start();
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Failed to unzip file, exit code: " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Unzip process interrupted", e);
        }
    }

    private String prepareTestCases(List<TestCase> testCases) {
        if (testCases.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (TestCase testCase : testCases) {
            String input = testCase.getInput().replace("\\n", "\n");
            String expected = testCase.getExpectedOutput().replace("\\n", "\n");
            sb.append(input).append("|||").append(expected).append("\n---\n");
        }
        return sb.toString();
    }

    private void processScriptOutput(String scriptOutput, Submissions submission) {
        try {
            // Extract lint and execution output
            int lintingIndex = scriptOutput.indexOf("LINTING");
            int executionIndex = scriptOutput.indexOf("EXECUTION");
            int metricsIndex = scriptOutput.indexOf("METRICS");

            if (lintingIndex == -1 || executionIndex == -1 || metricsIndex == -1) {
                logger.error("Missing required output markers in script output");
                submission.setStatus("Failed");
                submission.setOutput("Script output format error");
                return;
            }

            String lintingOutput = scriptOutput.substring(lintingIndex + 7, executionIndex);
            submission.setLintOutput(lintingOutput);

            boolean hasTestCases = scriptOutput.contains("TEST_CASES");
            if (hasTestCases) {
                processTestCaseOutput(scriptOutput.substring(scriptOutput.indexOf("TEST_CASES") + 10, metricsIndex).trim(), submission);
            } else {
                submission.setOutput(scriptOutput.substring(executionIndex + 10, metricsIndex).trim());
            }

            processMetrics(scriptOutput.substring(metricsIndex + 8).trim(), submission);
        } catch (Exception e) {
            logger.error("Error processing script output: ", e);
            submission.setOutput("Error processing script output: " + e.getMessage());
        }
    }

    private void processTestCaseOutput(String testCaseText, Submissions submission) {
        int jsonArrayStart = testCaseText.indexOf('[');
        int jsonArrayEnd = testCaseText.lastIndexOf(']') + 1;

        if (jsonArrayStart >= 0 && jsonArrayEnd > jsonArrayStart) {
            String jsonResults = testCaseText.substring(jsonArrayStart, jsonArrayEnd);
            submission.setTestResults(jsonResults);

            if (jsonArrayStart > 0) {
                submission.setOutput(testCaseText.substring(0, jsonArrayStart).trim());
            } else {
                submission.setOutput("Test case execution completed.");
            }
        } else {
            submission.setTestResults("");
            submission.setOutput(testCaseText);
        }
    }

    private void processMetrics(String metricsOutput, Submissions submission) {
        try {
            String[] metrics = metricsOutput.split(",");
            if (metrics.length >= 2) {
                processRuntimeMetric(metrics[0], submission);
                processMemoryMetric(metrics[1], submission);
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
    }

    private void processRuntimeMetric(String runtimePart, Submissions submission) {
        int runtimeColonIndex = runtimePart.indexOf(":");
        if (runtimeColonIndex >= 0) {
            String runtimeStr = runtimePart.substring(runtimeColonIndex + 1).trim().replaceAll("[^0-9]", "");
            submission.setRuntime(runtimeStr.isEmpty() ? 0 : Integer.parseInt(runtimeStr));
        } else {
            submission.setRuntime(0);
        }
    }

    private void processMemoryMetric(String memoryPart, Submissions submission) {
        int memoryColonIndex = memoryPart.indexOf(":");
        if (memoryColonIndex >= 0) {
            String memoryStr = memoryPart.substring(memoryColonIndex + 1).trim().replaceAll("[^0-9]", "");
            submission.setMemoryUsage(memoryStr.isEmpty() ? 0 : Integer.parseInt(memoryStr));
        } else {
            submission.setMemoryUsage(0);
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