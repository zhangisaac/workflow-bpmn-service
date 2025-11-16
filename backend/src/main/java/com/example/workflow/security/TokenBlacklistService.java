package com.example.workflow.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to manage blacklisted JWT tokens.
 * Tokens are blacklisted when users logout or tokens are revoked.
 * 
 * This implementation uses in-memory storage. For production, consider:
 * - Using Redis for distributed systems
 * - Using H2 database table for persistence
 * - Implementing token expiration cleanup
 */
@Service
public class TokenBlacklistService {

    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);

    // Store blacklisted tokens with their expiration time
    // Key: token (or token hash), Value: expiration timestamp
    private final Map<String, Instant> blacklist = new ConcurrentHashMap<>();

    /**
     * Add a token to the blacklist.
     * The token will be considered invalid until it naturally expires.
     * 
     * @param token The JWT token to blacklist
     * @param expirationTime When the token naturally expires (for cleanup)
     */
    public void blacklistToken(String token, Instant expirationTime) {
        // Use token hash to save memory (optional, can use full token)
        String tokenKey = token.length() > 50 ? token.substring(0, 50) + "..." : token;
        blacklist.put(tokenKey, expirationTime);
        logger.debug("Token blacklisted: {} (expires at: {})", tokenKey, expirationTime);
    }

    /**
     * Check if a token is blacklisted.
     * 
     * @param token The JWT token to check
     * @return true if the token is blacklisted, false otherwise
     */
    public boolean isTokenBlacklisted(String token) {
        String tokenKey = token.length() > 50 ? token.substring(0, 50) + "..." : token;
        Instant expiration = blacklist.get(tokenKey);
        
        if (expiration == null) {
            return false;
        }

        // If token has naturally expired, remove from blacklist and return false
        if (Instant.now().isAfter(expiration)) {
            blacklist.remove(tokenKey);
            logger.debug("Expired blacklisted token removed: {}", tokenKey);
            return false;
        }

        return true;
    }

    /**
     * Remove a token from the blacklist (e.g., if it was blacklisted by mistake).
     * 
     * @param token The JWT token to remove from blacklist
     */
    public void removeFromBlacklist(String token) {
        String tokenKey = token.length() > 50 ? token.substring(0, 50) + "..." : token;
        blacklist.remove(tokenKey);
        logger.debug("Token removed from blacklist: {}", tokenKey);
    }

    /**
     * Clean up expired tokens from the blacklist.
     * This should be called periodically to prevent memory leaks.
     */
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        int removed = 0;
        
        var iterator = blacklist.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (now.isAfter(entry.getValue())) {
                iterator.remove();
                removed++;
            }
        }
        
        if (removed > 0) {
            logger.info("Cleaned up {} expired blacklisted tokens", removed);
        }
    }

    /**
     * Get the current size of the blacklist.
     * 
     * @return Number of tokens in the blacklist
     */
    public int getBlacklistSize() {
        return blacklist.size();
    }
}

