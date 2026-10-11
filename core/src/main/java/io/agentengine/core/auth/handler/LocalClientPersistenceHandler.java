package io.agentengine.core.auth.handler;

import io.agentengine.core.common.enums.ResourceKind;
import io.agentengine.core.common.handler.AbstractResourcePersistenceHandler;
import io.agentengine.core.auth.domain.LocalClient;
import io.agentengine.core.auth.repository.LocalClientRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class LocalClientPersistenceHandler extends AbstractResourcePersistenceHandler<LocalClient> {
    private final LocalClientRepository repository;

    public LocalClientPersistenceHandler(LocalClientRepository repository) {
        super(ResourceKind.LOCAL_CLIENT, LocalClient.class, repository);
        this.repository = repository;
    }

    @Override
    public Optional<LocalClient> findByOwnerIdAndId(UUID ownerId, UUID id) {
        return repository.findByIdAndOwnerIdAndDeletedFalse(id, ownerId);
    }

    @Override
    public List<LocalClient> list(UUID ownerId, UUID projectId, boolean includeDeleted, Pageable pageable) {
        return repository.findByParams(ownerId, projectId, includeDeleted, pageable);
    }
}
