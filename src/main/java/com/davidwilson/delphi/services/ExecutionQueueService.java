package com.davidwilson.delphi.services;

import com.davidwilson.delphi.entities.Submissions;
import com.davidwilson.delphi.repositories.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class ExecutionQueueService {

    private final BlockingQueue<Submissions> submissionQueue = new LinkedBlockingQueue<>();
    private final SubmissionRepository submissionRepository;
    private final FileExecutionService fileExecutionService;

    @Autowired
    public ExecutionQueueService(SubmissionRepository submissionRepository, FileExecutionService fileExecutionService) {
        this.submissionRepository = submissionRepository;
        this.fileExecutionService = fileExecutionService;
    }

    public void addSubmission(Submissions submission) {
        submissionQueue.add(submission);
    }

    @PostConstruct
    public void startProcessing() {
        Thread processorThread = new Thread(new SubmissionProcessor(submissionQueue, fileExecutionService, submissionRepository));
        processorThread.start();
    }
}