package io.agentengine.webapp.memory.controller;

import io.agentengine.core.common.record.ResourceResponseRecord;
import io.agentengine.core.memory.record.ProposalDecisionRequestRecord;
import io.agentengine.core.task.service.TaskService;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/web/v1/memory-proposals")
public class ProposalController {
    private final TaskService service;

    public ProposalController(TaskService service) {
        this.service = service;
    }

    @PostMapping("/{id}/decision")
    public ResourceResponseRecord decide(@PathVariable UUID id, @RequestBody ProposalDecisionRequestRecord request) {
        return service.decideProposal(id, request.version(), request.decision(), request.editedText());
    }
}
