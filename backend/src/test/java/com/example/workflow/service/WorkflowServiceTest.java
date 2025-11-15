package com.example.workflow.service;

import com.example.workflow.dto.ProcessInstanceDto;
import com.example.workflow.dto.StartProcessRequest;
import com.example.workflow.dto.TaskCompletionRequest;
import com.example.workflow.dto.TaskDto;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PROCESS_KEY = "testProcess";
    @Mock
    private RepositoryService repositoryService;
    @Mock
    private RuntimeService runtimeService;
    @Mock
    private TaskService taskService;
    @Mock
    private HistoryService historyService;
    @Mock
    private UserContextService userContextService;
    @InjectMocks
    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        when(userContextService.currentUsername()).thenReturn(TEST_USERNAME);
    }

    @Test
    void testStartProcess_Success() {
        // Given
        StartProcessRequest request = new StartProcessRequest(
                TEST_PROCESS_KEY,
                "business-key-123",
                Map.of("key", "value")
        );

        ProcessInstance mockInstance = mock(ProcessInstance.class);
        when(mockInstance.getId()).thenReturn("proc-inst-1");
        when(mockInstance.getProcessDefinitionId()).thenReturn("proc-def-1");
        when(mockInstance.getProcessDefinitionKey()).thenReturn(TEST_PROCESS_KEY);
        when(mockInstance.getBusinessKey()).thenReturn("business-key-123");
        when(mockInstance.isSuspended()).thenReturn(false);

        when(runtimeService.startProcessInstanceByKey(
                eq(TEST_PROCESS_KEY),
                eq("business-key-123"),
                any(Map.class)
        )).thenReturn(mockInstance);

        when(historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(anyString())
                .singleResult()).thenReturn(null);

        // When
        ProcessInstanceDto result = workflowService.startProcess(request);

        // Then
        assertNotNull(result);
        assertEquals("proc-inst-1", result.id());
        assertEquals(TEST_PROCESS_KEY, result.processDefinitionKey());
        verify(runtimeService).startProcessInstanceByKey(
                eq(TEST_PROCESS_KEY),
                eq("business-key-123"),
                argThat(vars -> vars.containsKey("initiator") &&
                        vars.get("initiator").equals(TEST_USERNAME) &&
                        vars.get("key").equals("value"))
        );
    }

    @Test
    void testStartProcess_AddsInitiatorVariable() {
        // Given
        StartProcessRequest request = new StartProcessRequest(
                TEST_PROCESS_KEY,
                null,
                null
        );

        ProcessInstance mockInstance = mock(ProcessInstance.class);
        when(mockInstance.getId()).thenReturn("proc-inst-1");
        when(mockInstance.getProcessDefinitionId()).thenReturn("proc-def-1");
        when(mockInstance.getProcessDefinitionKey()).thenReturn(TEST_PROCESS_KEY);
        when(mockInstance.getBusinessKey()).thenReturn(null);
        when(mockInstance.isSuspended()).thenReturn(false);

        when(runtimeService.startProcessInstanceByKey(anyString(), any(), any(Map.class)))
                .thenReturn(mockInstance);
        when(historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(anyString())
                .singleResult()).thenReturn(null);

        // When
        workflowService.startProcess(request);

        // Then
        verify(runtimeService).startProcessInstanceByKey(
                eq(TEST_PROCESS_KEY),
                isNull(),
                argThat(vars -> vars.containsKey("initiator") &&
                        vars.get("initiator").equals(TEST_USERNAME))
        );
    }

    @Test
    void testGetTasksAssignedToCurrentUser() {
        // Given
        Task mockTask = mock(Task.class);
        when(mockTask.getId()).thenReturn("task-1");
        when(mockTask.getName()).thenReturn("Test Task");
        when(mockTask.getProcessInstanceId()).thenReturn("proc-inst-1");

        when(taskService.createTaskQuery()
                .taskAssignee(TEST_USERNAME)
                .active()
                .orderByTaskCreateTime()
                .asc()
                .list()).thenReturn(List.of(mockTask));

        when(taskService.getIdentityLinksForTask("task-1"))
                .thenReturn(Collections.emptyList());

        // When
        List<TaskDto> result = workflowService.getTasksAssignedToCurrentUser();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("task-1", result.get(0).id());
        verify(taskService).createTaskQuery().taskAssignee(TEST_USERNAME);
    }

    @Test
    void testCompleteTask_Success() {
        // Given
        String taskId = "task-1";
        TaskCompletionRequest request = new TaskCompletionRequest(Map.of("approved", true));

        Task mockTask = mock(Task.class);
        when(mockTask.getId()).thenReturn(taskId);
        when(mockTask.getAssignee()).thenReturn(TEST_USERNAME);
        when(mockTask.getProcessInstanceId()).thenReturn("proc-inst-1");

        when(taskService.createTaskQuery().taskId(taskId).singleResult()).thenReturn(mockTask);
        when(taskService.getIdentityLinksForTask(taskId)).thenReturn(Collections.emptyList());
        when(userContextService.currentUserGroups()).thenReturn(Collections.emptySet());

        // When
        assertDoesNotThrow(() -> workflowService.completeTask(taskId, request));

        // Then - verify complete was called with taskId and a Map containing the variables
        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(taskService).complete(eq(taskId), variablesCaptor.capture());

        Map<String, Object> capturedVariables = variablesCaptor.getValue();
        assertTrue(capturedVariables.containsKey("approved"));
        assertEquals(true, capturedVariables.get("approved"));
    }

    @Test
    void testCompleteTask_TaskNotFound() {
        // Given
        String taskId = "non-existent-task";
        when(taskService.createTaskQuery().taskId(taskId).singleResult()).thenReturn(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> workflowService.completeTask(taskId, new TaskCompletionRequest(Map.of()))
        );
        assertEquals("Task not found: " + taskId, exception.getMessage());
    }

    @Test
    void testCompleteTask_NotAssigned() {
        // Given
        String taskId = "task-1";
        Task mockTask = mock(Task.class);
        when(mockTask.getId()).thenReturn(taskId);
        when(mockTask.getAssignee()).thenReturn(null);

        when(taskService.createTaskQuery().taskId(taskId).singleResult()).thenReturn(mockTask);

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> workflowService.completeTask(taskId, new TaskCompletionRequest(Map.of()))
        );
        assertEquals("Task must be claimed before completion", exception.getMessage());
    }

    @Test
    void testCompleteTask_WrongAssignee() {
        // Given
        String taskId = "task-1";
        Task mockTask = mock(Task.class);
        when(mockTask.getId()).thenReturn(taskId);
        when(mockTask.getAssignee()).thenReturn("other-user");

        when(taskService.createTaskQuery().taskId(taskId).singleResult()).thenReturn(mockTask);

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> workflowService.completeTask(taskId, new TaskCompletionRequest(Map.of()))
        );
        assertEquals("You are not the assignee of this task", exception.getMessage());
    }
}

