package com.example.workflow.dto;

import java.time.Instant;
import java.util.List;

public record TaskDto(
        String id,
        String name,
        String description,
        String assignee,
        String processInstanceId,
        Instant createdTime,
        Instant dueDate,
        List<String> candidateGroups,
        List<String> candidateUsers
) {
}





