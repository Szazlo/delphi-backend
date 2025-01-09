package com.davidwilson.delphi.controllers;

import com.davidwilson.delphi.entities.Submissions;
import com.davidwilson.delphi.repositories.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    @Autowired
    private SubmissionRepository submissionRepository;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Submissions>> getSubmissionsForUser(@PathVariable String userId) {
        List<Submissions> submissions = submissionRepository.findByUserId(userId);
        if (submissions.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        //sort by time
        submissions.sort((o1, o2) -> o2.getTimestamp().compareTo(o1.getTimestamp()));
        return new ResponseEntity<>(submissions, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Submissions> getSubmissionById(@PathVariable UUID id) {
        Optional<Submissions> submission = submissionRepository.findById(id);
        return submission.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}