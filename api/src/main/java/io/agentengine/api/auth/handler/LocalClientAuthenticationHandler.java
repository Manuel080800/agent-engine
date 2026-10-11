package io.agentengine.api.auth.handler;

import io.agentengine.core.auth.service.LocalClientAuthenticationService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;

public class LocalClientAuthenticationHandler implements AuthenticationProvider {
    private final LocalClientAuthenticationService service;

    public LocalClientAuthenticationHandler(LocalClientAuthenticationService service) {
        this.service = service;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        return service.authenticate(authentication);
    }

    @Override
    public boolean supports(Class<?> type) {
        return service.supports(type);
    }
}
