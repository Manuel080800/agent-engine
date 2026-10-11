package io.agentengine.webapp.common.controller;

import io.agentengine.core.common.record.*;
import io.agentengine.core.common.service.KnowledgeService;
import io.agentengine.webapp.common.handler.ResourceRequestHandler;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import java.util.*;

public abstract class CrudController<T> {
    protected final KnowledgeService resources;
    protected final ResourceRequestHandler requestHandler;
    private final String kind;

    protected CrudController(String kind, KnowledgeService resources, ResourceRequestHandler requestHandler) {
        this.kind = kind;
        this.resources = resources;
        this.requestHandler = requestHandler;
    }

    @GetMapping
    public List<ResourceResponseRecord> list(@RequestParam(required = false) UUID projectId, @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "50") int limit, @RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "false") boolean tombstones) {
        return resources.list(kind, projectId, q, limit, offset, tombstones);
    }

    @GetMapping("/{id}")
    public ResourceResponseRecord get(@PathVariable UUID id) {
        return resources.get(kind, id);
    }

    @PostMapping
    public ResourceResponseRecord create(@Valid @RequestBody ResourceWriteRecord<T> write) {
        return resources.create(kind, requestHandler.convert(write));
    }

    @PutMapping("/{id}")
    public ResourceResponseRecord update(@PathVariable UUID id, @Valid @RequestBody ResourceWriteRecord<T> write) {
        return resources.update(kind, id, requestHandler.convert(write));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id, @RequestParam long version) {
        resources.delete(kind, id, version);
    }

}
