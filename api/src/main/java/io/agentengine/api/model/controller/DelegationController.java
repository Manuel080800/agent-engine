package io.agentengine.api.model.controller;

import io.agentengine.core.common.record.ResourceResponseRecord;
import io.agentengine.core.model.record.DelegationRequestRecord;
import io.agentengine.core.model.record.FinishDelegationRequestRecord;
import io.agentengine.core.model.service.DelegationService;

import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/v1/coordinator/delegations")
public class DelegationController {
    private final DelegationService service;

    public DelegationController(DelegationService service) {
        this.service = service;
    }

    @PostMapping
    public Map<String, Object> start(@RequestBody DelegationRequestRecord request) {
        return service.start(request);
    }

    @PostMapping("/{id}/finish")
    public ResourceResponseRecord finish(@PathVariable UUID id, @RequestBody FinishDelegationRequestRecord request) {
        return service.finish(request.taskId(), id, request.revision());
    }
}
