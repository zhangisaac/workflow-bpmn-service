package com.example.workflow.service;

import com.example.workflow.dto.ProcessInstanceDto;
import com.example.workflow.dto.TaskDto;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
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

@ExtendWith(MockitoExtension.class)
class WorkflowServiceEdgeCasesTest {

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
        lenient().when(userContextService.currentUsername()).thenReturn("testuser");
        lenient().when(userContextService.currentUserGroups()).thenReturn(Set.of("managers"));
    }

    @Test
    void testGetCandidateTasksForCurrentUser_EmptyGroups() {
        // Given
        when(userContextService.currentUserGroups()).thenReturn(Collections.emptySet());

        // When
        List<TaskDto> result = workflowService.getCandidateTasksForCurrentUser();

        // Then
        assertTrue(result.isEmpty());
        verify(taskService, never()).createTaskQuery();
    }

    @Test
    void testGetTasksAssignedToCurrentUser_EmptyResult() {
        // Given
        TaskQuery taskQuery = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskAssignee("testuser")).thenReturn(taskQuery);
        when(taskQuery.active()).thenReturn(taskQuery);
        when(taskQuery.orderByTaskCreateTime()).thenReturn(taskQuery);
        when(taskQuery.asc()).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(Collections.emptyList());

        // When
        List<TaskDto> result = workflowService.getTasksAssignedToCurrentUser();

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetActiveProcessInstances_EmptyResult() {
        // Given
        org.flowable.engine.runtime.ProcessInstanceQuery query = 
                mock(org.flowable.engine.runtime.ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(query);
        when(query.active()).thenReturn(query);
        when(query.list()).thenReturn(Collections.emptyList());

        // When
        var result = workflowService.getActiveProcessInstances();

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetCompletedProcesses_EmptyResult() {
        // Given
        HistoricProcessInstanceQuery query = mock(HistoricProcessInstanceQuery.class);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(query);
        when(query.finished()).thenReturn(query);
        when(query.orderByProcessInstanceEndTime()).thenReturn(query);
        when(query.desc()).thenReturn(query);
        when(query.list()).thenReturn(Collections.emptyList());

        // When
        var result = workflowService.getCompletedProcesses();

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetHistoricTasksForProcess_EmptyResult() {
        // Given
        HistoricTaskInstanceQuery query = mock(HistoricTaskInstanceQuery.class);
        when(historyService.createHistoricTaskInstanceQuery()).thenReturn(query);
        when(query.processInstanceId("proc-1")).thenReturn(query);
        when(query.finished()).thenReturn(query);
        when(query.orderByHistoricTaskInstanceEndTime()).thenReturn(query);
        when(query.asc()).thenReturn(query);
        when(query.list()).thenReturn(Collections.emptyList());

        // When
        var result = workflowService.getHistoricTasksForProcess("proc-1");

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testStartProcess_WithNullHistoricInstance() {
        // Given
        ProcessInstance instance = mock(ProcessInstance.class);
        when(instance.getId()).thenReturn("proc-1");
        when(instance.getProcessDefinitionId()).thenReturn("def-1");
        when(instance.getProcessDefinitionKey()).thenReturn("key-1");
        when(instance.getBusinessKey()).thenReturn(null);
        when(instance.isSuspended()).thenReturn(false);

        when(runtimeService.startProcessInstanceByKey(anyString(), any(), any())).thenReturn(instance);

        HistoricProcessInstanceQuery query = mock(HistoricProcessInstanceQuery.class);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(query);
        when(query.processInstanceId("proc-1")).thenReturn(query);
        when(query.singleResult()).thenReturn(null);

        // When
        ProcessInstanceDto result = workflowService.startProcess(
                new com.example.workflow.dto.StartProcessRequest("key-1", null, null));

        // Then
        assertNotNull(result);
        assertEquals("proc-1", result.id());
        assertNull(result.startUserId()); // Should be null when historic is null
    }

    @Test
    void testToTaskDto_WithNullDates() {
        // Given
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("task-1");
        when(task.getName()).thenReturn("Test Task");
        when(task.getDescription()).thenReturn(null);
        when(task.getAssignee()).thenReturn("user1");
        when(task.getProcessInstanceId()).thenReturn("proc-1");
        when(task.getCreateTime()).thenReturn(null);
        when(task.getDueDate()).thenReturn(null);

        when(taskService.getIdentityLinksForTask("task-1")).thenReturn(Collections.emptyList());

        TaskQuery taskQuery = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskAssignee("testuser")).thenReturn(taskQuery);
        when(taskQuery.active()).thenReturn(taskQuery);
        when(taskQuery.orderByTaskCreateTime()).thenReturn(taskQuery);
        when(taskQuery.asc()).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(List.of(task));

        // When
        List<TaskDto> result = workflowService.getTasksAssignedToCurrentUser();

        // Then
        assertEquals(1, result.size());
        TaskDto dto = result.get(0);
        assertNull(dto.createdTime());
        assertNull(dto.dueDate());
    }
}

