package io.agentengine.core.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.agentengine.core.auth.repository.dao.RefreshTokenDAO;
import io.agentengine.core.common.util.HashUtils;
import org.junit.jupiter.api.Test;

class TokenRefreshServiceTest {
    @Test
    void refreshIsLockedBeforeTheOperationAndDoesNotExposeTheTokenToDAO() {
        // El DAO recibe solo el hash y bloquea antes de que continúe la renovación.
        RefreshTokenDAO dao = mock(RefreshTokenDAO.class);
        Runnable operation = mock(Runnable.class);
        new TokenRefreshServiceImpl(dao).execute("private-refresh-token", operation);
        var order = inOrder(dao, operation);
        order.verify(dao).lockByTokenHash(HashUtils.sha256("private-refresh-token"));
        order.verify(operation).run();
        verifyNoMoreInteractions(dao, operation);
    }

    @Test
    void failurePropagatesThroughTheTransactionalBoundary() {
        // Una operación fallida conserva su excepción para provocar rollback en el
        // servicio.
        RefreshTokenDAO dao = mock(RefreshTokenDAO.class);
        RuntimeException failure = new RuntimeException("failed exchange");
        assertSame(failure, assertThrows(RuntimeException.class, () -> new TokenRefreshServiceImpl(dao).execute("token", () -> {
            throw failure;
        })));
    }
}
