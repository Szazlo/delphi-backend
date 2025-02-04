package com.davidwilson.delphi.repositories;

import com.davidwilson.delphi.entities.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    List<Assignment> findByGroupId(UUID groupId);
}
