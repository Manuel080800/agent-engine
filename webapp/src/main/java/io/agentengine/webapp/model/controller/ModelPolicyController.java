package io.agentengine.webapp.model.controller;

import io.agentengine.core.common.service.KnowledgeService;
import io.agentengine.webapp.common.handler.ResourceRequestHandler;
import io.agentengine.core.model.record.ModelPolicyRequestRecord;
import io.agentengine.webapp.common.controller.CrudController;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/web/v1/model-policies")
public class ModelPolicyController extends CrudController<ModelPolicyRequestRecord> {
    public ModelPolicyController(KnowledgeService resources, ResourceRequestHandler requestHandler) {
        super("model-policies", resources, requestHandler);
    }
}
