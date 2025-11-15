package com.example.workflow.service;

import com.example.workflow.repository.UserAccountRepository;
import org.flowable.engine.*;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests specifically for task claiming validation logic.
 */
@ExtendWith(MockitoExtension.class)
class TaskClaimingValidationTest {

    private static final String TEST_USERNAME = "testuser";
    private static final String OTHER_USER = "otheruser";
    private static final String TASK_ID = "task-123";
    @Mock
    private ProcessEngine processEngine;
    @Mock
    private RuntimeService runtimeService;
    @Mock
    private TaskService taskService;
    @Mock
    private HistoryService historyService;
    @Mock
    private RepositoryService repositoryService;
    @Mock
    private UserContextService userContextService;
    @Mock
    private UserAccountRepository userAccountRepository;
    @InjectMocks
    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        when(userContextService.currentUsername()).thenReturn(TEST_USERNAME);
    }

    @Test
    void testClaimTask_NullTaskId() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> workflowService.claimTask(null)
        );
        assertEquals("Task ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void testClaimTask_EmptyTaskId() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> workflowService.claimTask("   ")
        );
        assertEquals("Task ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void testClaimTask_TaskNotFound() {
        // Given
        when(taskService.createTaskQuery().taskId(TASK_ID).singleResult()).thenReturn(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> workflowService.claimTask(TASK_ID)
        );
        assertEquals("Task not found: " + TASK_ID, exception.getMessage());
    }

    @Test
    void testClaimTask_AlreadyClaimedByAnotherUser() {
        // Given
        Task mockTask = mock(Task.class);
        when(mockTask.getId()).thenReturn(TASK_ID);
        when(mockTask.getAssignee()).thenReturn(OTHER_USER);
        when(mockTask.getDelegationState()).thenReturn(null);

        when(taskService.createTaskQuery().taskId(TASK_ID).singleResult()).thenReturn(mockTask);

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> workflowService.claimTask(TASK_ID)
        );
        assertTrue(exception.getMessage().contains("already claimed by user"));
        assertTrue(exception.getMessage().contains(OTHER_USER));
        verify(taskService, never()).claim(anyString(), anyString());
    }

    @Test
    void testClaimTask_AlreadyClaimedByCurrentUser() {
        // Given
        Task mockTask = mock(Task.class);
        when(mockTask.getId()).thenReturn(TASK_ID);
        when(mockTask.getAssignee()).thenReturn(TEST_USERNAME);
        when(mockTask.getDelegationState()).thenReturn(null);

        when(taskService.createTaskQuery().taskId(TASK_ID).singleResult()).thenReturn(mockTask);

        // When
        assertDoesNotThrow(() -> workflowService.claimTask(TASK_ID));

        // Then
        verify(taskService, never()).claim(anyString(), anyString());
    }

    @Test
    void testClaimTask_DelegatedTask() {
        // Given
        Task mockTask = mock(Task.class);
        when(mockTask.getId()).thenReturn(TASK_ID);
        when(mockTask.getAssignee()).thenReturn(null);
        when(mockTask.getDelegationState()).thenReturn(org.flowable.task.api.DelegationState.PENDING);

        when(taskService.createTaskQuery().taskId(TASK_ID).singleResult()).thenReturn(mockTask);

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> workflowService.claimTask(TASK_ID)
        );
        assertEquals("Cannot claim a delegated task", exception.getMessage());
        verify(taskService, never()).claim(anyString(), anyString());
    }

    @Test
    void testClaimTask_NoCandidateGroupsOrUsers() {
        // Given
        Task mockTask = mock(Task.class);
        when(mockTask.getId()).thenReturn(TASK_ID);
        when(mockTask.getAssignee()).thenReturn(null);
        when(mockTask.getDelegationState()).thenReturn(null);

        when(taskService.createTaskQuery().taskId(TASK_ID).singleResult()).thenReturn(mockTask);
        when(taskService.getIdentityLinksForTask(TASK_ID)).thenReturn(Collections.emptyList());
        when(userContextService.currentUserGroups()).thenReturn(Set.of("managers"));

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> workflowService.claimTask(TASK_ID)
        );
        assertEquals("Task has no candidate groups or users assigned", exception.getMessage());
        verify(taskService, never()).claim(anyString(), anyString());
    }

    @Test
    void testClaimTask_UserNotCandidate() {
        // Given
        Task mockTask = mock(Task.class);
        when(mockTask.getId()).thenReturn(TASK_ID);
        when(mockTask.getAssignee()).thenReturn(null);
        when(mockTask.getDelegationState()).thenReturn(null);

        IdentityLink identityLink = mock(IdentityLink.class);
        when(identityLink.getGroupId()).thenReturn("hr_staff"); // Different group
        when(identityLink.getUserId()).thenReturn(null);

        when(taskService.createTaskQuery().taskId(TASK_ID).singleResult()).thenReturn(mockTask);
        when(taskService.getIdentityLinksForTask(TASK_ID)).thenReturn(List.of(identityLink));
        when(userContextService.currentUserGroups()).thenReturn(Set.of("managers")); // User is in managers, not hr_staff

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> workflowService.claimTask(TASK_ID)
        );
        assertEquals("You are not a candidate for this task", exception.getMessage());
        verify(taskService, never()).claim(anyString(), anyString());
    }

    @Test
    void testClaimTask_Success_UserInCandidateGroup() {
        // Given
        Task mockTask = mock(Task.class);
        when(mockTask.getId()).thenReturn(TASK_ID);
        when(mockTask.getAssignee()).thenReturn(null);
        when(mockTask.getDelegationState()).thenReturn(null);

        IdentityLink identityLink = mock(IdentityLink.class);
        when(identityLink.getGroupId()).thenReturn("managers");
        when(identityLink.getUserId()).thenReturn(null);

        when(taskService.createTaskQuery().taskId(TASK_ID).singleResult()).thenReturn(mockTask);
        when(taskService.getIdentityLinksForTask(TASK_ID)).thenReturn(List.of(identityLink));
        when(userContextService.currentUserGroups()).thenReturn(Set.of("managers"));

        // When
        assertDoesNotThrow(() -> workflowService.claimTask(TASK_ID));

        // Then
        verify(taskService).claim(TASK_ID, TEST_USERNAME);
    }

    @Test
    void testClaimTask_Success_UserIsCandidateUser() {
        // Given
        Task mockTask = mock(Task.class);
        when(mockTask.getId()).thenReturn(TASK_ID);
        when(mockTask.getAssignee()).thenReturn(null);
        when(mockTask.getDelegationState()).thenReturn(null);

        IdentityLink identityLink = mock(IdentityLink.class);
        when(identityLink.getGroupId()).thenReturn(null);
        when(identityLink.getUserId()).thenReturn(TEST_USERNAME);

        when(taskService.createTaskQuery().taskId(TASK_ID).singleResult()).thenReturn(mockTask);
        when(taskService.getIdentityLinksForTask(TASK_ID)).thenReturn(List.of(identityLink));
        when(userContextService.currentUserGroups()).thenReturn(Collections.emptySet());

        // When
        assertDoesNotThrow(() -> workflowService.claimTask(TASK_ID));

        // Then
        verify(taskService).claim(TASK_ID, TEST_USERNAME);
    }
}

