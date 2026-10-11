package io.agentengine.core.auth.repository.dao;

public interface RefreshTokenDAO {
    void lockByTokenHash(String tokenHash);
}
