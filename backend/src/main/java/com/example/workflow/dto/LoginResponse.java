package com.example.workflow.dto;

import java.time.Instant;
import java.util.List;

public record LoginResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
        Instant expiresAt,
        Instant refreshExpiresAt,
        String username,
        List<String> roles
) {
    public static LoginResponse of(String tokenType,
                                   String accessToken,
                                   String refreshToken,
                                   Instant expiresAt,
                                   Instant refreshExpiresAt,
                                   String username,
                                   List<String> roles) {
        return new LoginResponse(tokenType, accessToken, refreshToken, expiresAt, refreshExpiresAt, username, roles);
    }
}





