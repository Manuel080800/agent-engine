package io.agentengine.core.common.repository.dao;

import io.agentengine.core.common.domain.VersionedResource;
import io.agentengine.core.common.enums.ResourceKind;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResourceDAO {
    List<VersionedResource> search(ResourceKind kind, UUID ownerId, UUID projectId, String query, int limit, int offset, boolean tombstones);

    Optional<VersionedResource> findLockedByOwnerIdAndId(ResourceKind kind, UUID ownerId, UUID id);
}
