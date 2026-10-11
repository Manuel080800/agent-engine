package io.agentengine.api.execution.controller;

import io.agentengine.core.common.record.ResourceResponseRecord;
import io.agentengine.core.execution.record.TransitionRecord;
import io.agentengine.core.task.service.TaskService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/coordinator")
public class ExecutionController {
    private final TaskService service;

    public ExecutionController(TaskService service) {
        this.service = service;
    }

    @PostMapping("/events")
    public ResourceResponseRecord transition(@Valid @RequestBody TransitionRecord request) {
        return service.transition(request);
    }
}
