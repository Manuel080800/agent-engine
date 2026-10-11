package io.agentengine.api.task.controller;

import io.agentengine.core.common.record.*;
import io.agentengine.api.common.handler.ResourceRequestHandler;
import io.agentengine.core.task.record.BlockRequestRecord;
import io.agentengine.core.task.service.TaskService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/blocks")
public class BlockCreateController {
    private final TaskService tasks;
    private final ResourceRequestHandler requestHandler;

    public BlockCreateController(TaskService tasks, ResourceRequestHandler requestHandler) {
        this.tasks = tasks;
        this.requestHandler = requestHandler;
    }

    @PostMapping
    public ResourceResponseRecord create(@Valid @RequestBody ResourceWriteRecord<BlockRequestRecord> request) {
        var write = requestHandler.convert(request);
        return tasks.createChild("blocks", write);
    }
}
