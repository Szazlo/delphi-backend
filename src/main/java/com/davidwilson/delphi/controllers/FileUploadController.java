package com.davidwilson.delphi.controllers;
import com.davidwilson.delphi.entities.Submissions;
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
    Logger logger = Logger.getLogger(FileUploadController.class.getName());

    public FileUploadController(FileUploadService fileUploadService, SubmissionRepository submissionRepository, ExecutionQueueService executionQueueService, SubmissionController submissionController) {
        this.fileUploadService = fileUploadService;
        this.submissionRepository = submissionRepository;
        this.executionQueueService = executionQueueService;
        this.submissionController = submissionController;
    }



    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file, @RequestHeader("Authorization") String token) {
        Map<String, Object> response = new HashMap<>();
        try {
            // limit file types
            if (!Objects.requireNonNull(file.getOriginalFilename()).endsWith(ZIP_EXTENSION) ||
                    !ZIP_MIME_TYPE.equals(file.getContentType())) {
                response.put("message", "Bad file type. Formats accepted: .zip");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            //TODO: Check for files with same hash (duplicate files)
            //TODO: Associate files with user
            //TODO: Compilation and running in container, return results
            //TODO: Auto cleanup files after a certain time
            String userID = extractUserID(token);
            if (userID.isEmpty()) {
                response.put("message", "User not found");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            String filePath = fileUploadService.saveFile(file);
            logger.info("File upload success - File name: " + file.getOriginalFilename() + ", File path: " + filePath);

            Submissions submission = new Submissions();
            submission.setUserId(userID);
            submission.setFileName(file.getOriginalFilename());
            submission.setTimestamp(String.valueOf(System.currentTimeMillis()));
            submission.setStatus("Pending");
            submissionRepository.save(submission);

            executionQueueService.addSubmission(submission);
            UUID submissionId = submission.getId();
            response.put("message", "File uploaded successfully. Submission queued for execution.");
            response.put("submissionId", submissionId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IOException e) {
            logger.severe("Failed to upload the file: " + e.getMessage());
            response.put("message", "Failed to upload the file, please try again.");
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

