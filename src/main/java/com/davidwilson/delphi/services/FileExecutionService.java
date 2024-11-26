package com.davidwilson.delphi.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.davidwilson.delphi.entities.Submissions;
import com.davidwilson.delphi.repositories.SubmissionRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class FileExecutionService {

    private static final String SCRIPT_PATH = "Scripts/docker_runner_python.py";

    @Async("submissionExecutor")
    public void runScriptAsync(String zipFileName, Submissions submission, SubmissionRepository submissionRepository) {
        try {
            submission.setStatus("Running");
            submissionRepository.save(submission);
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
            String status = exitCode == 0 ? "Completed" : "Failed";
            submission.setOutput(output.toString());
            submission.setStatus(status);
            submissionRepository.save(submission);
        } catch (IOException | InterruptedException e) {
            submission.setOutput(e.getMessage());
            submission.setStatus("Failed");
            submissionRepository.save(submission);
        }
    }
}

