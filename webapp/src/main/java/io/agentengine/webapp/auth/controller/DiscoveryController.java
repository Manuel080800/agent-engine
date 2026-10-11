package io.agentengine.webapp.auth.controller;

import io.agentengine.core.auth.service.OAuthClientService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class DiscoveryController {
    private final OAuthClientService service;
    private final String issuer;

    public DiscoveryController(OAuthClientService service, @Value("${engine.issuer}") String issuer) {
        this.service = service;
        this.issuer = issuer;
    }

    @GetMapping("/.well-known/oauth-protected-resource/api/mcp")
    public Map<String, Object> resource() {
        return service.resource();
    }

    @GetMapping("/.well-known/oauth-authorization-server/api")
    public Map<String, Object> metadata() {
        return Map.of("issuer", issuer, "authorization_endpoint", issuer + "/oauth2/authorize", "token_endpoint", issuer + "/oauth2/token", "jwks_uri", issuer + "/oauth2/jwks", "registration_endpoint", issuer + "/oauth/register", "response_types_supported", List.of("code"), "grant_types_supported", List.of("authorization_code", "refresh_token"), "token_endpoint_auth_methods_supported", List.of("none"), "code_challenge_methods_supported", List.of("S256"), "scopes_supported", List.of("context:read", "knowledge:write", "coordinator:write"));
    }
}
