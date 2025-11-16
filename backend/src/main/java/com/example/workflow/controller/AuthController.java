package com.example.workflow.controller;

import com.example.workflow.dto.LoginRequest;
import com.example.workflow.dto.LoginResponse;
import com.example.workflow.dto.RefreshTokenRequest;
import com.example.workflow.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authenticate users, issue JWTs, refresh tokens, and logout")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate by username and password",
            description = "Returns access token and refresh token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token",
            description = "Use a valid refresh token to obtain a new access token")
    public ResponseEntity<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user",
            description = "Blacklists the current access token and revokes refresh token. " +
                    "Requires authentication. Pass refresh token in request body if available.")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshTokenRequest request,
                                       HttpServletRequest httpRequest,
                                       Authentication authentication) {
        // Extract access token from Authorization header
        String accessToken = extractTokenFromRequest(httpRequest);

        // Extract refresh token from request body (if provided)
        String refreshToken = request != null ? request.refreshToken() : null;

        authService.logout(accessToken, refreshToken);

        return ResponseEntity.ok().build();
    }

    /**
     * Extract JWT token from Authorization header.
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}





