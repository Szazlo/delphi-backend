package com.davidwilson.delphi.services;

import com.davidwilson.delphi.config.AIConfiguration;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AIConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(AIConfigurationService.class);
    private static final String CONFIG_FILE = "ai-configurations.json";
    private static final String DEFAULT_CONFIG_FILE = "default-ai-config.json";
    private final ObjectMapper objectMapper;
    private final Path configPath;

    public AIConfigurationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.configPath = Paths.get(System.getProperty("user.home"), ".delphi", CONFIG_FILE);
        initializeConfigurations();
    }

    private void initializeConfigurations() {
        try {
            // Create directory if it doesn't exist
            Files.createDirectories(configPath.getParent());

            // If config file doesn't exist, create it with default configuration
            if (!Files.exists(configPath)) {
                // Read default configuration from resources
                ClassPathResource defaultConfig = new ClassPathResource(DEFAULT_CONFIG_FILE);
                ConfigWrapper configWrapper = objectMapper.readValue(defaultConfig.getInputStream(), ConfigWrapper.class);
                saveConfigurations(configWrapper.getConfigurations());
            }
        } catch (IOException e) {
            logger.error("Failed to initialize configurations", e);
            throw new RuntimeException("Failed to initialize configurations", e);
        }
    }

    public List<AIConfiguration> getAllConfigurations() {
        try {
            return Files.exists(configPath) ?
                   objectMapper.readValue(configPath.toFile(), new TypeReference<List<AIConfiguration>>() {}) :
                   new ArrayList<>();
        } catch (IOException e) {
            logger.error("Failed to read configurations", e);
            return new ArrayList<>();
        }
    }

    public Optional<AIConfiguration> getActiveConfiguration() {
        return getAllConfigurations().stream()
                .filter(AIConfiguration::isActive)
                .findFirst();
    }

    public AIConfiguration createConfiguration(AIConfiguration config) {
        List<AIConfiguration> configs = getAllConfigurations();
        
        // Generate new ID if not provided
        if (config.getId() == null) {
            config.setId(UUID.randomUUID().toString());
        }

        // If this config is active, deactivate all others
        if (config.isActive()) {
            configs.forEach(c -> c.setActive(false));
        }

        configs.add(config);
        saveConfigurations(configs);
        return config;
    }

    public Optional<AIConfiguration> updateConfiguration(String id, AIConfiguration updatedConfig) {
        List<AIConfiguration> configs = getAllConfigurations();
        
        Optional<AIConfiguration> existingConfig = configs.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst();

        if (existingConfig.isPresent()) {
            AIConfiguration config = existingConfig.get();
            config.setModel(updatedConfig.getModel());
            config.setSystemPrompt(updatedConfig.getSystemPrompt());
            config.setAnalysisPrompt(updatedConfig.getAnalysisPrompt());
            config.setTemperature(updatedConfig.getTemperature());
            config.setMaxTokens(updatedConfig.getMaxTokens());
            
            // If this config is being activated, deactivate all others
            if (updatedConfig.isActive()) {
                configs.forEach(c -> c.setActive(false));
                config.setActive(true);
            } else {
                config.setActive(false);
            }

            saveConfigurations(configs);
            return Optional.of(config);
        }

        return Optional.empty();
    }

    public boolean deleteConfiguration(String id) {
        List<AIConfiguration> configs = getAllConfigurations();
        boolean removed = configs.removeIf(c -> c.getId().equals(id));
        if (removed) {
            saveConfigurations(configs);
        }
        return removed;
    }

    public boolean setActiveConfiguration(String id) {
        List<AIConfiguration> configs = getAllConfigurations();
        
        Optional<AIConfiguration> targetConfig = configs.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst();

        if (targetConfig.isPresent()) {
            // Deactivate all configurations
            configs.forEach(c -> c.setActive(false));
            // Activate the target configuration
            targetConfig.get().setActive(true);
            saveConfigurations(configs);
            return true;
        }

        return false;
    }

    private void saveConfigurations(List<AIConfiguration> configs) {
        try {
            objectMapper.writeValue(configPath.toFile(), configs);
        } catch (IOException e) {
            logger.error("Failed to save configurations", e);
            throw new RuntimeException("Failed to save configurations", e);
        }
    }

    // Wrapper class for the default configuration JSON structure
    private static class ConfigWrapper {
        private List<AIConfiguration> configurations;

        public List<AIConfiguration> getConfigurations() {
            return configurations;
        }

        public void setConfigurations(List<AIConfiguration> configurations) {
            this.configurations = configurations;
        }
    }
} 