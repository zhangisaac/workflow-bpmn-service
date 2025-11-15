package com.example.workflow.config;

import com.example.workflow.model.UserAccount;
import com.example.workflow.repository.UserAccountRepository;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ProcessEngine;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FlowableIdentityInitializer.
 */
@ExtendWith(MockitoExtension.class)
class FlowableIdentityInitializerTest {

    @Mock
    private ProcessEngine processEngine;

    @Mock
    private IdentityService identityService;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private FlowableIdentityInitializer initializer;

    @BeforeEach
    void setUp() {
        when(processEngine.getIdentityService()).thenReturn(identityService);
    }

    @Test
    void testInitializeFlowableIdentities_CreatesUsersAndGroups() {
        // Given
        UserAccount admin = new UserAccount(
                "admin",
                "hashed-password",
                Set.of("ROLE_ADMIN", "ROLE_USER"),
                Set.of("managers", "hr_staff")
        );
        UserAccount user = new UserAccount(
                "user",
                "hashed-password",
                Set.of("ROLE_USER"),
                Set.of("employees")
        );

        when(userAccountRepository.findAll()).thenReturn(List.of(admin, user));

        // Mock group queries - groups don't exist initially
        when(identityService.createGroupQuery().groupId(anyString()).singleResult())
                .thenReturn(null);

        // Mock user queries - users don't exist initially
        when(identityService.createUserQuery().userId(anyString()).singleResult())
                .thenReturn(null);

        // Mock group membership queries - no memberships exist initially
        when(identityService.createGroupQuery()
                .groupMember(anyString())
                .groupId(anyString())
                .count()).thenReturn(0L);

        // Mock new group/user creation
        Group mockGroup = mock(Group.class);
        when(identityService.newGroup(anyString())).thenReturn(mockGroup);

        User mockUser = mock(User.class);
        when(identityService.newUser(anyString())).thenReturn(mockUser);

        // When
        initializer.initializeFlowableIdentities();

        // Then
        // Verify all groups are created (managers, hr_staff, employees)
        verify(identityService, times(3)).newGroup(anyString());
        verify(identityService, times(3)).saveGroup(any(Group.class));

        // Verify all users are created (admin, user)
        verify(identityService, times(2)).newUser(anyString());
        verify(identityService, times(2)).saveUser(any(User.class));

        // Verify memberships are created
        verify(identityService).createMembership("admin", "managers");
        verify(identityService).createMembership("admin", "hr_staff");
        verify(identityService).createMembership("user", "employees");
    }

    @Test
    void testInitializeFlowableIdentities_SkipsExistingUsers() {
        // Given
        UserAccount admin = new UserAccount(
                "admin",
                "hashed-password",
                Set.of("ROLE_ADMIN"),
                Set.of("managers")
        );

        when(userAccountRepository.findAll()).thenReturn(List.of(admin));

        // Mock existing user
        User existingUser = mock(User.class);
        when(identityService.createUserQuery().userId("admin").singleResult())
                .thenReturn(existingUser);

        // Mock group doesn't exist
        when(identityService.createGroupQuery().groupId("managers").singleResult())
                .thenReturn(null);

        Group mockGroup = mock(Group.class);
        when(identityService.newGroup("managers")).thenReturn(mockGroup);

        when(identityService.createGroupQuery()
                .groupMember("admin")
                .groupId("managers")
                .count()).thenReturn(0L);

        // When
        initializer.initializeFlowableIdentities();

        // Then
        // User should not be created again
        verify(identityService, never()).newUser("admin");
        verify(identityService, never()).saveUser(existingUser);

        // Group should still be created
        verify(identityService).newGroup("managers");
        verify(identityService).saveGroup(any(Group.class));
    }

    @Test
    void testInitializeFlowableIdentities_SkipsExistingMemberships() {
        // Given
        UserAccount admin = new UserAccount(
                "admin",
                "hashed-password",
                Set.of("ROLE_ADMIN"),
                Set.of("managers")
        );

        when(userAccountRepository.findAll()).thenReturn(List.of(admin));

        // Mock existing user and group
        when(identityService.createUserQuery().userId("admin").singleResult())
                .thenReturn(null);
        when(identityService.createGroupQuery().groupId("managers").singleResult())
                .thenReturn(null);

        User mockUser = mock(User.class);
        when(identityService.newUser("admin")).thenReturn(mockUser);

        Group mockGroup = mock(Group.class);
        when(identityService.newGroup("managers")).thenReturn(mockGroup);

        // Mock existing membership
        when(identityService.createGroupQuery()
                .groupMember("admin")
                .groupId("managers")
                .count()).thenReturn(1L);

        // When
        initializer.initializeFlowableIdentities();

        // Then
        // Membership should not be created again
        verify(identityService, never()).createMembership("admin", "managers");
    }
}

