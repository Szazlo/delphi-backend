package com.davidwilson.delphi.controllers;
import com.davidwilson.delphi.entities.Assignment;
import com.davidwilson.delphi.entities.Submissions;
import com.davidwilson.delphi.repositories.AssignmentRepository;
import com.davidwilson.delphi.repositories.SubmissionRepository;
import com.davidwilson.delphi.services.ExecutionQueueService;
import com.davidwilson.delphi.services.FileUploadService;
import com.davidwilson.delphi.services.FileExecutionService;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.HashMap;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private static final String ZIP_MIME_TYPE = "application/zip";
    private static final String ZIP_EXTENSION = ".zip";
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(FileUploadController.class);


    private final FileUploadService fileUploadService;
    private final SubmissionRepository submissionRepository;
    private final ExecutionQueueService executionQueueService;
    private final SubmissionController submissionController;
    private final AssignmentRepository assignmentRepository;
    Logger logger = Logger.getLogger(FileUploadController.class.getName());

    public FileUploadController(FileUploadService fileUploadService, SubmissionRepository submissionRepository, ExecutionQueueService executionQueueService, SubmissionController submissionController, AssignmentRepository assignmentRepository) {
        this.fileUploadService = fileUploadService;
        this.submissionRepository = submissionRepository;
        this.executionQueueService = executionQueueService;
        this.submissionController = submissionController;
        this.assignmentRepository = assignmentRepository;
    }



    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam String assignmentId, @RequestHeader("Authorization") String token) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Check file type
            if (!Objects.requireNonNull(file.getOriginalFilename()).endsWith(ZIP_EXTENSION) ||
                    !ZIP_MIME_TYPE.equals(file.getContentType())) {
                response.put("message", "Bad file type. Formats accepted: .zip");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            String userID = extractUserID(token);
            if (userID.isEmpty()) {
                response.put("message", "User not found");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Fetch assignment
            Assignment assignment = assignmentRepository.findById(UUID.fromString(assignmentId))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid assignment ID"));

            // Save file
            String filePath = fileUploadService.saveFile(file);
            log.info("File uploaded successfully: " + file.getOriginalFilename() + ", Path: " + filePath);

            // Create submission and set all necessary fields
            Submissions submission = new Submissions();
            submission.setUserId(userID);
            submission.setFileName(file.getOriginalFilename());
            submission.setTimestamp(String.valueOf(System.currentTimeMillis()));
            submission.setStatus("Pending");
            submission.setAssignment(assignment);  // ✅ FIX: Setting the assignment entity
            submissionRepository.save(submission);

            // Add submission to execution queue
            executionQueueService.addSubmission(submission);

            response.put("message", "File uploaded successfully. Submission queued for execution.");
            response.put("submissionId", submission.getId());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.error("Assignment not found: " + e.getMessage());
            response.put("message", "Invalid assignment ID.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            log.error("File upload failed: " + e.getMessage());
            response.put("message", "Failed to upload the file. Please try again.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Handle MaxUploadSizeExceededException
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleMaxSizeException(MaxUploadSizeExceededException e) {
        logger.severe("File size exceeds the maximum limit (100MB): " + e.getMessage());
        return new ResponseEntity<>("File size exceeds the maximum limit (100MB).", HttpStatus.PAYLOAD_TOO_LARGE);
    }

    private String extractUserID(String token) {
        String[] tokenParts = token.split("\\.");
        String payload = new String(java.util.Base64.getDecoder().decode(tokenParts[1]));
        String[] payloadParts = payload.split(",");
        for (String part : payloadParts) {
            if (part.contains("sub")) {
                return part.split(":")[1].replace("\"", "");
            }
        }
        return "";
    }
}

