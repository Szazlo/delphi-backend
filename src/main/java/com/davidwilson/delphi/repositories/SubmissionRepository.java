package com.davidwilson.delphi.repositories;

import com.davidwilson.delphi.entities.Submissions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface SubmissionRepository extends JpaRepository<Submissions, Long> {
    Optional<Submissions> findById(Long id);
    Optional<Submissions> findByUserId(String userId);
    Optional<Submissions> findByFileName(String fileName);
    Optional<Submissions> findByOutput(String output);
    Optional<Submissions> findByTimestamp(String timestamp);
    Optional<Submissions> findByStatus(String status);
}
