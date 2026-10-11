package io.agentengine.core.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.agentengine.core.auth.repository.LocalClientRepository;
import io.agentengine.core.auth.repository.dao.AuthDAO;
import io.agentengine.core.common.exception.DomainException;
import io.agentengine.core.common.record.ResourceResponseRecord;
import io.agentengine.core.common.service.ResourceService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class LocalClientServiceTest {
    private final ResourceService resources = mock(ResourceService.class);
    private final AuthDAO dao = mock(AuthDAO.class);
    private final LocalClientRepository repository = mock(LocalClientRepository.class);
    private final LocalClientService service = new LocalClientServiceImpl(resources, dao, repository);
    private final UUID ownerId = UUID.randomUUID();
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(ownerId.toString(), null, List.of()));
        configure(false);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void inactiveUnrelatedClientIsDeletedAndAudited() {
        // Solo elimina físicamente un cliente inactivo sin relaciones y conserva la
        // auditoría.
        service.delete(id, 2);
        var order = inOrder(resources, dao, repository);
        order.verify(resources).get("local-clients", id, true);
        order.verify(dao).hasClientRelations(ownerId, "agent-engine-local");
        order.verify(repository).deleteById(id);
        order.verify(resources).audit("PURGE_local-clients", id);
    }

    @Test
    void relatedClientCannotBeDeleted() {
        // Un cliente con historial o autorizaciones debe conservarse.
        when(dao.hasClientRelations(ownerId, "agent-engine-local")).thenReturn(true);
        assertThrows(DomainException.class, () -> service.delete(id, 2));
        verifyNoInteractions(repository);
    }

    @Test
    void activeClientMustBeDeactivatedFirst() {
        // La eliminación no revoca ni borra un cliente activo de forma implícita.
        configure(true);
        assertThrows(DomainException.class, () -> service.delete(id, 2));
        verifyNoInteractions(dao, repository);
    }

    @Test
    void staleRevisionCannotDeleteClient() {
        // Una revisión obsoleta no permite borrar el registro actualizado.
        assertThrows(DomainException.class, () -> service.delete(id, 1));
        verifyNoInteractions(dao, repository);
    }

    @Test
    void anotherOwnersResourceCannotBeDeleted() {
        // El bloqueo mediante el servicio respeta el propietario antes de eliminar.
        when(resources.get("local-clients", id, true)).thenThrow(new DomainException(io.agentengine.core.common.enums.DomainError.NOT_FOUND));
        assertThrows(DomainException.class, () -> service.delete(id, 2));
        verifyNoInteractions(dao, repository);
    }

    private void configure(boolean enabled) {
        when(resources.get("local-clients", id, true)).thenReturn(new ResourceResponseRecord(id, null, 2, false, Map.of("name", "Local", "clientId", "agent-engine-local", "enabled", enabled)));
    }
}
