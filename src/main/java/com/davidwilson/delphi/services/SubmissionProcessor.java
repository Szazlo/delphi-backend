package com.davidwilson.delphi.services;

import com.davidwilson.delphi.entities.Submissions;
import com.davidwilson.delphi.repositories.SubmissionRepository;

import java.util.concurrent.BlockingQueue;

public class SubmissionProcessor implements Runnable {

    private final BlockingQueue<Submissions> submissionQueue;
    private final FileExecutionService fileExecutionService;
    private final SubmissionRepository submissionRepository;

    public SubmissionProcessor(BlockingQueue<Submissions> submissionQueue, FileExecutionService fileExecutionService, SubmissionRepository submissionRepository) {
        this.submissionQueue = submissionQueue;
        this.fileExecutionService = fileExecutionService;
        this.submissionRepository = submissionRepository;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Submissions submission = submissionQueue.take();
                fileExecutionService.runScriptAsync(submission.getFileName(), submission, submissionRepository);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}