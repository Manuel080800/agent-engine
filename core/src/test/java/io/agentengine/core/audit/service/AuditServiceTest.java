package io.agentengine.core.audit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.agentengine.core.audit.domain.AuditEntry;
import io.agentengine.core.audit.repository.AuditRepository;
import io.agentengine.core.audit.dataconverter.AuditDataConverter;
import org.springframework.data.domain.Pageable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import io.agentengine.core.common.exception.DomainException;
import io.agentengine.core.common.enums.DomainError;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

class AuditServiceTest {
    private final AuditRepository repository = mock(AuditRepository.class);
    private final AuditService service = new AuditServiceImpl(repository, new AuditDataConverter());

    @AfterEach
    void clearIdentity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unauthenticatedCallerCannotReadAudit() {
        // La consulta sin identidad falla antes de acceder a persistencia.
        DomainException failure = assertThrows(DomainException.class, () -> service.list(0));
        assertEquals(DomainError.UNAUTHORIZED, failure.getError());
        verifyNoInteractions(repository);
    }

    @Test
    void bearerCredentialCannotReadHumanAudit() {
        // Una credencial del coordinador no sustituye la sesión humana de auditoría.
        UUID owner = UUID.randomUUID();
        Jwt token = Jwt.withTokenValue("test-token").header("alg", "RS256").subject(owner.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(token, List.of(new SimpleGrantedAuthority("SCOPE_coordinator:write"))));
        DomainException failure = assertThrows(DomainException.class, () -> service.list(0));
        assertEquals(DomainError.FORBIDDEN, failure.getError());
        verifyNoInteractions(repository);
    }

    @Test
    void authenticatedAuditUsesOwnerAndTypedResponse() {
        // El servicio limita la consulta al propietario y devuelve el DTO de auditoría.
        UUID owner = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(owner.toString(), null, List.of()));
        AuditEntry entry = new AuditEntry();
        entry.setOperation("CREATE_PROJECT");
        entry.setResourceId(UUID.randomUUID());
        entry.setCreatedAt(Instant.parse("2026-10-11T00:00:00Z"));
        when(repository.findByOwnerIdOrderByCreatedAtDescIdAsc(eq(owner), any(Pageable.class))).thenReturn(List.of(entry));
        var response = service.list(-1).getFirst();
        assertEquals(entry.getResourceId(), response.resourceId());
        assertEquals(entry.getOperation(), response.operation());
        assertEquals(entry.getCreatedAt(), response.createdAt());
        var pagination = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByOwnerIdOrderByCreatedAtDescIdAsc(eq(owner), pagination.capture());
        assertEquals(0, pagination.getValue().getOffset());
        assertEquals(100, pagination.getValue().getPageSize());
    }
}
