package com.davidwilson.delphi.controllers;

import com.davidwilson.delphi.entities.Comment;
import com.davidwilson.delphi.repositories.CommentRepository;
import com.davidwilson.delphi.services.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping
    public ResponseEntity<Comment> createComment(@RequestBody Comment comment) {
        return ResponseEntity.ok(commentService.createComment(comment));
    }

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<Comment>> getCommentsBySubmission(@PathVariable String submissionId) {
        return ResponseEntity.ok(commentService.getCommentsBySubmission(submissionId));
    }

    @GetMapping("/submission/{submissionId}/file/{filePath}")
    public ResponseEntity<List<Comment>> getCommentsByFile(
            @PathVariable String submissionId,
            @PathVariable String filePath) {
        return ResponseEntity.ok(commentService.getCommentsBySubmissionAndFile(submissionId, filePath));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Comment> updateComment(
            @PathVariable UUID id,
            @RequestBody String text) {
        return ResponseEntity.ok(commentService.updateComment(id, text));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable UUID id) {
        commentService.deleteComment(id);
        return ResponseEntity.ok().build();
    }
}