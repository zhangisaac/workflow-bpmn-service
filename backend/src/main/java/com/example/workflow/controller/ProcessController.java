package com.example.workflow.controller;

import com.example.workflow.dto.*;
import com.example.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/processes")
@Tag(name = "Processes", description = "Manage workflow process definitions and instances")
public class ProcessController {

    private final WorkflowService workflowService;

    public ProcessController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping(path = "/deploy", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deploy a BPMN 2.0 process definition")
    public ResponseEntity<DeploymentResponse> deployProcess(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(workflowService.deployProcess(file));
    }

    @PostMapping("/start")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Start a new process instance")
    public ResponseEntity<ProcessInstanceDto> startProcess(@Valid @RequestBody StartProcessRequest request) {
        return ResponseEntity.ok(workflowService.startProcess(request));
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List active process instances")
    public ResponseEntity<List<ProcessInstanceDto>> getActiveInstances() {
        return ResponseEntity.ok(workflowService.getActiveProcessInstances());
    }

    @PostMapping("/{processInstanceId}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Suspend an active process instance")
    public ResponseEntity<Map<String, String>> suspendProcess(@PathVariable String processInstanceId) {
        workflowService.suspendProcessInstance(processInstanceId);
        return ResponseEntity.ok(Map.of("status", "suspended", "processInstanceId", processInstanceId));
    }

    @PostMapping("/{processInstanceId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate a suspended process instance")
    public ResponseEntity<Map<String, String>> activateProcess(@PathVariable String processInstanceId) {
        workflowService.activateProcessInstance(processInstanceId);
        return ResponseEntity.ok(Map.of("status", "activated", "processInstanceId", processInstanceId));
    }

    @DeleteMapping("/{processInstanceId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a process instance")
    public ResponseEntity<Map<String, String>> deleteProcess(
            @PathVariable String processInstanceId,
            @RequestParam(value = "reason", required = false) String reason) {
        workflowService.deleteProcessInstance(processInstanceId, reason);
        return ResponseEntity.ok(Map.of("status", "deleted", "processInstanceId", processInstanceId));
    }

    @GetMapping("/completed")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List completed process instances")
    public ResponseEntity<List<HistoricProcessInstanceDto>> getCompletedProcesses() {
        return ResponseEntity.ok(workflowService.getCompletedProcesses());
    }

    @GetMapping("/{processInstanceId}/history/tasks")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get historic tasks for a process instance")
    public ResponseEntity<List<HistoricTaskDto>> getHistoricTasks(@PathVariable String processInstanceId) {
        return ResponseEntity.ok(workflowService.getHistoricTasksForProcess(processInstanceId));
    }
}





