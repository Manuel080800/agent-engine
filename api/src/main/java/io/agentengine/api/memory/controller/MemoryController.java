package io.agentengine.api.memory.controller;

import io.agentengine.api.common.controller.CrudController;
import io.agentengine.core.common.service.KnowledgeService;
import io.agentengine.api.common.handler.ResourceRequestHandler;
import io.agentengine.core.memory.record.MemoryRequestRecord;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/memories")
public class MemoryController extends CrudController<MemoryRequestRecord> {
    public MemoryController(KnowledgeService resources, ResourceRequestHandler requestHandler) {
        super("memories", resources, requestHandler);
    }
}
