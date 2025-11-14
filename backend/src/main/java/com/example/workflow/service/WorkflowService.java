package com.example.workflow.service;

import com.example.workflow.dto.DeploymentResponse;
import com.example.workflow.dto.HistoricProcessInstanceDto;
import com.example.workflow.dto.HistoricTaskDto;
import com.example.workflow.dto.ProcessInstanceDto;
import com.example.workflow.dto.StartProcessRequest;
import com.example.workflow.dto.TaskCompletionRequest;
import com.example.workflow.dto.TaskDto;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WorkflowService {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final UserContextService userContextService;

    public WorkflowService(RepositoryService repositoryService,
                           RuntimeService runtimeService,
                           TaskService taskService,
                           HistoryService historyService,
                           UserContextService userContextService) {
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
        this.userContextService = userContextService;
    }

    public DeploymentResponse deployProcess(MultipartFile file) throws IOException {
        String originalFilename = Objects.requireNonNullElse(file.getOriginalFilename(), "uploaded.bpmn20.xml");
        Deployment deployment = repositoryService.createDeployment()
                .addInputStream(originalFilename, file.getInputStream())
                .name(originalFilename)
                .deploy();

        List<String> deployedKeys = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .list()
                .stream()
                .map(def -> def.getKey())
                .toList();

        return new DeploymentResponse(
                deployment.getId(),
                deployment.getName(),
                deployedKeys
        );
    }

    public ProcessInstanceDto startProcess(StartProcessRequest request) {
        String username = userContextService.currentUsername();
        Map<String, Object> variables = new java.util.HashMap<>();
        if (request.variables() != null) {
            variables.putAll(request.variables());
        }
        variables.putIfAbsent("initiator", username);

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                request.processDefinitionKey(),
                request.businessKey(),
                variables
        );

        return toProcessInstanceDto(instance);
    }

    public List<ProcessInstanceDto> getActiveProcessInstances() {
        return runtimeService.createProcessInstanceQuery()
                .active()
                .list()
                .stream()
                .map(this::toProcessInstanceDto)
                .toList();
    }

    public void suspendProcessInstance(String processInstanceId) {
        runtimeService.suspendProcessInstanceById(processInstanceId);
    }

    public void activateProcessInstance(String processInstanceId) {
        runtimeService.activateProcessInstanceById(processInstanceId);
    }

    public void deleteProcessInstance(String processInstanceId, String reason) {
        runtimeService.deleteProcessInstance(processInstanceId, reason == null ? "Deleted by admin" : reason);
    }

    public List<TaskDto> getTasksAssignedToCurrentUser() {
        String username = userContextService.currentUsername();
        return taskService.createTaskQuery()
                .taskAssignee(username)
                .active()
                .orderByTaskCreateTime().asc()
                .list()
                .stream()
                .map(this::toTaskDto)
                .toList();
    }

    public List<TaskDto> getCandidateTasksForCurrentUser() {
        Set<String> groups = userContextService.currentUserGroups();

        if (groups.isEmpty()) {
            return List.of();
        }

        return taskService.createTaskQuery()
                .taskCandidateGroupIn(groups.stream().toList())
                .active()
                .orderByTaskCreateTime().asc()
                .list()
                .stream()
                .map(this::toTaskDto)
                .sorted((a, b) -> {
                    Instant firstCreated = a.createdTime();
                    Instant secondCreated = b.createdTime();
                    if (firstCreated == null && secondCreated == null) {
                        return 0;
                    }
                    if (firstCreated == null) {
                        return 1;
                    }
                    if (secondCreated == null) {
                        return -1;
                    }
                    return firstCreated.compareTo(secondCreated);
                })
                .toList();
    }

    public void claimTask(String taskId) {
        String username = userContextService.currentUsername();
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        if (task.getAssignee() != null && !task.getAssignee().equals(username)) {
            throw new IllegalStateException("Task is already claimed by another user");
        }

        ensureUserCanInteractWithTask(task, username);

        taskService.claim(taskId, username);
    }

    public void completeTask(String taskId, TaskCompletionRequest request) {
        String username = userContextService.currentUsername();
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        if (task.getAssignee() == null) {
            throw new IllegalStateException("Task must be claimed before completion");
        }
        if (!task.getAssignee().equals(username)) {
            throw new IllegalStateException("You are not the assignee of this task");
        }
        ensureUserCanInteractWithTask(task, username);
        Map<String, Object> variables = Optional.ofNullable(request.variables()).orElse(Map.of());
        taskService.complete(taskId, variables);
    }

    public List<HistoricProcessInstanceDto> getCompletedProcesses() {
        return historyService.createHistoricProcessInstanceQuery()
                .finished()
                .orderByProcessInstanceEndTime().desc()
                .list()
                .stream()
                .map(this::toHistoricProcessInstanceDto)
                .toList();
    }

    public List<HistoricTaskDto> getHistoricTasksForProcess(String processInstanceId) {
        return historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .orderByHistoricTaskInstanceEndTime().asc()
                .list()
                .stream()
                .map(this::toHistoricTaskDto)
                .toList();
    }

    private ProcessInstanceDto toProcessInstanceDto(ProcessInstance instance) {
        HistoricProcessInstance historic = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(instance.getId())
                .singleResult();
        String initiator = null;
        if (historic != null) {
            initiator = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(instance.getId())
                    .variableName("initiator")
                    .list()
                    .stream()
                    .findFirst()
                    .map(var -> Objects.toString(var.getValue(), null))
                    .orElse(null);
        }

        return new ProcessInstanceDto(
                instance.getId(),
                instance.getProcessDefinitionId(),
                instance.getProcessDefinitionKey(),
                instance.getBusinessKey(),
                instance.isSuspended(),
                historic != null ? toInstant(historic.getStartTime()) : null,
                initiator
        );
    }

    private HistoricProcessInstanceDto toHistoricProcessInstanceDto(HistoricProcessInstance instance) {
        return new HistoricProcessInstanceDto(
                instance.getId(),
                instance.getProcessDefinitionId(),
                instance.getProcessDefinitionKey(),
                instance.getBusinessKey(),
                toInstant(instance.getStartTime()),
                toInstant(instance.getEndTime()),
                instance.getDurationInMillis(),
                instance.getStartUserId()
        );
    }

    private TaskDto toTaskDto(Task task) {
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());

        List<String> candidateGroups = identityLinks.stream()
                .map(IdentityLink::getGroupId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<String> candidateUsers = identityLinks.stream()
                .map(IdentityLink::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return new TaskDto(
                task.getId(),
                task.getName(),
                task.getDescription(),
                task.getAssignee(),
                task.getProcessInstanceId(),
                toInstant(task.getCreateTime()),
                toInstant(task.getDueDate()),
                candidateGroups,
                candidateUsers
        );
    }

    private HistoricTaskDto toHistoricTaskDto(HistoricTaskInstance instance) {
        Map<String, Object> variables = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instance.getProcessInstanceId())
                .list()
                .stream()
                .collect(Collectors.toMap(
                        var -> var.getVariableName(),
                        var -> var.getValue(),
                        (a, b) -> b
                ));

        return new HistoricTaskDto(
                instance.getId(),
                instance.getName(),
                instance.getAssignee(),
                instance.getProcessInstanceId(),
                toInstant(instance.getStartTime()),
                toInstant(instance.getEndTime()),
                instance.getDurationInMillis(),
                variables
        );
    }

    private Instant toInstant(java.util.Date date) {
        return date == null ? null : Instant.ofEpochMilli(date.getTime());
    }

    private void ensureUserCanInteractWithTask(Task task, String username) {
        Set<String> userGroups = userContextService.currentUserGroups();
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());

        boolean matchesGroup = identityLinks.stream()
                .map(IdentityLink::getGroupId)
                .filter(Objects::nonNull)
                .anyMatch(userGroups::contains);
        boolean matchesUser = identityLinks.stream()
                .map(IdentityLink::getUserId)
                .filter(Objects::nonNull)
                .anyMatch(username::equals);

        if (!matchesGroup && !matchesUser) {
            throw new IllegalStateException("You are not a candidate for this task");
        }
    }
}

