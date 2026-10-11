package io.agentengine.api.task.controller;

import io.agentengine.core.model.record.ConsentRequestRecord;
import io.agentengine.core.task.record.ApprovalRequestRecord;
import io.agentengine.core.task.service.TaskService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/v1/tasks")
public class TaskController {
    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping("/{id}/context")
    public Map<String, Object> context(@PathVariable UUID id, @RequestParam(required = false) UUID projectId) {
        return service.context(projectId, id);
    }

    @PostMapping("/{id}/approval")
    public Map<String, UUID> approve(@PathVariable UUID id, @RequestBody ApprovalRequestRecord request) {
        return Map.of("id", service.approve(id, request.version()));
    }

    @PostMapping("/{id}/consent")
    public Map<String, UUID> consent(@PathVariable UUID id, @Valid @RequestBody ConsentRequestRecord request) {
        return Map.of("id", service.consent(id, request.blockId(), request.delegationId(), request.modelId(), request.effort()));
    }
}
