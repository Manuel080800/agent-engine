package io.agentengine.core.memory.handler;

import io.agentengine.core.common.enums.ResourceKind;
import io.agentengine.core.common.handler.AbstractResourcePersistenceHandler;
import io.agentengine.core.memory.domain.Memory;
import io.agentengine.core.memory.repository.MemoryRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class MemoryPersistenceHandler extends AbstractResourcePersistenceHandler<Memory> {
    private final MemoryRepository repository;

    public MemoryPersistenceHandler(MemoryRepository repository) {
        super(ResourceKind.MEMORY, Memory.class, repository);
        this.repository = repository;
    }

    @Override
    public Optional<Memory> findByOwnerIdAndId(UUID ownerId, UUID id) {
        return repository.findByIdAndOwnerIdAndDeletedFalse(id, ownerId);
    }

    @Override
    public List<Memory> list(UUID ownerId, UUID projectId, boolean includeDeleted, Pageable pageable) {
        return repository.findByParams(ownerId, projectId, includeDeleted, pageable);
    }
}
