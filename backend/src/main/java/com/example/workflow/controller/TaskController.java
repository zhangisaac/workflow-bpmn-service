package com.example.workflow.controller;

import com.example.workflow.dto.TaskCompletionRequest;
import com.example.workflow.dto.TaskDto;
import com.example.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks", description = "Manage workflow tasks for authenticated users")
public class TaskController {

    private final WorkflowService workflowService;

    public TaskController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Get tasks assigned to the current user")
    public ResponseEntity<List<TaskDto>> getMyTasks() {
        return ResponseEntity.ok(workflowService.getTasksAssignedToCurrentUser());
    }

    @GetMapping("/candidate")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Get tasks available to claim for the current user")
    public ResponseEntity<List<TaskDto>> getCandidateTasks() {
        return ResponseEntity.ok(workflowService.getCandidateTasksForCurrentUser());
    }

    @PostMapping("/{taskId}/claim")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Claim a candidate task")
    public ResponseEntity<Map<String, String>> claimTask(@PathVariable String taskId) {
        workflowService.claimTask(taskId);
        return ResponseEntity.ok(Map.of("status", "claimed", "taskId", taskId));
    }

    @PostMapping("/{taskId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Complete a task that is assigned to the current user")
    public ResponseEntity<Map<String, String>> completeTask(
            @PathVariable String taskId,
            @Valid @RequestBody(required = false) TaskCompletionRequest request) {
        TaskCompletionRequest completionRequest = request == null
                ? new TaskCompletionRequest(Map.of())
                : request;
        workflowService.completeTask(taskId, completionRequest);
        return ResponseEntity.ok(Map.of("status", "completed", "taskId", taskId));
    }
}





