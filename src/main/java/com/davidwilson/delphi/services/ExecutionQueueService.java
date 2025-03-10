package com.davidwilson.delphi.services;

import com.davidwilson.delphi.entities.Submissions;
import com.davidwilson.delphi.repositories.AssignmentRepository;
import com.davidwilson.delphi.repositories.SubmissionRepository;
import com.davidwilson.delphi.repositories.TestCaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.*;

@Service
public class ExecutionQueueService {

    private final BlockingQueue<Submissions> submissionQueue = new LinkedBlockingQueue<>();
    private final SubmissionRepository submissionRepository;
    private final FileExecutionService fileExecutionService;
    private final AssignmentRepository assignmentRepository;
    private final TestCaseRepository testCaseRepository;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Autowired
    public ExecutionQueueService(SubmissionRepository submissionRepository,
                                 FileExecutionService fileExecutionService,
                                 AssignmentRepository assignmentRepository, TestCaseRepository testCaseRepository) {
        this.submissionRepository = submissionRepository;
        this.fileExecutionService = fileExecutionService;
        this.assignmentRepository = assignmentRepository;
        this.testCaseRepository = testCaseRepository;
    }

    public void addSubmission(Submissions submission) {
        boolean added = submissionQueue.offer(submission);
        if (!added) {
            throw new RuntimeException("Queue is full. Submission could not be added.");
        }
    }

    @PostConstruct
    public void startProcessing() {
        executorService.submit(new SubmissionProcessor(submissionQueue, fileExecutionService,
                submissionRepository, assignmentRepository, testCaseRepository));
    }
}
