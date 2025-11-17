package com.example.workflow.security;

import com.example.workflow.model.UserAccount;
import com.example.workflow.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserAccountRepository repository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private static final String TEST_USERNAME = "testuser";
    private UserAccount testUserAccount;

    @BeforeEach
    void setUp() {
        testUserAccount = new UserAccount(
                TEST_USERNAME,
                "encoded-password",
                Set.of("ROLE_USER", "ROLE_ADMIN"),
                Set.of("employees")
        );
    }

    @Test
    void testLoadUserByUsername_Success() {
        // Given
        when(repository.findByUsername(TEST_USERNAME))
                .thenReturn(Optional.of(testUserAccount));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername(TEST_USERNAME);

        // Then
        assertNotNull(userDetails);
        assertEquals(TEST_USERNAME, userDetails.getUsername());
        assertEquals("encoded-password", userDetails.getPassword());
        assertEquals(2, userDetails.getAuthorities().size());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        verify(repository).findByUsername(TEST_USERNAME);
    }

    @Test
    void testLoadUserByUsername_UserNotFound() {
        // Given
        when(repository.findByUsername(TEST_USERNAME))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(TEST_USERNAME));
        verify(repository).findByUsername(TEST_USERNAME);
    }

    @Test
    void testLoadUserByUsername_WithSingleRole() {
        // Given
        UserAccount singleRoleAccount = new UserAccount(
                "singleuser",
                "password",
                Set.of("ROLE_USER"),
                Set.of()
        );
        when(repository.findByUsername("singleuser"))
                .thenReturn(Optional.of(singleRoleAccount));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("singleuser");

        // Then
        assertNotNull(userDetails);
        assertEquals(1, userDetails.getAuthorities().size());
        assertEquals("ROLE_USER", userDetails.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void testLoadUserByUsername_WithNoRoles() {
        // Given
        UserAccount noRoleAccount = new UserAccount(
                "noroleuser",
                "password",
                Set.of(),
                Set.of()
        );
        when(repository.findByUsername("noroleuser"))
                .thenReturn(Optional.of(noRoleAccount));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("noroleuser");

        // Then
        assertNotNull(userDetails);
        assertEquals(0, userDetails.getAuthorities().size());
    }
}

