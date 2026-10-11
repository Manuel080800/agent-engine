package io.agentengine.api.skill.controller;

import io.agentengine.api.common.controller.CrudController;
import io.agentengine.core.common.service.KnowledgeService;
import io.agentengine.api.common.handler.ResourceRequestHandler;
import io.agentengine.core.skill.record.SkillRequestRecord;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/skills")
public class SkillController extends CrudController<SkillRequestRecord> {
    public SkillController(KnowledgeService resources, ResourceRequestHandler requestHandler) {
        super("skills", resources, requestHandler);
    }
}
