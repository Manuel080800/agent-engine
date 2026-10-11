package io.agentengine.core.auth.service;

import org.springframework.security.core.Authentication;

public interface LocalClientAuthenticationService {
    Authentication authenticate(Authentication authentication);

    boolean supports(Class<?> type);
}
