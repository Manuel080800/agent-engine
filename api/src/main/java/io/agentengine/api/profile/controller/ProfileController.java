package io.agentengine.api.profile.controller;

import io.agentengine.api.common.controller.CrudController;
import io.agentengine.core.common.service.KnowledgeService;
import io.agentengine.api.common.handler.ResourceRequestHandler;
import io.agentengine.core.profile.record.ProfileRequestRecord;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/profiles")
public class ProfileController extends CrudController<ProfileRequestRecord> {
    public ProfileController(KnowledgeService resources, ResourceRequestHandler requestHandler) {
        super("profiles", resources, requestHandler);
    }
}
