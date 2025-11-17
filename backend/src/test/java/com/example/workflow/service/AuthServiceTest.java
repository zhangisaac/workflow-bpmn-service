package com.example.workflow.service;

import com.example.workflow.dto.LoginRequest;
import com.example.workflow.dto.LoginResponse;
import com.example.workflow.dto.RefreshTokenRequest;
import com.example.workflow.model.UserAccount;
import com.example.workflow.repository.UserAccountRepository;
import com.example.workflow.security.JwtTokenProvider;
import com.example.workflow.security.RefreshTokenService;
import com.example.workflow.security.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private TokenBlacklistService blacklistService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "password";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private UserAccount testUserAccount;
    private UserDetails testUserDetails;
    private Authentication testAuthentication;

    @BeforeEach
    void setUp() {
        testUserAccount = new UserAccount(
                TEST_USERNAME,
                "encoded-password",
                Set.of("ROLE_USER"),
                Set.of("employees")
        );

        testUserDetails = new User(
                TEST_USERNAME,
                "encoded-password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        testAuthentication = new UsernamePasswordAuthenticationToken(
                testUserDetails,
                null,
                testUserDetails.getAuthorities()
        );
    }

    @Test
    void testLogin_Success() {
        // Given
        LoginRequest request = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);
        Instant accessExpiry = Instant.now().plusSeconds(600);
        Instant refreshExpiry = Instant.now().plusSeconds(86400);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(testAuthentication);
        when(tokenProvider.generateToken(testAuthentication)).thenReturn(ACCESS_TOKEN);
        when(tokenProvider.getExpirationInstant(ACCESS_TOKEN)).thenReturn(accessExpiry);
        when(tokenProvider.getRefreshTokenExpiration()).thenReturn(refreshExpiry);
        when(refreshTokenService.generateRefreshToken(eq(TEST_USERNAME), eq(refreshExpiry)))
                .thenReturn(REFRESH_TOKEN);
        when(userAccountRepository.findByUsername(TEST_USERNAME))
                .thenReturn(Optional.of(testUserAccount));

        // When
        LoginResponse response = authService.login(request);

        // Then
        assertNotNull(response);
        assertEquals("Bearer", response.tokenType());
        assertEquals(ACCESS_TOKEN, response.accessToken());
        assertEquals(REFRESH_TOKEN, response.refreshToken());
        assertEquals(accessExpiry, response.expiresAt());
        assertEquals(refreshExpiry, response.refreshExpiresAt());
        assertEquals(TEST_USERNAME, response.username());
        assertTrue(response.roles().contains("ROLE_USER"));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenProvider).generateToken(testAuthentication);
        verify(refreshTokenService).generateRefreshToken(eq(TEST_USERNAME), eq(refreshExpiry));
    }

    @Test
    void testLogin_InvalidCredentials() {
        // Given
        LoginRequest request = new LoginRequest(TEST_USERNAME, "wrong-password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When & Then
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(tokenProvider, never()).generateToken(any());
        verify(refreshTokenService, never()).generateRefreshToken(anyString(), any());
    }

    @Test
    void testLogin_UserNotFound() {
        // Given
        LoginRequest request = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(testAuthentication);
        when(tokenProvider.generateToken(testAuthentication)).thenReturn(ACCESS_TOKEN);
        when(tokenProvider.getExpirationInstant(ACCESS_TOKEN)).thenReturn(Instant.now());
        when(tokenProvider.getRefreshTokenExpiration()).thenReturn(Instant.now());
        when(refreshTokenService.generateRefreshToken(anyString(), any())).thenReturn(REFRESH_TOKEN);
        when(userAccountRepository.findByUsername(TEST_USERNAME))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    @Test
    void testRefreshToken_Success() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest(REFRESH_TOKEN);
        Instant accessExpiry = Instant.now().plusSeconds(600);
        Instant refreshExpiry = Instant.now().plusSeconds(86400);

        when(refreshTokenService.validateRefreshToken(REFRESH_TOKEN)).thenReturn(TEST_USERNAME);
        when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(testUserDetails);
        when(userAccountRepository.findByUsername(TEST_USERNAME))
                .thenReturn(Optional.of(testUserAccount));
        when(tokenProvider.generateToken(eq(testUserDetails), eq(null))).thenReturn(ACCESS_TOKEN);
        when(tokenProvider.getExpirationInstant(ACCESS_TOKEN)).thenReturn(accessExpiry);
        when(tokenProvider.getRefreshTokenExpiration()).thenReturn(refreshExpiry);

        // When
        LoginResponse response = authService.refreshToken(request);

        // Then
        assertNotNull(response);
        assertEquals("Bearer", response.tokenType());
        assertEquals(ACCESS_TOKEN, response.accessToken());
        assertEquals(REFRESH_TOKEN, response.refreshToken()); // Same refresh token
        assertEquals(accessExpiry, response.expiresAt());
        assertEquals(refreshExpiry, response.refreshExpiresAt());
        assertEquals(TEST_USERNAME, response.username());

        verify(refreshTokenService).validateRefreshToken(REFRESH_TOKEN);
        verify(tokenProvider).generateToken(eq(testUserDetails), eq(null));
    }

    @Test
    void testRefreshToken_InvalidToken() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");
        when(refreshTokenService.validateRefreshToken("invalid-token")).thenReturn(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> authService.refreshToken(request));
        verify(tokenProvider, never()).generateToken(any(), any());
    }

    @Test
    void testRefreshToken_UserNotFound() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest(REFRESH_TOKEN);
        when(refreshTokenService.validateRefreshToken(REFRESH_TOKEN)).thenReturn(TEST_USERNAME);
        when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(testUserDetails);
        when(userAccountRepository.findByUsername(TEST_USERNAME))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> authService.refreshToken(request));
    }

    @Test
    void testLogout_WithBothTokens() {
        // Given
        Instant expiration = Instant.now().plusSeconds(600);
        when(tokenProvider.getExpirationInstant(ACCESS_TOKEN)).thenReturn(expiration);

        // When
        authService.logout(ACCESS_TOKEN, REFRESH_TOKEN);

        // Then
        verify(blacklistService).blacklistToken(ACCESS_TOKEN, expiration);
        verify(refreshTokenService).revokeRefreshToken(REFRESH_TOKEN);
    }

    @Test
    void testLogout_WithAccessTokenOnly() {
        // Given
        Instant expiration = Instant.now().plusSeconds(600);
        when(tokenProvider.getExpirationInstant(ACCESS_TOKEN)).thenReturn(expiration);

        // When
        authService.logout(ACCESS_TOKEN, null);

        // Then
        verify(blacklistService).blacklistToken(ACCESS_TOKEN, expiration);
        verify(refreshTokenService, never()).revokeRefreshToken(anyString());
    }

    @Test
    void testLogout_WithRefreshTokenOnly() {
        // When
        authService.logout(null, REFRESH_TOKEN);

        // Then
        verify(blacklistService, never()).blacklistToken(anyString(), any());
        verify(refreshTokenService).revokeRefreshToken(REFRESH_TOKEN);
    }

    @Test
    void testLogout_WithEmptyTokens() {
        // When
        authService.logout("", "");

        // Then
        verify(blacklistService, never()).blacklistToken(anyString(), any());
        verify(refreshTokenService, never()).revokeRefreshToken(anyString());
    }

    @Test
    void testLogoutAll() {
        // Given
        when(refreshTokenService.revokeAllTokensForUser(TEST_USERNAME)).thenReturn(3);

        // When
        authService.logoutAll(TEST_USERNAME);

        // Then
        verify(refreshTokenService).revokeAllTokensForUser(TEST_USERNAME);
    }
}

