package com.davidwilson.delphi.services;

import com.davidwilson.delphi.entities.Group;
import com.davidwilson.delphi.repositories.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    public List<Group> getActiveGroups(String owner) {
        return groupRepository.findByOwnerAndArchivedFalse(owner);
    }

    public List<Group> getArchivedGroups(String owner) {
        return groupRepository.findByOwnerAndArchivedTrue(owner);
    }

    public boolean archiveGroup(UUID groupId, String owner) {
        Optional<Group> groupOptional = groupRepository.findById(groupId);
        if (groupOptional.isPresent()) {
            Group group = groupOptional.get();
            group.setArchived(true);
            groupRepository.save(group);
            return true;
        }
        return false;
    }

    public boolean restoreGroup(UUID groupId, String owner) {
        Optional<Group> groupOptional = groupRepository.findById(groupId);
        if (groupOptional.isPresent()) {
            Group group = groupOptional.get();
            group.setArchived(false);
            groupRepository.save(group);
            return true;
        }
        return false;
    }
}