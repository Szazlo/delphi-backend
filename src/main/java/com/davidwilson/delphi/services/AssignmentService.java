package com.davidwilson.delphi.services;

import com.davidwilson.delphi.entities.Assignment;
import com.davidwilson.delphi.entities.Group;
import com.davidwilson.delphi.repositories.AssignmentRepository;
import com.davidwilson.delphi.repositories.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

@Service
public class AssignmentService {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Transactional
    public Assignment createAssignment(Map<String, Object> assignmentData) {
        UUID groupId = UUID.fromString((String) assignmentData.get("group_id"));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid group ID"));

        Assignment assignment = new Assignment();
        assignment.setTitle((String) assignmentData.get("title"));
        assignment.setDescription((String) assignmentData.get("description"));

        Object timeLimitObj = assignmentData.get("time_limit");
        if (timeLimitObj instanceof Integer) {
            assignment.setTimeLimit(((Integer) timeLimitObj).floatValue());
        } else if (timeLimitObj instanceof Double) {
            assignment.setTimeLimit(((Double) timeLimitObj).floatValue());
        } else {
            throw new IllegalArgumentException("Invalid type for time_limit");
        }

        assignment.setMemoryLimit((Integer) assignmentData.get("memory_limit"));
        assignment.setMaxScore((Integer) assignmentData.get("max_score"));

        assignment.setGradeWeight((Integer) assignmentData.get("grade_weight"));

        assignment.setGroup(group);

        if (assignmentData.containsKey("due_date")) {
            assignment.setDueDate(Timestamp.valueOf((String) assignmentData.get("due_date")));
        }

        return assignmentRepository.save(assignment);
    }
}