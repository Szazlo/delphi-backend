package com.davidwilson.delphi.repositories;

import com.davidwilson.delphi.entities.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface UserGroupRepository extends JpaRepository<UserGroup, UUID> {
    List<UserGroup> findByUserId(String userId);
    List<UserGroup> findByGroupId(UUID groupId);
}
