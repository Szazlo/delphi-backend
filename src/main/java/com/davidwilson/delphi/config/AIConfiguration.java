package com.davidwilson.delphi.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AIConfiguration {
    private String id;
    private String model;
    private String systemPrompt;
    private String analysisPrompt;
    private Double temperature;
    private Integer maxTokens;
    private boolean active;

    // Default constructor
    public AIConfiguration() {}

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getAnalysisPrompt() {
        return analysisPrompt;
    }

    public void setAnalysisPrompt(String analysisPrompt) {
        this.analysisPrompt = analysisPrompt;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
} 