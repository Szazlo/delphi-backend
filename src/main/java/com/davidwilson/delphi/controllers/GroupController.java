package com.davidwilson.delphi.controllers;

import com.davidwilson.delphi.entities.Group;
import com.davidwilson.delphi.entities.UserGroup;
import com.davidwilson.delphi.repositories.GroupRepository;
import com.davidwilson.delphi.repositories.UserGroupRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;

    public GroupController(GroupRepository groupRepository, UserGroupRepository userGroupRepository) {
        this.groupRepository = groupRepository;
        this.userGroupRepository = userGroupRepository;
    }

    @PostMapping
    public ResponseEntity<Group> createGroup(@RequestBody Group group) {
        return ResponseEntity.ok(groupRepository.save(group));
    }

    @GetMapping
    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable UUID id) {
        Optional<Group> group = groupRepository.findById(id);
        if (group.isPresent()) {
            groupRepository.delete(group.get());
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Group> getGroupById(@PathVariable UUID id) {
        return groupRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Group> updateGroup(@PathVariable UUID id, @RequestBody Group group) {
        Optional<Group> existingGroup = groupRepository.findById(id);
        if (existingGroup.isPresent()) {
            group.setId(id);
            return ResponseEntity.ok(groupRepository.save(group));
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{groupId}/users")
    public ResponseEntity<UserGroup> addUserToGroup(@PathVariable UUID groupId, @RequestParam String userId, @RequestParam(defaultValue = "member") String role) {
        Optional<Group> group = groupRepository.findById(groupId);
        if (group.isPresent()) {
            UserGroup userGroup = new UserGroup();
            userGroup.setUserId(userId);
            userGroup.setGroup(group.get());
            userGroup.setRole(role);
            return ResponseEntity.ok(userGroupRepository.save(userGroup));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{groupId}/users")
    public List<UserGroup> getUsersInGroup(@PathVariable UUID groupId) {
        return userGroupRepository.findByGroupId(groupId);
    }

    @DeleteMapping("/{groupId}/users/{userId}")
    public ResponseEntity<Void> removeUserFromGroup(@PathVariable UUID groupId, @PathVariable String userId) {
        List<UserGroup> memberships = userGroupRepository.findByUserId(userId);
        for (UserGroup membership : memberships) {
            if (membership.getGroup().getId().equals(groupId)) {
                userGroupRepository.delete(membership);
                return ResponseEntity.ok().build();
            }
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{userId}/groups")
    public List<Group> getGroupsForUser(@PathVariable String userId) {
        List<UserGroup> userGroups = userGroupRepository.findByUserId(String.valueOf(UUID.fromString(userId)));
        List<Group> memberGroups = userGroups.stream()
                .map(UserGroup::getGroup)
                .collect(Collectors.toList());

        List<Group> ownerGroups = groupRepository.findByOwner(userId);

        memberGroups.addAll(ownerGroups);
        return memberGroups;
    }

}
