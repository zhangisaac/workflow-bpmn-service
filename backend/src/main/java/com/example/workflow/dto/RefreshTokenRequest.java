package com.example.workflow.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for token refresh endpoint.
 */
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}

