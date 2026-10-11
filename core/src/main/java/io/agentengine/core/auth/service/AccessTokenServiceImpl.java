package io.agentengine.core.auth.service;

import java.util.Objects;

import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccessTokenServiceImpl implements AccessTokenService {
    private final OAuth2AuthorizationService authorizations;

    public AccessTokenServiceImpl(OAuth2AuthorizationService authorizations) {
        this.authorizations = authorizations;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(String token) {
        var authorization = authorizations.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
        return Objects.nonNull(authorization) && Objects.nonNull(authorization.getAccessToken()) && authorization.getAccessToken().isActive();
    }
}
