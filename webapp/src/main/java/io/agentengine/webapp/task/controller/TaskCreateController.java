package io.agentengine.webapp.task.controller;

import io.agentengine.core.common.record.*;
import io.agentengine.webapp.common.handler.ResourceRequestHandler;
import io.agentengine.core.task.record.TaskRequestRecord;
import io.agentengine.core.task.service.TaskService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/web/v1/tasks")
public class TaskCreateController {
    private final TaskService tasks;
    private final ResourceRequestHandler requestHandler;

    public TaskCreateController(TaskService tasks, ResourceRequestHandler requestHandler) {
        this.tasks = tasks;
        this.requestHandler = requestHandler;
    }

    @PostMapping
    public ResourceResponseRecord create(@Valid @RequestBody ResourceWriteRecord<TaskRequestRecord> request) {
        var write = requestHandler.convert(request);
        return tasks.create(write);
    }
}
