package com.example.workflow.repository;

import com.example.workflow.model.UserAccount;

import java.util.Optional;

public interface UserAccountRepository {

    Optional<UserAccount> findByUsername(String username);

    java.util.Collection<UserAccount> findAll();
}

