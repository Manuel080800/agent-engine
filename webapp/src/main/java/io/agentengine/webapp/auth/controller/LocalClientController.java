package io.agentengine.webapp.auth.controller;

import io.agentengine.core.auth.record.LinkClientRequestRecord;
import io.agentengine.core.auth.service.LocalClientService;
import io.agentengine.core.common.record.ResourceResponseRecord;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/web/v1/local-clients")
public class LocalClientController {
    private final LocalClientService service;

    public LocalClientController(LocalClientService service) {
        this.service = service;
    }

    @PostMapping
    public ResourceResponseRecord link(@Valid @RequestBody LinkClientRequestRecord request) {
        return service.link(request);
    }

    @DeleteMapping("/{id}")
    public void revoke(@PathVariable UUID id, @RequestParam long version) {
        service.revoke(id, version);
    }

    @DeleteMapping("/{id}/permanent")
    public void delete(@PathVariable UUID id, @RequestParam long version) {
        service.delete(id, version);
    }
}
