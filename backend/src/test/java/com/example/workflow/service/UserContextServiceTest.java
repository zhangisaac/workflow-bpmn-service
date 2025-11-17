package com.example.workflow.service;

import com.example.workflow.model.UserAccount;
import com.example.workflow.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserContextServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserContextService userContextService;

    private static final String TEST_USERNAME = "testuser";
    private UserAccount testUserAccount;

    @BeforeEach
    void setUp() {
        testUserAccount = new UserAccount(
                TEST_USERNAME,
                "encoded-password",
                Set.of("ROLE_USER"),
                Set.of("employees", "developers")
        );
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testCurrentUsername() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(TEST_USERNAME);

        // When
        String username = userContextService.currentUsername();

        // Then
        assertEquals(TEST_USERNAME, username);
        verify(securityContext).getAuthentication();
    }

    @Test
    void testCurrentUsername_NoAuthentication() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When & Then
        assertThrows(IllegalStateException.class, () -> userContextService.currentUsername());
    }

    @Test
    void testCurrentUserGroups() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(TEST_USERNAME);
        when(userAccountRepository.findByUsername(TEST_USERNAME))
                .thenReturn(Optional.of(testUserAccount));

        // When
        Set<String> groups = userContextService.currentUserGroups();

        // Then
        assertNotNull(groups);
        assertEquals(2, groups.size());
        assertTrue(groups.contains("employees"));
        assertTrue(groups.contains("developers"));
        verify(userAccountRepository).findByUsername(TEST_USERNAME);
    }

    @Test
    void testCurrentUserGroups_UserNotFound() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(TEST_USERNAME);
        when(userAccountRepository.findByUsername(TEST_USERNAME))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalStateException.class, () -> userContextService.currentUserGroups());
    }

    @Test
    void testCurrentUserGroups_NoAuthentication() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When & Then
        assertThrows(IllegalStateException.class, () -> userContextService.currentUserGroups());
    }
}

