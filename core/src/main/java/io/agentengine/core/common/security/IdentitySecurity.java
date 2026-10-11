package io.agentengine.core.common.security;

import io.agentengine.core.common.enums.DomainError;
import io.agentengine.core.common.exception.DomainException;

import java.util.Objects;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class IdentitySecurity {
    private IdentitySecurity() {
    }

    public static Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static UUID owner() {
        var authentication = authentication();

        if (Objects.isNull(authentication) || !authentication.isAuthenticated()) {
            throw new DomainException(DomainError.UNAUTHORIZED);
        }

        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new DomainException(DomainError.UNAUTHORIZED);
        }
    }

    public static void human() {
        owner();

        if (authentication() instanceof JwtAuthenticationToken) {
            throw new DomainException(DomainError.FORBIDDEN, "A human web session is required");
        }
    }

    public static void admin() {
        human();

        if (authentication().getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new DomainException(DomainError.FORBIDDEN);
        }
    }

    public static String coordinator() {
        owner();

        if (!(authentication() instanceof JwtAuthenticationToken jwt) || !jwt.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("SCOPE_coordinator:write"))) {
            throw new DomainException(DomainError.FORBIDDEN, "Coordinator credential required");
        }

        return jwt.getToken().getClaimAsString("client_id");
    }

    public static void knowledgeWrite() {
        owner();

        if (authentication() instanceof JwtAuthenticationToken jwt && jwt.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("SCOPE_knowledge:write"))) {
            throw new DomainException(DomainError.FORBIDDEN);
        }
    }
}
