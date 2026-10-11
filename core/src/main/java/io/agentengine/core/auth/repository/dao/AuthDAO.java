package io.agentengine.core.auth.repository.dao;

import java.util.List;
import java.util.UUID;

public interface AuthDAO {
    boolean lockBootstrapConsumed();

    boolean isBootstrapFresh();

    void consumeBootstrap();

    List<UUID> lockAvailableInvitation(String tokenHash);

    void consumeInvitation(UUID id);

    void revokeAuthorizations(UUID ownerId, String clientId);

    boolean hasClientRelations(UUID ownerId, String clientId);
}
