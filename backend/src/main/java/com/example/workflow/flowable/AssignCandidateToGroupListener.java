package com.example.workflow.flowable;

import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;

public class AssignCandidateToGroupListener implements TaskListener {

    private Expression group;

    public void setGroup(Expression group) {
        this.group = group;
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        if (group != null) {
            Object value = group.getValue(delegateTask);
            if (value != null) {
                delegateTask.addCandidateGroup(value.toString());
            }
        }
    }
}

