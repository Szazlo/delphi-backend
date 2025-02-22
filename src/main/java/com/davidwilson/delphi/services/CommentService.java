package com.davidwilson.delphi.services;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.davidwilson.delphi.entities.Comment;
import com.davidwilson.delphi.repositories.CommentRepository;

@Service
@Transactional
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    public Comment createComment(Comment comment) {
        comment.setId(UUID.randomUUID());
        comment.setCreatedAt(OffsetDateTime.now());
        comment.setUpdatedAt(OffsetDateTime.now());
        return commentRepository.save(comment);
    }

    public List<Comment> getCommentsBySubmission(String submissionId) {
        return commentRepository.findBySubmissionId(submissionId);
    }

    public List<Comment> getCommentsBySubmissionAndFile(String submissionId, String filePath) {
        return commentRepository.findBySubmissionIdAndFilePath(submissionId, filePath);
    }

    public Comment updateComment(UUID id, String text) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        comment.setText(text);
        comment.setUpdatedAt(OffsetDateTime.now());
        return commentRepository.save(comment);
    }

    public void deleteComment(UUID id) {
        commentRepository.deleteById(id);
    }
}