package io.agentengine.api.model.controller;

import io.agentengine.api.common.controller.CrudController;
import io.agentengine.core.common.service.KnowledgeService;
import io.agentengine.api.common.handler.ResourceRequestHandler;
import io.agentengine.core.model.record.ModelPolicyRequestRecord;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/model-policies")
public class ModelPolicyController extends CrudController<ModelPolicyRequestRecord> {
    public ModelPolicyController(KnowledgeService resources, ResourceRequestHandler requestHandler) {
        super("model-policies", resources, requestHandler);
    }
}
