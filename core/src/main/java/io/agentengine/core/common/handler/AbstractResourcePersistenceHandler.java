package io.agentengine.core.common.handler;

import io.agentengine.core.common.domain.VersionedResource;
import io.agentengine.core.common.enums.ResourceKind;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public abstract class AbstractResourcePersistenceHandler<T extends VersionedResource> implements ResourcePersistenceHandler {
    private final ResourceKind kind;
    private final Class<T> entityType;
    private final JpaRepository<T, UUID> repository;

    protected AbstractResourcePersistenceHandler(ResourceKind kind, Class<T> entityType, JpaRepository<T, UUID> repository) {
        this.kind = kind;
        this.entityType = entityType;
        this.repository = repository;
    }

    @Override
    public ResourceKind kind() {
        return kind;
    }

    @Override
    public void save(VersionedResource entity) {
        repository.saveAndFlush(entityType.cast(entity));
    }
}
