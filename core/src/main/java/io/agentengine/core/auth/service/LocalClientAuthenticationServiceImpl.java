package io.agentengine.core.auth.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class LocalClientAuthenticationServiceImpl implements LocalClientAuthenticationService {
    private final RegisteredClientRepository clients;

    public LocalClientAuthenticationServiceImpl(RegisteredClientRepository clients) {
        this.clients = clients;
    }

    @Override
    @Transactional(readOnly = true)
    public Authentication authenticate(Authentication authentication) {
        var token = (OAuth2ClientAuthenticationToken) authentication;

        if (!ClientAuthenticationMethod.NONE.equals(token.getClientAuthenticationMethod()) || !"refresh_token".equals(token.getAdditionalParameters().get("grant_type"))) {
            return null;
        }

        var client = clients.findByClientId(token.getPrincipal().toString());

        if (Objects.isNull(client) || !client.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE) || !client.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN) || !client.getClientSettings().isRequireProofKey()) {
            return null;
        }

        return new OAuth2ClientAuthenticationToken(client, ClientAuthenticationMethod.NONE, null);
    }

    @Override
    public boolean supports(Class<?> type) {
        return OAuth2ClientAuthenticationToken.class.isAssignableFrom(type);
    }
}
