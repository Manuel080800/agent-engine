package io.agentengine.api.rule.controller;

import io.agentengine.api.common.controller.CrudController;
import io.agentengine.core.common.service.KnowledgeService;
import io.agentengine.api.common.handler.ResourceRequestHandler;
import io.agentengine.core.rule.record.RuleRequestRecord;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/rules")
public class RuleController extends CrudController<RuleRequestRecord> {
    public RuleController(KnowledgeService resources, ResourceRequestHandler requestHandler) {
        super("rules", resources, requestHandler);
    }
}
