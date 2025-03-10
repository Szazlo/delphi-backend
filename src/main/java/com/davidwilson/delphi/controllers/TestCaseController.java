package com.davidwilson.delphi.controllers;

import com.davidwilson.delphi.entities.Assignment;
import com.davidwilson.delphi.entities.TestCase;
import com.davidwilson.delphi.repositories.AssignmentRepository;
import com.davidwilson.delphi.repositories.TestCaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/testcases")
public class TestCaseController {

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @GetMapping("/{id}")
    public ResponseEntity<TestCase> getTestCase(@PathVariable UUID id) {
        Optional<TestCase> testCase = testCaseRepository.findById(id);
        return testCase.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/assignment/{assignmentId}")
    public ResponseEntity<Iterable<TestCase>> getTestCasesByAssignment(@PathVariable UUID assignmentId) {
        Iterable<TestCase> testCases = testCaseRepository.findByAssignmentId(assignmentId);
        return ResponseEntity.ok(testCases);
    }

    @PostMapping
    public ResponseEntity<TestCase> createTestCase(@RequestBody Map<String, Object> testCaseData) {
        try {
            // Extract assignmentId from the request body
            String assignmentId = (String) testCaseData.get("assignmentId");
            if (assignmentId == null) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            // Find the assignment
            Optional<Assignment> assignmentOpt = assignmentRepository.findById(UUID.fromString(assignmentId));
            if (!assignmentOpt.isPresent()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            TestCase testCase = new TestCase();
            testCase.setAssignment(assignmentOpt.get());

            if (testCaseData.containsKey("input")) testCase.setInput((String) testCaseData.get("input"));
            if (testCaseData.containsKey("expectedOutput")) testCase.setExpectedOutput((String) testCaseData.get("expectedOutput"));
            if (testCaseData.containsKey("description")) testCase.setDescription((String) testCaseData.get("description"));

            TestCase savedTestCase = testCaseRepository.save(testCase);
            return new ResponseEntity<>(savedTestCase, HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TestCase> updateTestCase(@PathVariable UUID id, @RequestBody Map<String, Object> testCaseData) {
        try {
            Optional<TestCase> existingTestCaseOpt = testCaseRepository.findById(id);
            if (existingTestCaseOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            TestCase testCase = existingTestCaseOpt.get();

            if (testCaseData.containsKey("input")) testCase.setInput((String) testCaseData.get("input"));
            if (testCaseData.containsKey("expectedOutput")) testCase.setExpectedOutput((String) testCaseData.get("expectedOutput"));
            if (testCaseData.containsKey("description")) testCase.setDescription((String) testCaseData.get("description"));

            if (testCaseData.containsKey("assignmentId")) {
                String assignmentId = (String) testCaseData.get("assignmentId");
                Optional<Assignment> assignmentOpt = assignmentRepository.findById(UUID.fromString(assignmentId));
                if (assignmentOpt.isEmpty()) {
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }
                testCase.setAssignment(assignmentOpt.get());
            }

            TestCase updatedTestCase = testCaseRepository.save(testCase);
            return ResponseEntity.ok(updatedTestCase);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTestCase(@PathVariable UUID id) {
        if (!testCaseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        testCaseRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}