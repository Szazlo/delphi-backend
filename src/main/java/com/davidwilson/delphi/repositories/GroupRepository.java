package com.davidwilson.delphi.repositories;

import com.davidwilson.delphi.entities.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {
}
