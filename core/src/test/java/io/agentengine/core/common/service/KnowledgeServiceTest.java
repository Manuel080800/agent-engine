package io.agentengine.core.common.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.agentengine.core.common.record.ResourceWriteRecord;
import io.agentengine.core.common.enums.DomainError;
import io.agentengine.core.common.exception.DomainException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class KnowledgeServiceTest {
    @AfterEach
    void clearIdentity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void coordinatorCannotUseKnowledgeCRUDWithoutWriteScope() {
        // El servicio de negocio rechaza la credencial de ejecución antes de acceder al
        // CRUD.
        ResourceService resources = mock(ResourceService.class);
        UUID owner = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "RS256").subject(owner.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("SCOPE_coordinator:write"))));
        var service = new KnowledgeServiceImpl(resources);
        DomainException failure = assertThrows(DomainException.class, () -> service.create("projects", new ResourceWriteRecord<>(null, null, Map.of("name", "Project"))));
        assertEquals(DomainError.FORBIDDEN, failure.getError());
        verifyNoInteractions(resources);
    }

    @Test
    void genericCRUDCannotBypassTaskWorkflow() {
        // La fachada de conocimiento no permite modificar tareas ni recursos
        // operativos.
        ResourceService resources = mock(ResourceService.class);
        assertThrows(IllegalArgumentException.class, () -> new KnowledgeServiceImpl(resources).delete("tasks", UUID.randomUUID(), 1));
        verifyNoInteractions(resources);
    }
}
