package com.example.workflow.service;

import com.example.workflow.model.UserAccount;
import com.example.workflow.repository.UserAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class UserContextService {

    private final UserAccountRepository userAccountRepository;

    public UserContextService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return authentication.getName();
    }

    public Set<String> currentUserGroups() {
        String username = currentUsername();
        UserAccount account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found: " + username));
        return account.groups();
    }
}





