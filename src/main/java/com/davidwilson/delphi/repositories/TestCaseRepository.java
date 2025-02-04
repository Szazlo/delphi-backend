package com.davidwilson.delphi.repositories;

import com.davidwilson.delphi.entities.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {
    List<TestCase> findByAssignmentId(UUID assignmentId);
}
