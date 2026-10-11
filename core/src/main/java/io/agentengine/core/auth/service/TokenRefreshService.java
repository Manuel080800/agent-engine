package io.agentengine.core.auth.service;

public interface TokenRefreshService {
    void execute(String refreshToken, Runnable operation);
}
