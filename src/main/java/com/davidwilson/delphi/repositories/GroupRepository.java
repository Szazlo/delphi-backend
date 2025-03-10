package com.davidwilson.delphi.repositories;

import com.davidwilson.delphi.entities.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {
    List<Group> findByOwnerAndArchivedFalse(String owner);
    List<Group> findByOwnerAndArchivedTrue(String owner);
    Optional<Group> findByIdAndOwner(UUID id, String owner);
    List<Group> findByOwner(String owner);
}