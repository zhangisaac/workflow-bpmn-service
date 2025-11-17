package com.example.workflow.flowable;

import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.task.service.delegate.DelegateTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignCandidateToGroupListenerTest {

    @Mock
    private DelegateTask delegateTask;

    @Mock
    private Expression groupExpression;

    private AssignCandidateToGroupListener listener;

    @BeforeEach
    void setUp() {
        listener = new AssignCandidateToGroupListener();
    }

    @Test
    void testNotify_WithGroupExpression() {
        // Given
        listener.setGroup(groupExpression);
        when(groupExpression.getValue(delegateTask)).thenReturn("managers");

        // When
        listener.notify(delegateTask);

        // Then
        verify(delegateTask).addCandidateGroup("managers");
    }

    @Test
    void testNotify_WithNullGroupExpression() {
        // Given
        listener.setGroup(null);

        // When
        listener.notify(delegateTask);

        // Then
        verify(delegateTask, never()).addCandidateGroup(anyString());
    }

    @Test
    void testNotify_WithNullGroupValue() {
        // Given
        listener.setGroup(groupExpression);
        when(groupExpression.getValue(delegateTask)).thenReturn(null);

        // When
        listener.notify(delegateTask);

        // Then
        verify(delegateTask, never()).addCandidateGroup(anyString());
    }

    @Test
    void testNotify_WithEmptyGroupValue() {
        // Given
        listener.setGroup(groupExpression);
        when(groupExpression.getValue(delegateTask)).thenReturn("");

        // When
        listener.notify(delegateTask);

        // Then
        verify(delegateTask).addCandidateGroup("");
    }
}

