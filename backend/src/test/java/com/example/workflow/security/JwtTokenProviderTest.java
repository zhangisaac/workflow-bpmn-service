package com.example.workflow.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @Mock
    private TokenBlacklistService blacklistService;

    private JwtTokenProvider jwtTokenProvider;

    private static final String TEST_SECRET = "test-secret-key-for-testing-purposes-only-min-32-chars";
    private static final long EXPIRATION_MINUTES = 10;
    private static final long REFRESH_EXPIRATION_DAYS = 1;
    private UserDetails testUserDetails;
    private Authentication testAuthentication;

    @BeforeEach
    void setUp() {
        // Create a real JwtTokenProvider instance with test values
        jwtTokenProvider = new JwtTokenProvider(
                TEST_SECRET,
                EXPIRATION_MINUTES,
                REFRESH_EXPIRATION_DAYS,
                blacklistService
        );

        testUserDetails = new User(
                "testuser",
                "password",
                List.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_ADMIN")
                )
        );

        testAuthentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                testUserDetails,
                null,
                testUserDetails.getAuthorities()
        );
    }

    @Test
    void testGenerateToken_WithAuthentication() {
        // When
        String token = jwtTokenProvider.generateToken(testAuthentication);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Verify token can be parsed
        Claims claims = parseToken(token);
        assertEquals("testuser", claims.getSubject());
        assertNotNull(claims.get("roles"));
    }

    @Test
    void testGenerateToken_WithUserDetails() {
        // When
        String token = jwtTokenProvider.generateToken(testUserDetails, null);

        // Then
        assertNotNull(token);
        Claims claims = parseToken(token);
        assertEquals("testuser", claims.getSubject());
    }

    @Test
    void testGenerateToken_WithAdditionalClaims() {
        // Given
        Map<String, Object> additionalClaims = Map.of("custom", "value");

        // When
        String token = jwtTokenProvider.generateToken(testUserDetails, additionalClaims);

        // Then
        assertNotNull(token);
        Claims claims = parseToken(token);
        assertEquals("value", claims.get("custom"));
    }

    @Test
    void testValidateToken_ValidToken() {
        // Given
        String token = jwtTokenProvider.generateToken(testAuthentication);
        when(blacklistService.isTokenBlacklisted(token)).thenReturn(false);

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertTrue(isValid);
        verify(blacklistService).isTokenBlacklisted(token);
    }

    @Test
    void testValidateToken_BlacklistedToken() {
        // Given
        String token = jwtTokenProvider.generateToken(testAuthentication);
        when(blacklistService.isTokenBlacklisted(token)).thenReturn(true);

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertFalse(isValid);
        verify(blacklistService).isTokenBlacklisted(token);
    }

    @Test
    void testValidateToken_InvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";
        when(blacklistService.isTokenBlacklisted(invalidToken)).thenReturn(false);

        // When
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testGetUsernameFromToken() {
        // Given
        String token = jwtTokenProvider.generateToken(testAuthentication);

        // When
        String username = jwtTokenProvider.getUsernameFromToken(token);

        // Then
        assertEquals("testuser", username);
    }

    @Test
    void testGetRolesFromToken() {
        // Given
        String token = jwtTokenProvider.generateToken(testAuthentication);

        // When
        List<String> roles = jwtTokenProvider.getRolesFromToken(token);

        // Then
        assertNotNull(roles);
        assertTrue(roles.contains("ROLE_USER"));
        assertTrue(roles.contains("ROLE_ADMIN"));
    }

    @Test
    void testGetExpirationInstant() {
        // Given
        String token = jwtTokenProvider.generateToken(testAuthentication);

        // When
        Instant expiration = jwtTokenProvider.getExpirationInstant(token);

        // Then
        assertNotNull(expiration);
        assertTrue(expiration.isAfter(Instant.now()));
    }

    @Test
    void testGetRefreshTokenExpiration() {
        // When
        Instant expiration = jwtTokenProvider.getRefreshTokenExpiration();

        // Then
        assertNotNull(expiration);
        assertTrue(expiration.isAfter(Instant.now()));
        // Should be approximately 1 day from now (allow some tolerance)
        long daysDifference = java.time.Duration.between(Instant.now(), expiration).toDays();
        assertTrue(daysDifference >= 0 && daysDifference <= 1, 
                "Expiration should be 0-1 days from now, but was: " + daysDifference);
    }

    @Test
    void testConstructor_WithBase64Secret() {
        // Given
        String base64Secret = "dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLXB1cnBvc2VzLW9ubHktbWluLTMyLWNoYXJz";
        JwtTokenProvider provider = new JwtTokenProvider(
                base64Secret,
                EXPIRATION_MINUTES,
                REFRESH_EXPIRATION_DAYS,
                blacklistService
        );

        // When
        String token = provider.generateToken(testAuthentication);

        // Then
        assertNotNull(token);
    }

    @Test
    void testConstructor_WithPlainTextSecret() {
        // Given - secret that doesn't match base64 pattern but is long enough (>= 32 chars for HMAC-SHA256)
        String plainSecret = "plain-text-secret-key-for-testing-purposes-only";
        JwtTokenProvider provider = new JwtTokenProvider(
                plainSecret,
                EXPIRATION_MINUTES,
                REFRESH_EXPIRATION_DAYS,
                blacklistService
        );

        // When
        String token = provider.generateToken(testAuthentication);

        // Then
        assertNotNull(token);
    }

    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

