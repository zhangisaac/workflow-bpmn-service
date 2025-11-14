package com.example.workflow.dto;

import java.util.List;

public record DeploymentResponse(
        String deploymentId,
        String deploymentName,
        List<String> deployedProcessDefinitionKeys
) {
}





