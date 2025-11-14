package com.example.workflow.repository;

import com.example.workflow.model.UserAccount;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public class InMemoryUserAccountRepository implements UserAccountRepository {

    private final Map<String, UserAccount> users;

    public InMemoryUserAccountRepository() {
        this.users = Map.of(
                "admin", new UserAccount(
                        "admin",
                        "$2a$10$7EqJtq98hPqEX7fNZaFWoe3h2U5Y.fF4UpG1HcLUj3q5uQnlQWWe",
                        Set.of("ROLE_ADMIN", "ROLE_USER"),
                        Set.of("managers", "hr_staff")
                ),
                "user", new UserAccount(
                        "user",
                        "$2a$10$Y1t0CuxR7Yk4V/KENjHk7uC18JNpDutLCRa14Q6gttYVOlJawVS9G",
                        Set.of("ROLE_USER"),
                        Set.of("employees")
                ),
                "manager", new UserAccount(
                        "manager",
                        "$2a$10$P7oOIkfbQ/.uwZQzQ0imeOqRCGDpa2BkLomqKgJo0vvArk.W5AOFO",
                        Set.of("ROLE_USER"),
                        Set.of("managers")
                ),
                "hr", new UserAccount(
                        "hr",
                        "$2a$10$gI8DdFZ3VAb9Y/jGj33jjOqvEihQ/HVbUaz2YsmiFjCwXTlzAIXCa",
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

