package io.agentengine.webapp.model.controller;

import io.agentengine.core.common.service.KnowledgeService;
import io.agentengine.webapp.common.handler.ResourceRequestHandler;
import io.agentengine.core.model.record.ModelRequestRecord;
import io.agentengine.webapp.common.controller.CrudController;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/web/v1/models")
public class ModelController extends CrudController<ModelRequestRecord> {
    public ModelController(KnowledgeService resources, ResourceRequestHandler requestHandler) {
        super("models", resources, requestHandler);
    }
}
