package com.davidwilson.delphi.controllers;

import com.davidwilson.delphi.entities.Assignment;
import com.davidwilson.delphi.entities.AssignmentGroup;
import com.davidwilson.delphi.entities.Group;
import com.davidwilson.delphi.entities.TestCase;
import com.davidwilson.delphi.repositories.AssignmentRepository;
import com.davidwilson.delphi.repositories.TestCaseRepository;
import com.davidwilson.delphi.repositories.AssignmentGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    // Create an assignment for a group
    @PostMapping("/create")
    public ResponseEntity<Assignment> createAssignment(@RequestBody Assignment assignment) {
        // Set defaults for optional fields if not provided
        if (assignment.getTimeLimit() == null) assignment.setTimeLimit(2.0f);
        if (assignment.getMemoryLimit() == null) assignment.setMemoryLimit(256);
        if (assignment.getMaxScore() == null) assignment.setMaxScore(100);

        Assignment newAssignment = assignmentRepository.save(assignment);
        return new ResponseEntity<>(newAssignment, HttpStatus.CREATED);
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
