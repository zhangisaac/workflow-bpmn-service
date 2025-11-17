package com.example.workflow.repository;

import com.example.workflow.model.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryUserAccountRepositoryTest {

    private InMemoryUserAccountRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserAccountRepository();
    }

    @Test
    void testFindByUsername_ExistingUser() {
        // When
        Optional<UserAccount> admin = repository.findByUsername("admin");
        Optional<UserAccount> user = repository.findByUsername("user");
        Optional<UserAccount> manager = repository.findByUsername("manager");
        Optional<UserAccount> hr = repository.findByUsername("hr");

        // Then
        assertTrue(admin.isPresent());
        assertEquals("admin", admin.get().username());
        assertTrue(admin.get().roles().contains("ROLE_ADMIN"));
        assertTrue(admin.get().roles().contains("ROLE_USER"));

        assertTrue(user.isPresent());
        assertEquals("user", user.get().username());
        assertTrue(user.get().roles().contains("ROLE_USER"));

        assertTrue(manager.isPresent());
        assertEquals("manager", manager.get().username());

        assertTrue(hr.isPresent());
        assertEquals("hr", hr.get().username());
    }

    @Test
    void testFindByUsername_NonExistentUser() {
        // When
        Optional<UserAccount> result = repository.findByUsername("nonexistent");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testFindByUsername_CaseSensitive() {
        // When
        Optional<UserAccount> upperCase = repository.findByUsername("ADMIN");
        Optional<UserAccount> lowerCase = repository.findByUsername("admin");

        // Then
        assertFalse(upperCase.isPresent()); // Case sensitive
        assertTrue(lowerCase.isPresent());
    }

    @Test
    void testFindAll() {
        // When
        Collection<UserAccount> allUsers = repository.findAll();

        // Then
        assertNotNull(allUsers);
        assertEquals(4, allUsers.size()); // admin, user, manager, hr

        // Verify all expected users are present
        Set<String> usernames = allUsers.stream()
                .map(UserAccount::username)
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(usernames.contains("admin"));
        assertTrue(usernames.contains("user"));
        assertTrue(usernames.contains("manager"));
        assertTrue(usernames.contains("hr"));
    }

    @Test
    void testFindAll_ReturnsImmutableCollection() {
        // Given
        Collection<UserAccount> allUsers = repository.findAll();

        // When & Then - should not be able to modify
        assertThrows(UnsupportedOperationException.class, () -> allUsers.clear());
    }

    @Test
    void testUserAccount_PasswordEncoded() {
        // When
        Optional<UserAccount> admin = repository.findByUsername("admin");

        // Then
        assertTrue(admin.isPresent());
        String password = admin.get().password();
        // BCrypt passwords start with $2a$, $2b$, or $2y$
        assertTrue(password.startsWith("$2") && password.length() > 20);
    }

    @Test
    void testUserAccount_Groups() {
        // When
        Optional<UserAccount> admin = repository.findByUsername("admin");
        Optional<UserAccount> user = repository.findByUsername("user");

        // Then
        assertTrue(admin.isPresent());
        assertTrue(admin.get().groups().contains("managers"));
        assertTrue(admin.get().groups().contains("hr_staff"));

        assertTrue(user.isPresent());
        assertTrue(user.get().groups().contains("employees"));
    }
}

