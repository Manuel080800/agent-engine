package io.agentengine.api.project.controller;

import io.agentengine.api.common.controller.CrudController;
import io.agentengine.core.common.service.KnowledgeService;
import io.agentengine.api.common.handler.ResourceRequestHandler;
import io.agentengine.core.project.record.ProjectRequestRecord;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/projects")
public class ProjectController extends CrudController<ProjectRequestRecord> {
    public ProjectController(KnowledgeService resources, ResourceRequestHandler requestHandler) {
        super("projects", resources, requestHandler);
    }
}
