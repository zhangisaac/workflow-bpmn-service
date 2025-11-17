package com.example.workflow.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TokenBlacklistServiceTest {

    private TokenBlacklistService tokenBlacklistService;

    private static final String TEST_TOKEN = "test-jwt-token-string-that-is-long-enough-to-test-token-key-generation";
    private static final String SHORT_TOKEN = "short";

    @BeforeEach
    void setUp() {
        tokenBlacklistService = new TokenBlacklistService();
    }

    @Test
    void testBlacklistToken() {
        // Given
        Instant expiration = Instant.now().plusSeconds(600);

        // When
        tokenBlacklistService.blacklistToken(TEST_TOKEN, expiration);

        // Then
        assertTrue(tokenBlacklistService.isTokenBlacklisted(TEST_TOKEN));
    }

    @Test
    void testBlacklistToken_ShortToken() {
        // Given
        Instant expiration = Instant.now().plusSeconds(600);

        // When
        tokenBlacklistService.blacklistToken(SHORT_TOKEN, expiration);

        // Then
        assertTrue(tokenBlacklistService.isTokenBlacklisted(SHORT_TOKEN));
    }

    @Test
    void testBlacklistToken_LongToken() {
        // Given
        String longToken = "a".repeat(100);
        Instant expiration = Instant.now().plusSeconds(600);

        // When
        tokenBlacklistService.blacklistToken(longToken, expiration);

        // Then
        assertTrue(tokenBlacklistService.isTokenBlacklisted(longToken));
    }

    @Test
    void testIsTokenBlacklisted_NotBlacklisted() {
        // When
        boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(TEST_TOKEN);

        // Then
        assertFalse(isBlacklisted);
    }

    @Test
    void testIsTokenBlacklisted_ExpiredToken() throws InterruptedException {
        // Given
        Instant expiration = Instant.now().plusSeconds(1); // Expires in 1 second
        tokenBlacklistService.blacklistToken(TEST_TOKEN, expiration);
        assertTrue(tokenBlacklistService.isTokenBlacklisted(TEST_TOKEN));

        // Wait for expiration
        Thread.sleep(1100);

        // When
        boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(TEST_TOKEN);

        // Then
        assertFalse(isBlacklisted); // Expired tokens are automatically removed
    }

    @Test
    void testRemoveFromBlacklist() {
        // Given
        Instant expiration = Instant.now().plusSeconds(600);
        tokenBlacklistService.blacklistToken(TEST_TOKEN, expiration);
        assertTrue(tokenBlacklistService.isTokenBlacklisted(TEST_TOKEN));

        // When
        tokenBlacklistService.removeFromBlacklist(TEST_TOKEN);

        // Then
        assertFalse(tokenBlacklistService.isTokenBlacklisted(TEST_TOKEN));
    }

    @Test
    void testRemoveFromBlacklist_NonExistentToken() {
        // When & Then - should not throw exception
        assertDoesNotThrow(() -> tokenBlacklistService.removeFromBlacklist("non-existent-token"));
    }

    @Test
    void testCleanupExpiredTokens() throws InterruptedException {
        // Given
        Instant expiredExpiration = Instant.now().plusSeconds(1); // Expires in 1 second
        Instant validExpiration = Instant.now().plusSeconds(600);

        String expiredToken = "expired-token";
        String validToken = "valid-token";

        tokenBlacklistService.blacklistToken(expiredToken, expiredExpiration);
        tokenBlacklistService.blacklistToken(validToken, validExpiration);

        assertEquals(2, tokenBlacklistService.getBlacklistSize());

        // Wait for expiration
        Thread.sleep(1100);

        // When
        tokenBlacklistService.cleanupExpiredTokens();

        // Then
        assertFalse(tokenBlacklistService.isTokenBlacklisted(expiredToken));
        assertTrue(tokenBlacklistService.isTokenBlacklisted(validToken));
        assertEquals(1, tokenBlacklistService.getBlacklistSize());
    }

    @Test
    void testCleanupExpiredTokens_NoExpiredTokens() {
        // Given
        Instant expiration = Instant.now().plusSeconds(600);
        tokenBlacklistService.blacklistToken(TEST_TOKEN, expiration);
        int initialSize = tokenBlacklistService.getBlacklistSize();

        // When
        tokenBlacklistService.cleanupExpiredTokens();

        // Then
        assertEquals(initialSize, tokenBlacklistService.getBlacklistSize());
    }

    @Test
    void testGetBlacklistSize() {
        // Given
        Instant expiration = Instant.now().plusSeconds(600);
        assertEquals(0, tokenBlacklistService.getBlacklistSize());

        // When
        tokenBlacklistService.blacklistToken("token1", expiration);
        tokenBlacklistService.blacklistToken("token2", expiration);
        tokenBlacklistService.blacklistToken("token3", expiration);

        // Then
        assertEquals(3, tokenBlacklistService.getBlacklistSize());
    }

    @Test
    void testGetBlacklistSize_AfterRemoval() {
        // Given
        Instant expiration = Instant.now().plusSeconds(600);
        tokenBlacklistService.blacklistToken("token1", expiration);
        tokenBlacklistService.blacklistToken("token2", expiration);
        assertEquals(2, tokenBlacklistService.getBlacklistSize());

        // When
        tokenBlacklistService.removeFromBlacklist("token1");

        // Then
        assertEquals(1, tokenBlacklistService.getBlacklistSize());
    }
}

