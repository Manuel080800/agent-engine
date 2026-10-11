package io.agentengine.core.auth.record;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OAuthRegistrationRequestRecordTest {
    @Test
    void acceptsCodexRegistrationMetadata() throws Exception {
        // Acepta los metadatos estándar enviados por Codex sin perder los parámetros de seguridad.
        var mapper = JsonMapper.builder().build();
        var request = mapper.readValue("""
            {"redirect_uris":["http://127.0.0.1:8765/callback"],"token_endpoint_auth_method":"none","grant_types":["authorization_code"],"scope":"context:read knowledge:write","response_types":["code"],"client_uri":"https://openai.com"}
            """, OAuthRegistrationRequestRecord.class);

        assertEquals("none", request.token_endpoint_auth_method());
        assertEquals("http://127.0.0.1:8765/callback", request.redirect_uris().getFirst());
        assertEquals("context:read knowledge:write", request.scope());
    }
}
