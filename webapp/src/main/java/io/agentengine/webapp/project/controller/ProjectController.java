package io.agentengine.webapp.project.controller;

import io.agentengine.core.common.service.KnowledgeService;
import io.agentengine.webapp.common.handler.ResourceRequestHandler;
import io.agentengine.core.project.record.ProjectRequestRecord;
import io.agentengine.webapp.common.controller.CrudController;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/web/v1/projects")
public class ProjectController extends CrudController<ProjectRequestRecord> {
    public ProjectController(KnowledgeService resources, ResourceRequestHandler requestHandler) {
        super("projects", resources, requestHandler);
    }
}
