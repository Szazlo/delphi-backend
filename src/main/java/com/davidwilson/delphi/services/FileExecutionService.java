package com.davidwilson.delphi.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileExecutionService {

    private static final String SCRIPT_PATH = "Scripts/docker_runner_python.py";

    public String runScript(String zipFileName) {
        try {
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
            if (exitCode == 0) {
                return output.toString();
            } else {
                return "Execution failed with exit code: " + exitCode + "\nOutput:\n" + output.toString();
            }
        } catch (IOException | InterruptedException e) {
            return e.getMessage();
        }
    }

}

