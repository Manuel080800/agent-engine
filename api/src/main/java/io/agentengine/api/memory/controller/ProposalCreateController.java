package io.agentengine.api.memory.controller;

import io.agentengine.core.common.record.*;
import io.agentengine.api.common.handler.ResourceRequestHandler;
import io.agentengine.core.memory.record.ProposalRequestRecord;
import io.agentengine.core.task.service.TaskService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/memory-proposals")
public class ProposalCreateController {
    private final TaskService tasks;
    private final ResourceRequestHandler requestHandler;

    public ProposalCreateController(TaskService tasks, ResourceRequestHandler requestHandler) {
        this.tasks = tasks;
        this.requestHandler = requestHandler;
    }

    @PostMapping
    public ResourceResponseRecord create(@Valid @RequestBody ResourceWriteRecord<ProposalRequestRecord> request) {
        var write = requestHandler.convert(request);
        return tasks.propose(write);
    }
}
