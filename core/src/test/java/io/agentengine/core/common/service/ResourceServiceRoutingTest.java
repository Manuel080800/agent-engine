package io.agentengine.core.common.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentengine.core.audit.repository.AuditRepository;
import io.agentengine.core.common.dataconverter.ResourceDataConverter;
import io.agentengine.core.common.enums.ResourceKind;
import io.agentengine.core.common.handler.ResourcePersistenceHandler;
import io.agentengine.core.common.handler.ResourcePersistenceHandlerResolver;
import io.agentengine.core.common.handler.ResourceValidationHandler;
import io.agentengine.core.common.repository.dao.ResourceDAO;
import io.agentengine.core.common.util.JsonCodec;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class ResourceServiceRoutingTest {
    private final ResourceDAO dao = mock(ResourceDAO.class);
    private final ResourcePersistenceHandler handler = mock(ResourcePersistenceHandler.class);
    private final ResourcePersistenceHandlerResolver resolver = mock(ResourcePersistenceHandlerResolver.class);
    private final JsonCodec json = new JsonCodec(new ObjectMapper());
    private final ResourceService service = new ResourceServiceImpl(dao, resolver, new ResourceDataConverter(), mock(AuditRepository.class), json, new ResourceValidationHandler(json), mock(io.agentengine.core.model.service.ModelConfigurationService.class));

    private final UUID owner = UUID.randomUUID();

    @AfterEach
    void clearIdentity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ordinaryListUsesTheRepositoryHandlerAndClampsPagination() {
        // Una lista sin búsqueda usa el repositorio, conserva el propietario y limita
        // la paginación.
        authenticate();
        when(resolver.resolve(ResourceKind.PROJECT)).thenReturn(handler);
        when(handler.list(eq(owner), isNull(), eq(false), any(Pageable.class))).thenReturn(List.of());
        assertTrue(service.list("projects", null, " ", 500, -10, false).isEmpty());
        var page = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(handler).list(eq(owner), isNull(), eq(false), page.capture());
        assertEquals(100, page.getValue().getPageSize());
        assertEquals(0, page.getValue().getOffset());
        verifyNoInteractions(dao);
    }

    @Test
    void fullTextSearchUsesOnlyTheComplexQueryDAO() {
        // La búsqueda de texto utiliza el DAO PostgreSQL y no la consulta simple del
        // repositorio.
        authenticate();
        when(dao.search(ResourceKind.PROJECT, owner, null, "search term", 25, 3, true)).thenReturn(List.of());
        assertTrue(service.list("projects", null, "search term", 25, 3, true).isEmpty());
        verify(dao).search(ResourceKind.PROJECT, owner, null, "search term", 25, 3, true);
        verifyNoInteractions(resolver, handler);
    }

    private void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(owner.toString(), null, List.of()));
    }
}
