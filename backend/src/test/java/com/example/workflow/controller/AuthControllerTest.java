package com.example.workflow.controller;

import com.example.workflow.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthController authController;

    @Test
    void testLogout_WithBearerToken() {
        // Given
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer test-token-123");
        
        // When - Call logout which uses extractTokenFromRequest internally
        ResponseEntity<Void> response = authController.logout(null, httpRequest, authentication);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).logout("test-token-123", null);
    }

    @Test
    void testLogout_WithoutBearerPrefix() {
        // Given
        when(httpRequest.getHeader("Authorization")).thenReturn("test-token-123");

        // When
        ResponseEntity<Void> response = authController.logout(null, httpRequest, authentication);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).logout(null, null);
    }

    @Test
    void testLogout_WithNullHeader() {
        // Given
        when(httpRequest.getHeader("Authorization")).thenReturn(null);

        // When
        ResponseEntity<Void> response = authController.logout(null, httpRequest, authentication);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).logout(null, null);
    }

    @Test
    void testLogout_WithRefreshToken() {
        // Given
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer test-token-123");
        com.example.workflow.dto.RefreshTokenRequest request = 
                new com.example.workflow.dto.RefreshTokenRequest("refresh-token-456");

        // When
        ResponseEntity<Void> response = authController.logout(request, httpRequest, authentication);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).logout("test-token-123", "refresh-token-456");
    }
}

