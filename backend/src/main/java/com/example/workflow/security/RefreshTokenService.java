package com.example.workflow.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to manage refresh tokens.
 * Refresh tokens are long-lived tokens used to obtain new access tokens.
 * <p>
 * This implementation uses in-memory storage. For production, consider:
 * - Using H2 database table for persistence
 * - Using Redis for distributed systems
 * - Implementing token rotation
 * - Adding device/session tracking
 */
@Service
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    /**
     * Internal storage for refresh tokens.
     * Key: refresh token string, Value: RefreshTokenInfo (username and expiration)
     */
    private final Map<String, RefreshTokenInfo> refreshTokens = new ConcurrentHashMap<>();

    /**
     * Generate a new refresh token for a user.
     *
     * @param username       The username for whom the token is generated
     * @param expirationTime When the refresh token expires
     * @return The refresh token string
     */
    public String generateRefreshToken(String username, Instant expirationTime) {
        // Generate a secure random token (UUID-based)
        String token = UUID.randomUUID() + "-" + UUID.randomUUID();
        Instant now = Instant.now();

        RefreshTokenInfo info = new RefreshTokenInfo(username, expirationTime, now);
        refreshTokens.put(token, info);

        logger.debug("Generated refresh token for user: {} (expires at: {})", username, expirationTime);
        return token;
    }

    /**
     * Validate a refresh token and return the associated username.
     *
     * @param refreshToken The refresh token to validate
     * @return The username if token is valid, null otherwise
     */
    public String validateRefreshToken(String refreshToken) {
        RefreshTokenInfo info = refreshTokens.get(refreshToken);

        if (info == null) {
            logger.debug("Refresh token not found: {}", refreshToken.substring(0, Math.min(20, refreshToken.length())));
            return null;
        }

        if (info.isExpired()) {
            // Remove expired token
            refreshTokens.remove(refreshToken);
            logger.debug("Refresh token expired and removed: {}", refreshToken.substring(0, Math.min(20, refreshToken.length())));
            return null;
        }

        return info.username();
    }

    /**
     * Revoke a refresh token (e.g., on logout or security breach).
     *
     * @param refreshToken The refresh token to revoke
     */
    public void revokeRefreshToken(String refreshToken) {
        RefreshTokenInfo removed = refreshTokens.remove(refreshToken);
        if (removed != null) {
            logger.debug("Refresh token revoked for user: {}", removed.username());
        }
    }

    /**
     * Revoke all refresh tokens for a specific user (e.g., on password change).
     *
     * @param username The username whose tokens should be revoked
     * @return Number of tokens revoked
     */
    public int revokeAllTokensForUser(String username) {
        int count = 0;
        var iterator = refreshTokens.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().username().equals(username)) {
                iterator.remove();
                count++;
            }
        }

        if (count > 0) {
            logger.info("Revoked {} refresh tokens for user: {}", count, username);
        }

        return count;
    }

    /**
     * Clean up expired refresh tokens.
     * This should be called periodically to prevent memory leaks.
     */
    public void cleanupExpiredTokens() {
        int removed = 0;
        var iterator = refreshTokens.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
                removed++;
            }
        }

        if (removed > 0) {
            logger.info("Cleaned up {} expired refresh tokens", removed);
        }
    }

    /**
     * Get the current number of active refresh tokens.
     *
     * @return Number of active refresh tokens
     */
    public int getActiveTokenCount() {
        return refreshTokens.size();
    }

    /**
         * Information stored with each refresh token.
         */
        public record RefreshTokenInfo(String username, Instant expiration, Instant createdAt) {

        public boolean isExpired() {
                return Instant.now().isAfter(expiration);
            }
        }
}

