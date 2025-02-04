package com.davidwilson.delphi.repositories;

import com.davidwilson.delphi.entities.AssignmentGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssignmentGroupRepository extends JpaRepository<AssignmentGroup, UUID> {
    List<AssignmentGroup> findByGroupId(UUID groupId);
}
