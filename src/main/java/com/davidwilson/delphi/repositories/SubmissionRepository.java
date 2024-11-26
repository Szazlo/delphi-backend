package com.davidwilson.delphi.repositories;

import com.davidwilson.delphi.entities.Submissions;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
