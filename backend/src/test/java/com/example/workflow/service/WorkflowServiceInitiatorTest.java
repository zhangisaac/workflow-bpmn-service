package com.example.workflow.service;

import com.example.workflow.dto.ProcessInstanceDto;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceInitiatorTest {

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private TaskService taskService;

    @Mock
    private org.flowable.engine.HistoryService historyService;

    @Mock
    private UserContextService userContextService;

    @InjectMocks
    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        when(userContextService.currentUsername()).thenReturn("testuser");
    }

    @Test
    void testStartProcess_InitiatorFromStartUserId() {
        // Given
        ProcessInstance instance = mock(ProcessInstance.class);
        when(instance.getId()).thenReturn("proc-1");
        when(instance.getProcessDefinitionId()).thenReturn("def-1");
        when(instance.getProcessDefinitionKey()).thenReturn("key-1");
        when(instance.getBusinessKey()).thenReturn(null);
        when(instance.isSuspended()).thenReturn(false);

        when(runtimeService.startProcessInstanceByKey(anyString(), any(), any())).thenReturn(instance);

        HistoricProcessInstance historic = mock(HistoricProcessInstance.class);
        when(historic.getStartUserId()).thenReturn("startuser");
        when(historic.getStartTime()).thenReturn(new java.util.Date());

        HistoricProcessInstanceQuery query = mock(HistoricProcessInstanceQuery.class);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(query);
        when(query.processInstanceId("proc-1")).thenReturn(query);
        when(query.singleResult()).thenReturn(historic);

        // When
        ProcessInstanceDto result = workflowService.startProcess(
                new com.example.workflow.dto.StartProcessRequest("key-1", null, null));

        // Then
        assertNotNull(result);
        assertEquals("proc-1", result.id());
        assertEquals("startuser", result.startUserId()); // Should use startUserId
        verify(historyService, never()).createHistoricVariableInstanceQuery();
    }

    @Test
    void testStartProcess_InitiatorFromVariable_WhenStartUserIdIsNull() {
        // Given
        ProcessInstance instance = mock(ProcessInstance.class);
        when(instance.getId()).thenReturn("proc-1");
        when(instance.getProcessDefinitionId()).thenReturn("def-1");
        when(instance.getProcessDefinitionKey()).thenReturn("key-1");
        when(instance.getBusinessKey()).thenReturn(null);
        when(instance.isSuspended()).thenReturn(false);

        when(runtimeService.startProcessInstanceByKey(anyString(), any(), any())).thenReturn(instance);

        HistoricProcessInstance historic = mock(HistoricProcessInstance.class);
        when(historic.getStartUserId()).thenReturn(null); // startUserId is null
        when(historic.getStartTime()).thenReturn(new java.util.Date());

        HistoricProcessInstanceQuery query = mock(HistoricProcessInstanceQuery.class);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(query);
        when(query.processInstanceId("proc-1")).thenReturn(query);
        when(query.singleResult()).thenReturn(historic);

        // Create a mock variable instance - use Answer to return an object with getValue()
        org.flowable.variable.api.history.HistoricVariableInstanceQuery varQuery = 
                mock(org.flowable.variable.api.history.HistoricVariableInstanceQuery.class);
        when(historyService.createHistoricVariableInstanceQuery()).thenReturn(varQuery);
        when(varQuery.processInstanceId("proc-1")).thenReturn(varQuery);
        when(varQuery.variableName("initiator")).thenReturn(varQuery);
        
        // Create a mock HistoricVariableInstance
        org.flowable.variable.api.history.HistoricVariableInstance mockVar = 
                mock(org.flowable.variable.api.history.HistoricVariableInstance.class);
        when(mockVar.getValue()).thenReturn("varuser");
        when(varQuery.list()).thenReturn(List.of(mockVar));

        // When
        ProcessInstanceDto result = workflowService.startProcess(
                new com.example.workflow.dto.StartProcessRequest("key-1", null, null));

        // Then
        assertNotNull(result);
        assertEquals("proc-1", result.id());
        assertEquals("varuser", result.startUserId()); // Should fall back to variable
    }

    @Test
    void testStartProcess_InitiatorFromVariable_WhenStartUserIdIsEmpty() {
        // Given
        ProcessInstance instance = mock(ProcessInstance.class);
        when(instance.getId()).thenReturn("proc-1");
        when(instance.getProcessDefinitionId()).thenReturn("def-1");
        when(instance.getProcessDefinitionKey()).thenReturn("key-1");
        when(instance.getBusinessKey()).thenReturn(null);
        when(instance.isSuspended()).thenReturn(false);

        when(runtimeService.startProcessInstanceByKey(anyString(), any(), any())).thenReturn(instance);

        HistoricProcessInstance historic = mock(HistoricProcessInstance.class);
        when(historic.getStartUserId()).thenReturn(""); // startUserId is empty
        when(historic.getStartTime()).thenReturn(new java.util.Date());

        HistoricProcessInstanceQuery query = mock(HistoricProcessInstanceQuery.class);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(query);
        when(query.processInstanceId("proc-1")).thenReturn(query);
        when(query.singleResult()).thenReturn(historic);

        org.flowable.variable.api.history.HistoricVariableInstanceQuery varQuery = 
                mock(org.flowable.variable.api.history.HistoricVariableInstanceQuery.class);
        when(historyService.createHistoricVariableInstanceQuery()).thenReturn(varQuery);
        when(varQuery.processInstanceId("proc-1")).thenReturn(varQuery);
        when(varQuery.variableName("initiator")).thenReturn(varQuery);
        org.flowable.variable.api.history.HistoricVariableInstance mockVar2 = 
                mock(org.flowable.variable.api.history.HistoricVariableInstance.class);
        when(mockVar2.getValue()).thenReturn("varuser2");
        when(varQuery.list()).thenReturn(List.of(mockVar2));

        // When
        ProcessInstanceDto result = workflowService.startProcess(
                new com.example.workflow.dto.StartProcessRequest("key-1", null, null));

        // Then
        assertNotNull(result);
        assertEquals("proc-1", result.id());
        assertEquals("varuser2", result.startUserId()); // Should fall back to variable
    }

    @Test
    void testStartProcess_InitiatorFromVariable_WhenVariableNotFound() {
        // Given
        ProcessInstance instance = mock(ProcessInstance.class);
        when(instance.getId()).thenReturn("proc-1");
        when(instance.getProcessDefinitionId()).thenReturn("def-1");
        when(instance.getProcessDefinitionKey()).thenReturn("key-1");
        when(instance.getBusinessKey()).thenReturn(null);
        when(instance.isSuspended()).thenReturn(false);

        when(runtimeService.startProcessInstanceByKey(anyString(), any(), any())).thenReturn(instance);

        HistoricProcessInstance historic = mock(HistoricProcessInstance.class);
        when(historic.getStartUserId()).thenReturn(null);
        when(historic.getStartTime()).thenReturn(new java.util.Date());

        HistoricProcessInstanceQuery query = mock(HistoricProcessInstanceQuery.class);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(query);
        when(query.processInstanceId("proc-1")).thenReturn(query);
        when(query.singleResult()).thenReturn(historic);

        org.flowable.variable.api.history.HistoricVariableInstanceQuery varQuery = 
                mock(org.flowable.variable.api.history.HistoricVariableInstanceQuery.class);
        when(historyService.createHistoricVariableInstanceQuery()).thenReturn(varQuery);
        when(varQuery.processInstanceId("proc-1")).thenReturn(varQuery);
        when(varQuery.variableName("initiator")).thenReturn(varQuery);
        when(varQuery.list()).thenReturn(Collections.emptyList()); // Variable not found

        // When
        ProcessInstanceDto result = workflowService.startProcess(
                new com.example.workflow.dto.StartProcessRequest("key-1", null, null));

        // Then
        assertNotNull(result);
        assertEquals("proc-1", result.id());
        assertNull(result.startUserId()); // Should be null when variable not found
    }
}

