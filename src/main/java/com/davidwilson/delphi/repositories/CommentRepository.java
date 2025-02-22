package com.davidwilson.delphi.repositories;

import com.davidwilson.delphi.entities.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findBySubmissionId(String submissionId);
    List<Comment> findBySubmissionIdAndFilePath(String submissionId, String filePath);
}
