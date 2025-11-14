package com.example.workflow.dto;

import java.util.Map;

public record TaskCompletionRequest(
        Map<String, Object> variables
) {
}





