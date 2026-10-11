package io.agentengine.api.common.controller;

import io.agentengine.core.common.record.ResourceResponseRecord;
import io.agentengine.core.common.service.ResourceService;

import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/v1")
public class ResourceController {
    private final ResourceService resources;

    public ResourceController(ResourceService resources) {
        this.resources = resources;
    }

    @GetMapping("/{kind:tasks|plans|blocks|reviews|memory-proposals|checkpoints|local-clients}")
    List<ResourceResponseRecord> list(@PathVariable String kind, @RequestParam(required = false) UUID projectId, @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "50") int limit, @RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "false") boolean tombstones) {
        return resources.list(kind, projectId, q, limit, offset, tombstones);
    }

    @GetMapping("/{kind:tasks|plans|blocks|reviews|memory-proposals|checkpoints|local-clients}/{id}")
    ResourceResponseRecord get(@PathVariable String kind, @PathVariable UUID id) {
        return resources.get(kind, id, false);
    }
}
