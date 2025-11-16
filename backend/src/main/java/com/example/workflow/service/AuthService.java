package com.example.workflow.service;

import com.example.workflow.dto.LoginRequest;
import com.example.workflow.dto.LoginResponse;
import com.example.workflow.dto.RefreshTokenRequest;
import com.example.workflow.model.UserAccount;
import com.example.workflow.repository.UserAccountRepository;
import com.example.workflow.security.JwtTokenProvider;
import com.example.workflow.security.RefreshTokenService;
import com.example.workflow.security.TokenBlacklistService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService blacklistService;
    private final UserDetailsService userDetailsService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       UserAccountRepository userAccountRepository,
                       RefreshTokenService refreshTokenService,
                       TokenBlacklistService blacklistService,
                       UserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userAccountRepository = userAccountRepository;
        this.refreshTokenService = refreshTokenService;
        this.blacklistService = blacklistService;
        this.userDetailsService = userDetailsService;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserDetails principal = (UserDetails) authentication.getPrincipal();
        String accessToken = tokenProvider.generateToken(authentication);
        Instant accessTokenExpiry = tokenProvider.getExpirationInstant(accessToken);

        // Generate refresh token
        Instant refreshTokenExpiry = tokenProvider.getRefreshTokenExpiration();
        String refreshToken = refreshTokenService.generateRefreshToken(principal.getUsername(), refreshTokenExpiry);

        UserAccount account = userAccountRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return LoginResponse.of(
                "Bearer",
                accessToken,
                refreshToken,
                accessTokenExpiry,
                refreshTokenExpiry,
                account.username(),
                account.roles().stream().sorted().toList()
        );
    }

    /**
     * Refresh access token using a valid refresh token.
     * 
     * @param request Refresh token request
     * @return New LoginResponse with new access token and optionally new refresh token
     * @throws IllegalArgumentException if refresh token is invalid or expired
     */
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        // Validate refresh token
        String username = refreshTokenService.validateRefreshToken(request.refreshToken());
        if (username == null) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        // Load user details
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        UserAccount account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Generate new access token
        String newAccessToken = tokenProvider.generateToken(userDetails, null);
        Instant accessTokenExpiry = tokenProvider.getExpirationInstant(newAccessToken);

        // Optionally rotate refresh token (for better security)
        // For now, we keep the same refresh token
        Instant refreshTokenExpiry = tokenProvider.getRefreshTokenExpiration();

        return LoginResponse.of(
                "Bearer",
                newAccessToken,
                request.refreshToken(), // Keep same refresh token (or rotate if needed)
                accessTokenExpiry,
                refreshTokenExpiry,
                account.username(),
                account.roles().stream().sorted().toList()
        );
    }

    /**
     * Logout user by blacklisting the access token and revoking refresh token.
     * 
     * @param accessToken The access token to blacklist
     * @param refreshToken The refresh token to revoke (optional)
     */
    public void logout(String accessToken, String refreshToken) {
        // Blacklist the access token
        if (accessToken != null && !accessToken.isEmpty()) {
            Instant expiration = tokenProvider.getExpirationInstant(accessToken);
            blacklistService.blacklistToken(accessToken, expiration);
        }

        // Revoke the refresh token
        if (refreshToken != null && !refreshToken.isEmpty()) {
            refreshTokenService.revokeRefreshToken(refreshToken);
        }
    }

    /**
     * Logout user by username (revokes all refresh tokens and blacklists all active tokens).
     * Useful for security breaches or password changes.
     * 
     * @param username The username whose tokens should be revoked
     */
    public void logoutAll(String username) {
        refreshTokenService.revokeAllTokensForUser(username);
        // Note: We can't blacklist all access tokens for a user without tracking them
        // In production, consider maintaining a token store to track active tokens per user
    }
}





