package com.davidwilson.delphi.entities;

import java.util.UUID;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;

@Entity
@Table(name = "submissions")
public class Submissions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @Column(name="user_id")
    private String userId;

    @Column(name="file_name")
    private String fileName;

    @Column(length = 10000)
    private String output;

    @Column(length = 10000)
    private String lint_output;

    @Column(length = 10000)
    private String ai_output;

    @Column(name="runtime")
    private Integer runtime; // in ms

    @Column(name="memory")
    private Integer memory; // in KB

    private String timestamp;

    private String status;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getLintOutput() {
        return lint_output;
    }

    public void setLintOutput(String lint_output) {
        this.lint_output = lint_output;
    }

    public String getAIOutput() {
        return ai_output;
    }

    public void setAIOutput(String ai_output) {
        this.ai_output = ai_output;
    }

    public Integer getRuntime() {
        return runtime;
    }

    public void setRuntime(Integer runtime) {
        this.runtime = runtime;
    }

    public Integer getMemory() {
        return memory;
    }

    public void setMemoryUsage(Integer memory) {
        this.memory = memory;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}