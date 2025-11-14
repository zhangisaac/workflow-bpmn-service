package com.example.workflow.dto;

import java.time.Instant;

public record HistoricProcessInstanceDto(
        String id,
        String processDefinitionId,
        String processDefinitionKey,
        String businessKey,
        Instant startTime,
        Instant endTime,
        Long durationInMillis,
        String startUserId
) {
}





