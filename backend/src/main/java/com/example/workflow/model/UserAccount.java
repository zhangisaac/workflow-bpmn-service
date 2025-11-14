package com.example.workflow.model;

import java.util.Set;

public record UserAccount(
        String username,
        String password,
        Set<String> roles,
        Set<String> groups
) {
}





