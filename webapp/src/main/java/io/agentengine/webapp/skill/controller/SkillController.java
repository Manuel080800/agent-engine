package io.agentengine.webapp.skill.controller;

import io.agentengine.core.common.service.KnowledgeService;
import io.agentengine.webapp.common.handler.ResourceRequestHandler;
import io.agentengine.core.skill.record.SkillRequestRecord;
import io.agentengine.webapp.common.controller.CrudController;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/web/v1/skills")
public class SkillController extends CrudController<SkillRequestRecord> {
    public SkillController(KnowledgeService resources, ResourceRequestHandler requestHandler) {
        super("skills", resources, requestHandler);
    }
}
