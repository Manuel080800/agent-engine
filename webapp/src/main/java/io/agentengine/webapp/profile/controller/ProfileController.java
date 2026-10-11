package io.agentengine.webapp.profile.controller;

import io.agentengine.core.common.service.KnowledgeService;
import io.agentengine.webapp.common.handler.ResourceRequestHandler;
import io.agentengine.core.profile.record.ProfileRequestRecord;
import io.agentengine.webapp.common.controller.CrudController;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/web/v1/profiles")
public class ProfileController extends CrudController<ProfileRequestRecord> {
    public ProfileController(KnowledgeService resources, ResourceRequestHandler requestHandler) {
        super("profiles", resources, requestHandler);
    }
}
