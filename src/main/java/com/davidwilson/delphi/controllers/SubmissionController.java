package com.davidwilson.delphi.controllers;

import com.davidwilson.delphi.entities.Submissions;
import com.davidwilson.delphi.repositories.SubmissionRepository;
import com.davidwilson.delphi.entities.SubmissionReviews;
import com.davidwilson.delphi.repositories.SubmissionReviewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private SubmissionReviewsRepository submissionReviewsRepository;

    private static Logger logger = Logger.getLogger(SubmissionController.class.getName());

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

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadSubmission(@PathVariable UUID id) {
        Optional<Submissions> submission = submissionRepository.findById(id);
        String fileName = submission.map(Submissions::getFileName).orElse(null);

        if (fileName == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            byte[] fileContent = Files.readAllBytes(filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(fileContent);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/reviewer/{id}")
    public ResponseEntity<List<SubmissionReviews>> getSubmissionsForReviewer(@PathVariable String id) {
        List<SubmissionReviews> submissionReviews = submissionReviewsRepository.findBySubmissionId(id);
        if (submissionReviews.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(submissionReviews, HttpStatus.OK);
    }

    @PostMapping("/{id}/addreviewer")
    public ResponseEntity<String> assignSubmissionReviewer(@RequestParam String reviewerId , @PathVariable String id) {
        if (submissionReviewsRepository.existsBySubmissionId(id)) {
            return new ResponseEntity<>("Reviewer already assigned", HttpStatus.BAD_REQUEST);
        }
        SubmissionReviews submissionReviews = new SubmissionReviews();
        submissionReviews.setSubmissionId(id);
        submissionReviews.setReviewerId(reviewerId);
        submissionReviews.setStatus("Pending");
        submissionReviews.setCreatedAt(String.valueOf(System.currentTimeMillis()));
        logger.info("Assigning reviewer " + reviewerId + " to submission " + id);
        submissionReviewsRepository.save(submissionReviews);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}