package com.davidwilson.delphi.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.davidwilson.delphi.entities.Submissions;
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
    public void runScriptAsync(String zipFileName, Submissions submission, SubmissionRepository submissionRepository) {
        try {
            submission.setStatus("Running");
            submissionRepository.save(submission);

            // Run script
            String scriptOutput = runScript(zipFileName);
            // Extract lint and execution output
            int lintingIndex = scriptOutput.indexOf("LINTING");
            int executionIndex = scriptOutput.indexOf("EXECUTION");
            String lintingOutput = scriptOutput.substring(lintingIndex + 7, executionIndex);
            String executionOutput = scriptOutput.substring(executionIndex + 10);

            submission.setOutput(executionOutput);
            submission.setLintOutput(lintingOutput);

            submission.setStatus("Completed");
            submissionRepository.save(submission);
        } catch (IOException | InterruptedException e) {
            submission.setOutput(e.getMessage());
            submission.setStatus("Failed");
            submissionRepository.save(submission);
        }
    }

    private String runScript(String zipFileName) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("python3", SCRIPT_PATH, zipFileName);
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