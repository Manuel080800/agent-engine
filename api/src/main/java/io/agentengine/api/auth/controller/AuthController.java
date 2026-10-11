package io.agentengine.api.auth.controller;

import io.agentengine.core.auth.record.RegistrationRequestRecord;
import io.agentengine.core.auth.service.AuthService;
import io.agentengine.api.auth.handler.SessionHandler;

import jakarta.validation.Valid;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/v1")
public class AuthController {
    private final AuthService service;
    private final SessionHandler sessionHandler;

    public AuthController(AuthService service, SessionHandler sessionHandler) {
        this.service = service;
        this.sessionHandler = sessionHandler;
    }

    @GetMapping("/session")
    public Map<String, Object> session(CsrfToken csrf) {
        return sessionHandler.response(csrf);
    }

    @PostMapping("/auth/bootstrap")
    public Map<String, Object> bootstrap(@Valid @RequestBody RegistrationRequestRecord request) {
        return service.bootstrap(request);
    }

    @PostMapping("/auth/redeem")
    public Map<String, Object> redeem(@Valid @RequestBody RegistrationRequestRecord request) {
        return service.redeem(request);
    }

    @PostMapping("/admin/invitations")
    public Map<String, Object> invitation() {
        return service.invitation();
    }
}
