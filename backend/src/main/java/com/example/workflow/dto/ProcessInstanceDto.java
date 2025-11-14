package com.example.workflow.dto;

import java.time.Instant;

public record ProcessInstanceDto(
        String id,
        String processDefinitionId,
        String processDefinitionKey,
        String businessKey,
        boolean suspended,
        Instant startTime,
        String startUserId
) {
}





