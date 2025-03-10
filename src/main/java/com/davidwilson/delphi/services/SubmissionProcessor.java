package com.davidwilson.delphi.services;

import com.davidwilson.delphi.entities.Assignment;
import com.davidwilson.delphi.entities.Submissions;
import com.davidwilson.delphi.repositories.AssignmentRepository;
import com.davidwilson.delphi.repositories.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;

import com.davidwilson.delphi.repositories.TestCaseRepository;
import com.davidwilson.delphi.entities.TestCase;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.List;

public class SubmissionProcessor implements Runnable {

    private final BlockingQueue<Submissions> submissionQueue;
    private final FileExecutionService fileExecutionService;
    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final TestCaseRepository testCaseRepository;

    @Autowired
    public SubmissionProcessor(BlockingQueue<Submissions> submissionQueue, FileExecutionService fileExecutionService, SubmissionRepository submissionRepository, AssignmentRepository assignmentRepository, TestCaseRepository testCaseRepository) {
        this.submissionQueue = submissionQueue;
        this.fileExecutionService = fileExecutionService;
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.testCaseRepository = testCaseRepository;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Submissions submission = submissionQueue.take();
                UUID assignmentId = submission.getAssignment().getId();
                Assignment assignment = assignmentRepository.findById(assignmentId)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid assignment ID"));

                submission.setAssignment(assignment);
                submissionRepository.save(submission);

                List<TestCase> testCases = testCaseRepository.findByAssignmentId(assignmentId);

                fileExecutionService.runScriptAsync(submission.getFileName(), submission, submissionRepository, testCases);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}