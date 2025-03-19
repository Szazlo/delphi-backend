package com.davidwilson.delphi.repositories;

import com.davidwilson.delphi.entities.Submissions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submissions, Long> {
    Optional<Submissions> findById(UUID id);
    List<Submissions> findByUserId(String userId);
    Optional<Submissions> findByFileName(String fileName);
    Optional<Submissions> findByOutput(String output);
    Optional<Submissions> findByTimestamp(String timestamp);
    Optional<Submissions> findByStatus(String status);
    List<Submissions> findByAssignmentId(UUID assignmentId);

    @Query("SELECT s FROM Submissions s WHERE s.assignment.id = :assignmentId AND s.timestamp = (SELECT MAX(s2.timestamp) FROM Submissions s2 WHERE s2.assignment.id = :assignmentId AND s2.userId = s.userId)")
    List<Submissions> findLatestSubmissionsPerUserForAssignment(@Param("assignmentId") UUID assignmentId);
}
