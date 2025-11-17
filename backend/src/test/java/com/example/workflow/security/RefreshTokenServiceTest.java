package com.example.workflow.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenServiceTest {

    private RefreshTokenService refreshTokenService;

    private static final String TEST_USERNAME = "testuser";
    private static final long EXPIRATION_DAYS = 1;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService();
    }

    @Test
    void testGenerateRefreshToken() {
        // Given
        Instant expiration = Instant.now().plusSeconds(TimeUnit.DAYS.toSeconds(EXPIRATION_DAYS));

        // When
        String token = refreshTokenService.generateRefreshToken(TEST_USERNAME, expiration);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("-")); // UUID format
    }

    @Test
    void testValidateRefreshToken_ValidToken() {
        // Given
        Instant expiration = Instant.now().plusSeconds(TimeUnit.DAYS.toSeconds(EXPIRATION_DAYS));
        String token = refreshTokenService.generateRefreshToken(TEST_USERNAME, expiration);

        // When
        String username = refreshTokenService.validateRefreshToken(token);

        // Then
        assertEquals(TEST_USERNAME, username);
    }

    @Test
    void testValidateRefreshToken_InvalidToken() {
        // Given
        String invalidToken = "invalid-token";

        // When
        String username = refreshTokenService.validateRefreshToken(invalidToken);

        // Then
        assertNull(username);
    }

    @Test
    void testValidateRefreshToken_ExpiredToken() throws InterruptedException {
        // Given
        Instant expiration = Instant.now().plusSeconds(1); // Expires in 1 second
        String token = refreshTokenService.generateRefreshToken(TEST_USERNAME, expiration);

        // Wait for token to expire
        Thread.sleep(1100);

        // When
        String username = refreshTokenService.validateRefreshToken(token);

        // Then
        assertNull(username);
        // Token should be removed from storage
        assertNull(refreshTokenService.validateRefreshToken(token));
    }

    @Test
    void testRevokeRefreshToken() {
        // Given
        Instant expiration = Instant.now().plusSeconds(TimeUnit.DAYS.toSeconds(EXPIRATION_DAYS));
        String token = refreshTokenService.generateRefreshToken(TEST_USERNAME, expiration);
        assertNotNull(refreshTokenService.validateRefreshToken(token));

        // When
        refreshTokenService.revokeRefreshToken(token);

        // Then
        assertNull(refreshTokenService.validateRefreshToken(token));
    }

    @Test
    void testRevokeRefreshToken_NonExistentToken() {
        // Given
        String nonExistentToken = "non-existent-token";

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> refreshTokenService.revokeRefreshToken(nonExistentToken));
    }

    @Test
    void testRevokeAllTokensForUser() {
        // Given
        Instant expiration = Instant.now().plusSeconds(TimeUnit.DAYS.toSeconds(EXPIRATION_DAYS));
        String token1 = refreshTokenService.generateRefreshToken(TEST_USERNAME, expiration);
        String token2 = refreshTokenService.generateRefreshToken(TEST_USERNAME, expiration);
        String token3 = refreshTokenService.generateRefreshToken("otheruser", expiration);

        // When
        int revokedCount = refreshTokenService.revokeAllTokensForUser(TEST_USERNAME);

        // Then
        assertEquals(2, revokedCount);
        assertNull(refreshTokenService.validateRefreshToken(token1));
        assertNull(refreshTokenService.validateRefreshToken(token2));
        assertEquals("otheruser", refreshTokenService.validateRefreshToken(token3)); // Other user's token still valid
    }

    @Test
    void testRevokeAllTokensForUser_NoTokens() {
        // When
        int revokedCount = refreshTokenService.revokeAllTokensForUser("nonexistentuser");

        // Then
        assertEquals(0, revokedCount);
    }

    @Test
    void testCleanupExpiredTokens() throws InterruptedException {
        // Given
        Instant expiredExpiration = Instant.now().plusSeconds(1); // Expires in 1 second
        Instant validExpiration = Instant.now().plusSeconds(TimeUnit.DAYS.toSeconds(EXPIRATION_DAYS));

        String expiredToken = refreshTokenService.generateRefreshToken(TEST_USERNAME, expiredExpiration);
        String validToken = refreshTokenService.generateRefreshToken(TEST_USERNAME, validExpiration);

        // Wait for token to expire
        Thread.sleep(1100);

        // When
        refreshTokenService.cleanupExpiredTokens();

        // Then
        assertNull(refreshTokenService.validateRefreshToken(expiredToken));
        assertEquals(TEST_USERNAME, refreshTokenService.validateRefreshToken(validToken));
    }

    @Test
    void testGetActiveTokenCount() {
        // Given
        Instant expiration = Instant.now().plusSeconds(TimeUnit.DAYS.toSeconds(EXPIRATION_DAYS));
        assertEquals(0, refreshTokenService.getActiveTokenCount());

        // When
        refreshTokenService.generateRefreshToken(TEST_USERNAME, expiration);
        refreshTokenService.generateRefreshToken(TEST_USERNAME, expiration);
        refreshTokenService.generateRefreshToken("user2", expiration);

        // Then
        assertEquals(3, refreshTokenService.getActiveTokenCount());
    }

    @Test
    void testRefreshTokenInfo_IsExpired() {
        // Given
        Instant pastExpiration = Instant.now().minusSeconds(100);
        Instant futureExpiration = Instant.now().plusSeconds(100);

        RefreshTokenService.RefreshTokenInfo expiredInfo = new RefreshTokenService.RefreshTokenInfo(
                TEST_USERNAME, pastExpiration, Instant.now());
        RefreshTokenService.RefreshTokenInfo validInfo = new RefreshTokenService.RefreshTokenInfo(
                TEST_USERNAME, futureExpiration, Instant.now());

        // When & Then
        assertTrue(expiredInfo.isExpired());
        assertFalse(validInfo.isExpired());
    }
}

