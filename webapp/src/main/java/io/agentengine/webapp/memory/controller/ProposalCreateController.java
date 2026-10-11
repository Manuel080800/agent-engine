package io.agentengine.webapp.memory.controller;

import io.agentengine.core.common.record.*;
import io.agentengine.webapp.common.handler.ResourceRequestHandler;
import io.agentengine.core.memory.record.ProposalRequestRecord;
import io.agentengine.core.task.service.TaskService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/web/v1/memory-proposals")
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
