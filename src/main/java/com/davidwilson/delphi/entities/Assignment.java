package com.davidwilson.delphi.entities;

import jakarta.persistence.*;
import java.util.UUID;
import java.sql.Timestamp;

@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    private String title;
    private String description;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    private Timestamp dueDate;  // No need to specify nullable, it's the default

    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    private Float timeLimit = 2.0f;
    private Integer memoryLimit = 256;
    private Integer maxScore = 100;
    private Integer gradeWeight = 70;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public Timestamp getDueDate() {
        return dueDate;
    }

    public void setDueDate(Timestamp dueDate) {
        this.dueDate = dueDate;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Float getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(Float timeLimit) {
        this.timeLimit = timeLimit;
    }

    public Integer getMemoryLimit() {
        return memoryLimit;
    }

    public void setMemoryLimit(Integer memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    public Integer getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Integer maxScore) {
        this.maxScore = maxScore;
    }

    public Integer getGradeWeight() { return gradeWeight; }

    public void setGradeWeight(Integer gradeWeight) { this.gradeWeight = gradeWeight; }
}