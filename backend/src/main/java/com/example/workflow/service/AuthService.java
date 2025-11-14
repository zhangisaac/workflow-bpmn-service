package com.example.workflow.service;

import com.example.workflow.dto.LoginRequest;
import com.example.workflow.dto.LoginResponse;
import com.example.workflow.model.UserAccount;
import com.example.workflow.repository.UserAccountRepository;
import com.example.workflow.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserAccountRepository userAccountRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       UserAccountRepository userAccountRepository) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userAccountRepository = userAccountRepository;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserDetails principal = (UserDetails) authentication.getPrincipal();
        String token = tokenProvider.generateToken(authentication);

        UserAccount account = userAccountRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return LoginResponse.of(
                "Bearer",
                token,
                tokenProvider.getExpirationInstant(token),
                account.username(),
                account.roles().stream().sorted().toList()
        );
    }
}





