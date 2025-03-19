package com.davidwilson.delphi.services;

import com.davidwilson.delphi.config.AIConfig;
import com.davidwilson.delphi.config.AIConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AIAnalysisService {
    private final Logger logger = LoggerFactory.getLogger(AIAnalysisService.class);
    private final AIConfig aiConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AIConfigurationService configService;

    public AIAnalysisService(AIConfig aiConfig, AIConfigurationService configService) {
        this.aiConfig = aiConfig;
        this.configService = configService;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public String analyzeCode(String projectPath) {
        // First check if AI analysis is enabled
        Optional<AIConfiguration> activeConfig = configService.getActiveConfiguration();
        if (activeConfig.isEmpty() || !activeConfig.get().isActive()) {
            logger.info("AI analysis is disabled - no active configuration found or configuration is inactive");
            return null;
        }

        try {
            // Convert to absolute path if not already
            Path path = Paths.get(projectPath).toAbsolutePath();
            logger.info("Analyzing code in directory: {}", path);

            if (!Files.exists(path)) {
                logger.error("Project path does not exist: {}", path);
                return "Error: Project directory not found.";
            }

            if (!Files.isDirectory(path)) {
                logger.error("Path is not a directory: {}", path);
                return "Error: Path is not a directory.";
            }

            // List directory contents for debugging
            logger.info("Directory contents:");
            try (var paths = Files.walk(path)) {
                paths.forEach(p -> logger.info("- {}", p));
            }

            // Read all code files
            List<FileContent> codeFiles = readProjectFiles(path.toString());
            if (codeFiles.isEmpty()) {
                logger.warn("No code files found in: {}", path);
                return "No code files found to analyze.";
            }

            logger.info("Found {} files to analyze:", codeFiles.size());
            codeFiles.forEach(file -> logger.info("- {}", file.path));
            
            // Prepare the prompt
            String prompt = buildAnalysisPrompt(codeFiles);

            // Call LLM API
            return callLLMAPI(prompt);
        } catch (Exception e) {
            logger.error("Error during code analysis for path: " + projectPath, e);
            return "Error analyzing code: " + e.getMessage();
        }
    }

    public String analyzeSubmissionText(String prompt) {
        // First check if AI analysis is enabled
        Optional<AIConfiguration> activeConfig = configService.getActiveConfiguration();
        if (activeConfig.isEmpty() || !activeConfig.get().isActive()) {
            logger.info("AI analysis is disabled - no active configuration found or configuration is inactive");
            return null;
        }

        try {
            // Call LLM API directly with the prompt
            return callLLMAPI(prompt);
        } catch (Exception e) {
            logger.error("Error analyzing submission text", e);
            return "Error analyzing submissions: " + e.getMessage();
        }
    }

    private List<FileContent> readProjectFiles(String projectPath) throws IOException {
        List<FileContent> files = new ArrayList<>();
        Path basePath = Paths.get(projectPath).toAbsolutePath();
        
        if (!Files.exists(basePath)) {
            throw new IOException("Project directory does not exist: " + basePath);
        }

        if (!Files.isDirectory(basePath)) {
            throw new IOException("Path is not a directory: " + basePath);
        }

        logger.debug("Reading files from: {}", basePath);
        
        try (var paths = Files.walk(basePath)) {
            paths.filter(Files::isRegularFile)
                .filter(path -> isCodeFile(path.toString()))
                .forEach(path -> {
                    try {
                        String content = Files.readString(path);
                        String relativePath = basePath.relativize(path).toString();
                        files.add(new FileContent(relativePath, content));
                        logger.debug("Added file for analysis: {}", relativePath);
                    } catch (IOException e) {
                        logger.error("Error reading file: " + path, e);
                    }
                });
        }
        
        return files;
    }

    private boolean isCodeFile(String path) {
        String lowercasePath = path.toLowerCase();
        return lowercasePath.endsWith(".java") || 
               lowercasePath.endsWith(".py") || 
               lowercasePath.endsWith(".js") || 
               lowercasePath.endsWith(".jsx") || 
               lowercasePath.endsWith(".ts") || 
               lowercasePath.endsWith(".tsx") ||
               lowercasePath.endsWith(".html") ||
               lowercasePath.endsWith(".css");
    }

    private String buildAnalysisPrompt(List<FileContent> files) {
        AIConfiguration config = configService.getActiveConfiguration()
            .orElseThrow(() -> new RuntimeException("No active AI configuration found"));

        StringBuilder prompt = new StringBuilder();
        prompt.append(config.getAnalysisPrompt());
        prompt.append("\n\n");

        for (FileContent file : files) {
            prompt.append("File: ").append(file.path).append("\n");
            prompt.append("```\n").append(file.content).append("\n```\n\n");
        }

        return prompt.toString();
    }

    private String callLLMAPI(String prompt) throws Exception {
        AIConfiguration config = configService.getActiveConfiguration()
            .orElseThrow(() -> new RuntimeException("No active AI configuration found"));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getModel());
        requestBody.put("messages", Arrays.asList(
            Map.of("role", "system", "content", config.getSystemPrompt()),
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", config.getTemperature());
        requestBody.put("max_tokens", config.getMaxTokens());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiConfig.getApiKey());

        HttpEntity<String> requestEntity = new HttpEntity<>(
            objectMapper.writeValueAsString(requestBody),
            headers
        );

        ResponseEntity<Map> response = restTemplate.exchange(
            aiConfig.getApiUrl(),
            HttpMethod.POST,
            requestEntity,
            Map.class
        );

        if (response.getBody() != null && response.getBody().containsKey("choices")) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            if (!choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
        }

        throw new Exception("Invalid API response");
    }

    private static class FileContent {
        String path;
        String content;

        FileContent(String path, String content) {
            this.path = path;
            this.content = content;
        }
    }
} 