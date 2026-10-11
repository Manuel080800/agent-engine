package io.agentengine.core.common.handler;

import io.agentengine.core.common.domain.VersionedResource;
import io.agentengine.core.common.enums.ResourceKind;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface ResourcePersistenceHandler {
    ResourceKind kind();

    Optional<? extends VersionedResource> findByOwnerIdAndId(UUID ownerId, UUID id);

    List<? extends VersionedResource> list(UUID ownerId, UUID projectId, boolean includeDeleted, Pageable pageable);

    void save(VersionedResource entity);
}
