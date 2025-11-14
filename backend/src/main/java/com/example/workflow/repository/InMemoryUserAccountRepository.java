package com.example.workflow.repository;

import com.example.workflow.model.UserAccount;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public class InMemoryUserAccountRepository implements UserAccountRepository {

    private final Map<String, UserAccount> users;

    public InMemoryUserAccountRepository() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        this.users = Map.of(
                "admin", new UserAccount(
                        "admin",
                        passwordEncoder.encode("admin"),
                        Set.of("ROLE_ADMIN", "ROLE_USER"),
                        Set.of("managers", "hr_staff")
                ),
                "user", new UserAccount(
                        "user",
                        passwordEncoder.encode("user"),
                        Set.of("ROLE_USER"),
                        Set.of("employees")
                ),
                "manager", new UserAccount(
                        "manager",
                        passwordEncoder.encode("manager"),
                        Set.of("ROLE_USER"),
                        Set.of("managers")
                ),
                "hr", new UserAccount(
                        "hr",
                        passwordEncoder.encode("hr"),
                        Set.of("ROLE_USER"),
                        Set.of("hr_staff")
                )
        );
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    @Override
    public java.util.Collection<UserAccount> findAll() {
        return users.values();
    }
}

