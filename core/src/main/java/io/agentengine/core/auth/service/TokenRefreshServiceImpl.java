package io.agentengine.core.auth.service;

import io.agentengine.core.auth.repository.dao.RefreshTokenDAO;
import io.agentengine.core.common.util.HashUtils;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenRefreshServiceImpl implements TokenRefreshService {
    private final RefreshTokenDAO refreshTokenDAO;

    public TokenRefreshServiceImpl(RefreshTokenDAO refreshTokenDAO) {
        this.refreshTokenDAO = refreshTokenDAO;
    }

    @Override
    @Transactional
    public void execute(String refreshToken, Runnable operation) {
        refreshTokenDAO.lockByTokenHash(HashUtils.sha256(refreshToken));
        operation.run();
    }
}
