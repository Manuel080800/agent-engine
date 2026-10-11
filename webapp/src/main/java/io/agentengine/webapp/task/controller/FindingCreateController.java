package io.agentengine.webapp.task.controller;

import io.agentengine.core.common.record.*;
import io.agentengine.webapp.common.handler.ResourceRequestHandler;
import io.agentengine.core.task.record.FindingRequestRecord;
import io.agentengine.core.task.service.TaskService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/web/v1/reviews")
public class FindingCreateController {
    private final TaskService tasks;
    private final ResourceRequestHandler requestHandler;

    public FindingCreateController(TaskService tasks, ResourceRequestHandler requestHandler) {
        this.tasks = tasks;
        this.requestHandler = requestHandler;
    }

    @PostMapping
    public ResourceResponseRecord create(@Valid @RequestBody ResourceWriteRecord<FindingRequestRecord> request) {
        var write = requestHandler.convert(request);
        return tasks.createChild("reviews", write);
    }
}
