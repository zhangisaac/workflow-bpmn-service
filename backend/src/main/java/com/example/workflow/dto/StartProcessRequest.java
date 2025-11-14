package com.example.workflow.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record StartProcessRequest(
        @NotBlank(message = "Process definition key is required")
        String processDefinitionKey,
        String businessKey,
        Map<String, Object> variables
) {
}





