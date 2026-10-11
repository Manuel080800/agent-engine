package io.agentengine.webapp.task.controller;

import io.agentengine.core.common.record.*;
import io.agentengine.webapp.common.handler.ResourceRequestHandler;
import io.agentengine.core.task.record.PlanRequestRecord;
import io.agentengine.core.task.service.TaskService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/web/v1/plans")
public class PlanCreateController {
    private final TaskService tasks;
    private final ResourceRequestHandler requestHandler;

    public PlanCreateController(TaskService tasks, ResourceRequestHandler requestHandler) {
        this.tasks = tasks;
        this.requestHandler = requestHandler;
    }

    @PostMapping
    public ResourceResponseRecord create(@Valid @RequestBody ResourceWriteRecord<PlanRequestRecord> request) {
        var write = requestHandler.convert(request);
        return tasks.createChild("plans", write);
    }
}
