package com.davidwilson.delphi.controllers;

import com.davidwilson.delphi.entities.Assignment;
import com.davidwilson.delphi.entities.AssignmentGroup;
import com.davidwilson.delphi.entities.Group;
import com.davidwilson.delphi.entities.TestCase;
import com.davidwilson.delphi.entities.Submissions;
import com.davidwilson.delphi.config.AIConfiguration;
import com.davidwilson.delphi.repositories.AssignmentRepository;
import com.davidwilson.delphi.repositories.TestCaseRepository;
import com.davidwilson.delphi.repositories.AssignmentGroupRepository;
import com.davidwilson.delphi.repositories.SubmissionRepository;
import com.davidwilson.delphi.services.AssignmentService;
import com.davidwilson.delphi.services.AIConfigurationService;
import com.davidwilson.delphi.services.AIAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private AssignmentGroupRepository assignmentGroupRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private AIConfigurationService aiConfigurationService;

    @Autowired
    private AIAnalysisService aiAnalysisService;

    @GetMapping("/{id}")
    public ResponseEntity<Assignment> getAssignment(@PathVariable UUID id) {
        Optional<Assignment> assignment = assignmentRepository.findById(id);
        return assignment.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/submissions")
    public ResponseEntity<List<Submissions>> getLatestSubmissionsPerUser(@PathVariable UUID id) {
        List<Submissions> submissions = submissionRepository.findLatestSubmissionsPerUserForAssignment(id);
        if (submissions.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        
        // Sort by timestamp in descending order (newest first)
        submissions.sort((o1, o2) -> o2.getTimestamp().compareTo(o1.getTimestamp()));
        return new ResponseEntity<>(submissions, HttpStatus.OK);
    }

    @GetMapping("/{id}/submissions/analyze")
    public ResponseEntity<String> analyzeSubmissionsSubset(@PathVariable UUID id) {
        // Get all latest submissions per user
        List<Submissions> allSubmissions = submissionRepository.findLatestSubmissionsPerUserForAssignment(id);
        
        if (allSubmissions.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        // Calculate subset size (1/10th of total submissions, minimum 2)
        if (allSubmissions.size() < 2) {
            return new ResponseEntity<>("Not enough submissions to analyze", HttpStatus.NO_CONTENT);
        }
        int subsetSize = Math.max(2, allSubmissions.size() / 10);
        
        // Take the first subsetSize submissions
        List<Submissions> subset = allSubmissions.subList(0, subsetSize);
        
        // Build the prompt for analysis
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert code reviewer analyzing student submissions. Focus on identifying patterns and common issues across the submissions.\n\n");
        prompt.append("For each submission, analyze:\n");
        prompt.append("1. Code structure and organization\n");
        prompt.append("2. Algorithm implementation\n");
        prompt.append("3. Error handling\n");
        prompt.append("4. Code style and readability\n\n");
        prompt.append("Provide a concise summary in this format:\n\n");
        prompt.append("COMMON ISSUES:\n");
        prompt.append("- List the 3-5 most frequent issues found across submissions\n");
        prompt.append("- For each issue, briefly explain why it's problematic\n\n");
        prompt.append("OVERALL FEEDBACK:\n");
        prompt.append("- 2-3 key points about the general quality of submissions\n");
        prompt.append("- 1-2 specific suggestions for improvement\n\n");
        prompt.append("Submissions to analyze:\n\n");
        
        for (Submissions submission : subset) {
            prompt.append("Submission from user ").append(submission.getUserId()).append(":\n");
            prompt.append(submission.getAIOutput()).append("\n\n");
        }
        
        // Call the AI service to analyze
        String analysis = aiAnalysisService.analyzeSubmissionText(prompt.toString());
        
        return new ResponseEntity<>(analysis, HttpStatus.OK);
    }

    // Create an assignment for a group
    @PostMapping
    public ResponseEntity<Assignment> createAssignment(@RequestBody Map<String, Object> assignmentData) {
        Assignment newAssignment = assignmentService.createAssignment(assignmentData);
        return new ResponseEntity<>(newAssignment, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Assignment> updateAssignment(@PathVariable UUID id, @RequestBody Map<String, Object> updates) {
        Optional<Assignment> existingAssignmentOpt = assignmentRepository.findById(id);
        if (existingAssignmentOpt.isPresent()) {
            Assignment existingAssignment = existingAssignmentOpt.get();

            updates.forEach((key, value) -> {
                switch (key) {
                    case "title":
                        existingAssignment.setTitle((String) value);
                        break;
                    case "description":
                        existingAssignment.setDescription((String) value);
                        break;
                    case "group":
                        Group group = new Group();
                        group.setId(UUID.fromString((String) ((Map<String, Object>) value).get("id")));
                        existingAssignment.setGroup(group);
                        break;
                    case "dueDate":
                        existingAssignment.setDueDate(Timestamp.valueOf((String) value));
                        break;
                    case "timeLimit":
                        existingAssignment.setTimeLimit(((Number) value).floatValue());
                        break;
                    case "memoryLimit":
                        existingAssignment.setMemoryLimit((Integer) value);
                        break;
                    case "maxScore":
                        existingAssignment.setMaxScore((Integer) value);
                        break;
                    case "gradeWeight":
                        existingAssignment.setGradeWeight((Integer) value);
                        break;
                }
            });

            Assignment updatedAssignment = assignmentRepository.save(existingAssignment);
            return new ResponseEntity<>(updatedAssignment, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAssignment(@PathVariable UUID id) {
        if (assignmentRepository.existsById(id)) {
            assignmentRepository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    // Get assignments for a specific group
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<Assignment>> getAssignmentsForGroup(@PathVariable UUID groupId) {
        List<Assignment> assignments = assignmentRepository.findByGroupId(groupId);
        return new ResponseEntity<>(assignments, HttpStatus.OK);
    }

    // Get test cases for an assignment
    @GetMapping("/{assignmentId}/testcases")
    public ResponseEntity<List<TestCase>> getTestCasesForAssignment(@PathVariable UUID assignmentId) {
        List<TestCase> testCases = testCaseRepository.findByAssignmentId(assignmentId);
        return new ResponseEntity<>(testCases, HttpStatus.OK);
    }

    // Add a test case to an assignment
    @PostMapping("/{assignmentId}/addtestcase")
    public ResponseEntity<TestCase> addTestCaseToAssignment(@PathVariable UUID assignmentId, @RequestBody TestCase testCase) {
        testCase.setAssignment(new Assignment());
        testCase.getAssignment().setId(assignmentId);
        TestCase savedTestCase = testCaseRepository.save(testCase);
        return new ResponseEntity<>(savedTestCase, HttpStatus.CREATED);
    }

    // Assign an assignment to a group
    @PostMapping("/{assignmentId}/assign-to-group/{groupId}")
    public ResponseEntity<String> assignAssignmentToGroup(@PathVariable UUID assignmentId, @PathVariable UUID groupId) {
        AssignmentGroup assignmentGroup = new AssignmentGroup();
        assignmentGroup.setAssignment(new Assignment());
        assignmentGroup.getAssignment().setId(assignmentId);
        assignmentGroup.setGroup(new Group());
        assignmentGroup.getGroup().setId(groupId);

        assignmentGroupRepository.save(assignmentGroup);
        return new ResponseEntity<>("Assignment assigned to group successfully.", HttpStatus.OK);
    }
}
