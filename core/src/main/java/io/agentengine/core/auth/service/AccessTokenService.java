package io.agentengine.core.auth.service;

public interface AccessTokenService {
    boolean isActive(String token);
}
