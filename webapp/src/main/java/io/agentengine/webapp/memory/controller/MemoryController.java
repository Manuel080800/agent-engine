package io.agentengine.webapp.memory.controller;

import io.agentengine.core.common.service.KnowledgeService;
import io.agentengine.webapp.common.handler.ResourceRequestHandler;
import io.agentengine.core.memory.record.MemoryRequestRecord;
import io.agentengine.webapp.common.controller.CrudController;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/web/v1/memories")
public class MemoryController extends CrudController<MemoryRequestRecord> {
    public MemoryController(KnowledgeService resources, ResourceRequestHandler requestHandler) {
        super("memories", resources, requestHandler);
    }
}
