package io.agentengine.api.auth.handler;

import io.agentengine.core.common.security.IdentitySecurity;
import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;

@Component
public class SessionHandler {
    public Map<String, Object> response(CsrfToken csrf) {
        return Map.of("userId", IdentitySecurity.owner(), "administrator", IdentitySecurity.authentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")), "csrfToken", csrf.getToken());
    }
}
