package com.example.workflow.dto;

import java.time.Instant;
import java.util.Map;

public record HistoricTaskDto(
        String id,
        String name,
        String assignee,
        String processInstanceId,
        Instant startTime,
        Instant endTime,
        Long durationInMillis,
        Map<String, Object> variables
) {
}





