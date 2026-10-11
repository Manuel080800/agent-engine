package io.agentengine.webapp.rule.controller;

import io.agentengine.core.common.service.KnowledgeService;
import io.agentengine.webapp.common.handler.ResourceRequestHandler;
import io.agentengine.core.rule.record.RuleRequestRecord;
import io.agentengine.webapp.common.controller.CrudController;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/web/v1/rules")
public class RuleController extends CrudController<RuleRequestRecord> {
    public RuleController(KnowledgeService resources, ResourceRequestHandler requestHandler) {
        super("rules", resources, requestHandler);
    }
}
