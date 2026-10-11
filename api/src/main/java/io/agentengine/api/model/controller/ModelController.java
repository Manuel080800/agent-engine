package io.agentengine.api.model.controller;

import io.agentengine.api.common.controller.CrudController;
import io.agentengine.core.common.service.KnowledgeService;
import io.agentengine.api.common.handler.ResourceRequestHandler;
import io.agentengine.core.model.record.ModelRequestRecord;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/models")
public class ModelController extends CrudController<ModelRequestRecord> {
    public ModelController(KnowledgeService resources, ResourceRequestHandler requestHandler) {
        super("models", resources, requestHandler);
    }
}
