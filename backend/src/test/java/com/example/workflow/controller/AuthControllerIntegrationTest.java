package com.example.workflow.controller;

import com.example.workflow.dto.LoginRequest;
import com.example.workflow.dto.RefreshTokenRequest;
import com.example.workflow.repository.UserAccountRepository;
import com.example.workflow.security.JwtTokenProvider;
import com.example.workflow.security.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private com.example.workflow.security.TokenBlacklistService tokenBlacklistService;

    private String authToken;

    @BeforeEach
    void setUp() {
        // Clear SecurityContext to ensure clean state
        SecurityContextHolder.clearContext();
        
        // Set up authentication context for testing
        var userAccount = userAccountRepository.findByUsername("user")
                .orElseThrow();

        UserDetails userDetails = User.builder()
                .username(userAccount.username())
                .password("")
                .authorities(userAccount.roles().stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList())
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userAccount.roles().stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList()
        );

        authToken = jwtTokenProvider.generateToken(authentication);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        // Clear SecurityContext after each test
        SecurityContextHolder.clearContext();
        // Note: We don't clear the blacklist here as it's shared state
        // Tests should use fresh tokens to avoid blacklist issues
    }

    @Test
    void testLogin_Success() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("user", "user");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.username").value("user"))
                .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    void testLogin_InvalidCredentials() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("user", "wrong-password");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogin_UserNotFound() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("nonexistent", "password");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogin_InvalidRequest() throws Exception {
        // Given - missing username
        String invalidJson = "{\"password\":\"test\"}";

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRefreshToken_Success() throws Exception {
        // Given - first login to get refresh token
        LoginRequest loginRequest = new LoginRequest("user", "user");
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract refresh token from response
        String refreshToken = objectMapper.readTree(loginResponse)
                .get("refreshToken").asText();

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);

        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").value(refreshToken))
                .andExpect(jsonPath("$.username").value("user"));
    }

    @Test
    void testRefreshToken_InvalidToken() throws Exception {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-refresh-token");

        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRefreshToken_ExpiredToken() throws Exception {
        // Given - create an expired refresh token
        String expiredToken = refreshTokenService.generateRefreshToken(
                "user",
                java.time.Instant.now().minusSeconds(100) // Expired
        );

        RefreshTokenRequest request = new RefreshTokenRequest(expiredToken);

        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLogout_Success() throws Exception {
        // Given - generate a completely fresh token to avoid any blacklist issues
        var userAccount = userAccountRepository.findByUsername("user")
                .orElseThrow();
        UserDetails userDetails = User.builder()
                .username(userAccount.username())
                .password("")
                .authorities(userAccount.roles().stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList())
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userAccount.roles().stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList()
        );
        String freshToken = jwtTokenProvider.generateToken(authentication);
        
        // Ensure token is not blacklisted
        if (tokenBlacklistService.isTokenBlacklisted(freshToken)) {
            tokenBlacklistService.removeFromBlacklist(freshToken);
        }
        
        RefreshTokenRequest request = new RefreshTokenRequest("some-refresh-token");

        // When & Then
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + freshToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testLogout_WithoutRefreshToken() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void testLogout_Unauthenticated() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}

