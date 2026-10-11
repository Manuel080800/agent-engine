package io.agentengine.core.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocalClientAuthenticationServiceTest {
    @Test
    void authenticatesRegisteredPublicPkceClientForRefresh() {
        // Permite renovar un cliente dinámico registrado con PKCE y permiso de refresh.
        var repository = mock(RegisteredClientRepository.class);
        var client = client(ClientAuthenticationMethod.NONE, true, true);
        when(repository.findByClientId("dynamic-client")).thenReturn(client);
        var result = new LocalClientAuthenticationServiceImpl(repository).authenticate(request());

        assertNotNull(result);
        assertTrue(result.isAuthenticated());
        assertEquals("dynamic-client", result.getName());
    }

    @Test
    void rejectsUnknownConfidentialAndUnqualifiedClients() {
        // No permite usar el flujo público para clientes desconocidos, confidenciales o sin PKCE/refresh.
        var repository = mock(RegisteredClientRepository.class);
        var service = new LocalClientAuthenticationServiceImpl(repository);
        assertNull(service.authenticate(request()));

        for (var client : new RegisteredClient[] {client(ClientAuthenticationMethod.CLIENT_SECRET_BASIC, true, true), client(ClientAuthenticationMethod.NONE, false, true), client(ClientAuthenticationMethod.NONE, true, false)}) {
            when(repository.findByClientId("dynamic-client")).thenReturn(client);
            assertNull(service.authenticate(request()));
        }
    }

    private OAuth2ClientAuthenticationToken request() {
        return new OAuth2ClientAuthenticationToken("dynamic-client", ClientAuthenticationMethod.NONE, null, Map.of("grant_type", "refresh_token"));
    }

    private RegisteredClient client(ClientAuthenticationMethod method, boolean pkce, boolean refresh) {
        var builder = RegisteredClient.withId("test-client").clientId("dynamic-client").clientAuthenticationMethod(method).authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).redirectUri("http://127.0.0.1/callback").clientSettings(ClientSettings.builder().requireProofKey(pkce).build());

        if (refresh) {
            builder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
        }

        return builder.build();
    }
}
