package com.davidwilson.delphi.services;

import com.davidwilson.delphi.entities.Assignment;
import com.davidwilson.delphi.entities.Submissions;
import com.davidwilson.delphi.repositories.AssignmentRepository;
import com.davidwilson.delphi.repositories.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;

public class SubmissionProcessor implements Runnable {

    private final BlockingQueue<Submissions> submissionQueue;
    private final FileExecutionService fileExecutionService;
    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;

    @Autowired
    public SubmissionProcessor(BlockingQueue<Submissions> submissionQueue, FileExecutionService fileExecutionService, SubmissionRepository submissionRepository, AssignmentRepository assignmentRepository) {
        this.submissionQueue = submissionQueue;
        this.fileExecutionService = fileExecutionService;
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
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

                fileExecutionService.runScriptAsync(submission.getFileName(), submission, submissionRepository);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}