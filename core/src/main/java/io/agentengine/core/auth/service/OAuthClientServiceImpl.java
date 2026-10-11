package io.agentengine.core.auth.service;

import io.agentengine.core.auth.record.OAuthRegistrationRequestRecord;

import java.net.URI;
import java.time.Duration;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.server.authorization.client.*;
import org.springframework.security.oauth2.server.authorization.settings.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuthClientServiceImpl implements OAuthClientService {
    private final RegisteredClientRepository clients;
    private final String issuer;
    private final String redirect;
    private final boolean server;

    public OAuthClientServiceImpl(RegisteredClientRepository clients, @Value("${engine.issuer}") String issuer, @Value("${engine.local-client-redirect}") String redirect, @Value("${engine.authorization-server:false}") boolean server) {
        this.clients = clients;
        this.issuer = issuer;
        this.redirect = redirect;
        this.server = server;
    }

    @Override
    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void initializeLocalClient() {
        if (server && clients.findByClientId("agent-engine-local") == null) {
            clients.save(RegisteredClient.withId(UUID.randomUUID().toString()).clientId("agent-engine-local").clientAuthenticationMethod(ClientAuthenticationMethod.NONE).authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN).redirectUri(redirect).scope("context:read").scope("coordinator:write").clientSettings(ClientSettings.builder().requireProofKey(true).requireAuthorizationConsent(true).build()).tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofMinutes(15)).refreshTokenTimeToLive(Duration.ofDays(7)).reuseRefreshTokens(false).build()).build());
        }
    }

    @Override
    public Map<String, Object> resource() {
        return Map.of("resource", issuer + "/mcp", "authorization_servers", List.of(issuer), "scopes_supported", List.of("context:read", "knowledge:write"), "bearer_methods_supported", List.of("header"));
    }

    @Override
    @Transactional
    public Map<String, Object> register(OAuthRegistrationRequestRecord registration) {
        if (Objects.isNull(registration.redirect_uris()) || registration.redirect_uris().isEmpty() || registration.redirect_uris().size() > 5 || (Objects.nonNull(registration.token_endpoint_auth_method()) && !registration.token_endpoint_auth_method().equals("none"))) {
            throw new IllegalArgumentException("Public client registration required");
        }

        for (var redirect : registration.redirect_uris()) {
            var uri = URI.create(redirect);

            if (Objects.nonNull(uri.getFragment()) || Objects.nonNull(uri.getUserInfo()) || !("https".equals(uri.getScheme()) || ("http".equals(uri.getScheme()) && Set.of("127.0.0.1", "localhost", "[::1]").contains(uri.getHost())))) {
                throw new IllegalArgumentException("HTTPS or loopback redirect required");
            }
        }

        var id = "codex-" + UUID.randomUUID();
        var builder = RegisteredClient.withId(UUID.randomUUID().toString()).clientId(id).clientAuthenticationMethod(ClientAuthenticationMethod.NONE).authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN).scope("context:read").scope("knowledge:write").clientSettings(ClientSettings.builder().requireProofKey(true).requireAuthorizationConsent(true).build()).tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofMinutes(15)).refreshTokenTimeToLive(Duration.ofDays(7)).reuseRefreshTokens(false).build());
        registration.redirect_uris().forEach(builder::redirectUri);
        clients.save(builder.build());
        return Map.of("client_id", id, "redirect_uris", registration.redirect_uris(), "token_endpoint_auth_method", "none", "grant_types", List.of("authorization_code", "refresh_token"), "response_types", List.of("code"), "scope", "context:read knowledge:write");
    }
}
