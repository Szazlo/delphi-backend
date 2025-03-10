package com.davidwilson.delphi.controllers;

import com.davidwilson.delphi.entities.Assignment;
import com.davidwilson.delphi.entities.AssignmentGroup;
import com.davidwilson.delphi.entities.Group;
import com.davidwilson.delphi.entities.TestCase;
import com.davidwilson.delphi.repositories.AssignmentRepository;
import com.davidwilson.delphi.repositories.TestCaseRepository;
import com.davidwilson.delphi.repositories.AssignmentGroupRepository;
import com.davidwilson.delphi.services.AssignmentService;
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
    private AssignmentService assignmentService;

    @GetMapping("/{id}")
    public ResponseEntity<Assignment> getAssignment(@PathVariable UUID id) {
        Optional<Assignment> assignment = assignmentRepository.findById(id);
        return assignment.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
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
