package io.agentengine.api.auth.controller;

import io.agentengine.core.auth.record.OAuthRegistrationRequestRecord;
import io.agentengine.core.auth.service.OAuthClientService;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class DiscoveryController {
    private final OAuthClientService service;

    public DiscoveryController(OAuthClientService service) {
        this.service = service;
    }

    @GetMapping({"/.well-known/oauth-protected-resource", "/.well-known/oauth-protected-resource/mcp"})
    public Map<String, Object> resource() {
        return service.resource();
    }

    @PostMapping("/oauth/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody OAuthRegistrationRequestRecord request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request));
    }
}
