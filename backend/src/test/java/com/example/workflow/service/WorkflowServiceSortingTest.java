package com.example.workflow.service;

import com.example.workflow.dto.TaskDto;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceSortingTest {

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
        when(userContextService.currentUserGroups()).thenReturn(Set.of("managers"));
    }

    @Test
    void testGetCandidateTasks_SortingWithNullDates() {
        // Given
        Task task1 = mock(Task.class);
        when(task1.getId()).thenReturn("task-1");
        when(task1.getName()).thenReturn("Task 1");
        when(task1.getDescription()).thenReturn(null);
        when(task1.getAssignee()).thenReturn(null);
        when(task1.getProcessInstanceId()).thenReturn("proc-1");
        when(task1.getCreateTime()).thenReturn(null);
        when(task1.getDueDate()).thenReturn(null);

        Task task2 = mock(Task.class);
        when(task2.getId()).thenReturn("task-2");
        when(task2.getName()).thenReturn("Task 2");
        when(task2.getDescription()).thenReturn(null);
        when(task2.getAssignee()).thenReturn(null);
        when(task2.getProcessInstanceId()).thenReturn("proc-2");
        when(task2.getCreateTime()).thenReturn(null);
        when(task2.getDueDate()).thenReturn(null);

        when(taskService.getIdentityLinksForTask("task-1")).thenReturn(Collections.emptyList());
        when(taskService.getIdentityLinksForTask("task-2")).thenReturn(Collections.emptyList());

        TaskQuery taskQuery = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskCandidateGroupIn(anyList())).thenReturn(taskQuery);
        when(taskQuery.active()).thenReturn(taskQuery);
        when(taskQuery.orderByTaskCreateTime()).thenReturn(taskQuery);
        when(taskQuery.asc()).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(List.of(task1, task2));

        // When
        List<TaskDto> result = workflowService.getCandidateTasksForCurrentUser();

        // Then - both have null dates, should be sorted as equal (0)
        assertEquals(2, result.size());
        // Order should be preserved when both are null (comparison returns 0)
    }

    @Test
    void testGetCandidateTasks_SortingWithOneNullDate() {
        // Given
        Task task1 = mock(Task.class);
        when(task1.getId()).thenReturn("task-1");
        when(task1.getName()).thenReturn("Task 1");
        when(task1.getDescription()).thenReturn(null);
        when(task1.getAssignee()).thenReturn(null);
        when(task1.getProcessInstanceId()).thenReturn("proc-1");
        when(task1.getCreateTime()).thenReturn(null); // null date
        when(task1.getDueDate()).thenReturn(null);

        Task task2 = mock(Task.class);
        when(task2.getId()).thenReturn("task-2");
        when(task2.getName()).thenReturn("Task 2");
        when(task2.getDescription()).thenReturn(null);
        when(task2.getAssignee()).thenReturn(null);
        when(task2.getProcessInstanceId()).thenReturn("proc-2");
        when(task2.getCreateTime()).thenReturn(new java.util.Date(Instant.now().toEpochMilli())); // has date
        when(task2.getDueDate()).thenReturn(null);

        when(taskService.getIdentityLinksForTask("task-1")).thenReturn(Collections.emptyList());
        when(taskService.getIdentityLinksForTask("task-2")).thenReturn(Collections.emptyList());

        TaskQuery taskQuery = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskCandidateGroupIn(anyList())).thenReturn(taskQuery);
        when(taskQuery.active()).thenReturn(taskQuery);
        when(taskQuery.orderByTaskCreateTime()).thenReturn(taskQuery);
        when(taskQuery.asc()).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(List.of(task1, task2));

        // When
        List<TaskDto> result = workflowService.getCandidateTasksForCurrentUser();

        // Then - task1 (null date) should come after task2 (has date) - returns 1
        assertEquals(2, result.size());
        // task2 should come first (has date), task1 should come second (null date)
        assertEquals("task-2", result.get(0).id());
        assertEquals("task-1", result.get(1).id());
    }

    @Test
    void testGetCandidateTasks_SortingWithBothDates() {
        // Given
        Instant earlier = Instant.now().minusSeconds(100);
        Instant later = Instant.now();

        Task task1 = mock(Task.class);
        when(task1.getId()).thenReturn("task-1");
        when(task1.getName()).thenReturn("Task 1");
        when(task1.getDescription()).thenReturn(null);
        when(task1.getAssignee()).thenReturn(null);
        when(task1.getProcessInstanceId()).thenReturn("proc-1");
        when(task1.getCreateTime()).thenReturn(new java.util.Date(later.toEpochMilli())); // later date
        when(task1.getDueDate()).thenReturn(null);

        Task task2 = mock(Task.class);
        when(task2.getId()).thenReturn("task-2");
        when(task2.getName()).thenReturn("Task 2");
        when(task2.getDescription()).thenReturn(null);
        when(task2.getAssignee()).thenReturn(null);
        when(task2.getProcessInstanceId()).thenReturn("proc-2");
        when(task2.getCreateTime()).thenReturn(new java.util.Date(earlier.toEpochMilli())); // earlier date
        when(task2.getDueDate()).thenReturn(null);

        when(taskService.getIdentityLinksForTask("task-1")).thenReturn(Collections.emptyList());
        when(taskService.getIdentityLinksForTask("task-2")).thenReturn(Collections.emptyList());

        TaskQuery taskQuery = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskCandidateGroupIn(anyList())).thenReturn(taskQuery);
        when(taskQuery.active()).thenReturn(taskQuery);
        when(taskQuery.orderByTaskCreateTime()).thenReturn(taskQuery);
        when(taskQuery.asc()).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(List.of(task1, task2));

        // When
        List<TaskDto> result = workflowService.getCandidateTasksForCurrentUser();

        // Then - task2 (earlier) should come before task1 (later)
        assertEquals(2, result.size());
        assertEquals("task-2", result.get(0).id());
        assertEquals("task-1", result.get(1).id());
    }
}

