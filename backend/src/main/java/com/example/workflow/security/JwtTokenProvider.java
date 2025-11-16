package com.example.workflow.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationInMinutes;
    private final long refreshExpirationInDays;
    private final TokenBlacklistService blacklistService;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-in-minutes}") long expirationInMinutes,
            @Value("${jwt.refresh-expiration-in-days:1}") long refreshExpirationInDays,
            TokenBlacklistService blacklistService
    ) {
        byte[] keyBytes;
        if (secret.matches("^[A-Za-z0-9+/=]{32,}$")) {
            keyBytes = Decoders.BASE64.decode(secret);
        } else {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationInMinutes = expirationInMinutes;
        this.refreshExpirationInDays = refreshExpirationInDays;
        this.blacklistService = blacklistService;
    }

    public String generateToken(Authentication authentication) {
        UserDetails user = (UserDetails) authentication.getPrincipal();
        return generateToken(user, null);
    }

    public String generateToken(UserDetails userDetails, Map<String, Object> additionalClaims) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationInMinutes, ChronoUnit.MINUTES);
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        JwtBuilder builder = Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .claim("roles", authorities.stream().map(GrantedAuthority::getAuthority).toList());

        if (additionalClaims != null) {
            additionalClaims.forEach(builder::claim);
        }

        return builder.signWith(secretKey, SignatureAlgorithm.HS256).compact();
    }

    public boolean validateToken(String token) {
        // First check if token is blacklisted
        if (blacklistService.isTokenBlacklisted(token)) {
            return false;
        }

        // Then validate token signature and expiration
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generate a refresh token (long-lived token for obtaining new access tokens).
     * Note: In this implementation, refresh tokens are managed by RefreshTokenService,
     * not as JWTs. This method is kept for potential future use.
     *
     * @param userDetails User details
     * @return Refresh token expiration time
     */
    public Instant getRefreshTokenExpiration() {
        return Instant.now().plus(refreshExpirationInDays, ChronoUnit.DAYS);
    }

    public String getUsernameFromToken(String token) {
        return getAllClaims(token).getSubject();
    }

    public List<String> getRolesFromToken(String token) {
        Claims claims = getAllClaims(token);
        Object roles = claims.get("roles");
        if (roles instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    public Instant getExpirationInstant(String token) {
        Claims claims = getAllClaims(token);
        Date expiration = claims.getExpiration();
        return expiration != null ? expiration.toInstant() : Instant.now();
    }

    private Claims getAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

