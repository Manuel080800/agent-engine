package io.agentengine.core.execution.handler;

import io.agentengine.core.common.enums.ResourceKind;
import io.agentengine.core.common.handler.AbstractResourcePersistenceHandler;
import io.agentengine.core.execution.domain.Checkpoint;
import io.agentengine.core.execution.repository.CheckpointRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class CheckpointPersistenceHandler extends AbstractResourcePersistenceHandler<Checkpoint> {
    private final CheckpointRepository repository;

    public CheckpointPersistenceHandler(CheckpointRepository repository) {
        super(ResourceKind.CHECKPOINT, Checkpoint.class, repository);
        this.repository = repository;
    }

    @Override
    public Optional<Checkpoint> findByOwnerIdAndId(UUID ownerId, UUID id) {
        return repository.findByIdAndOwnerIdAndDeletedFalse(id, ownerId);
    }

    @Override
    public List<Checkpoint> list(UUID ownerId, UUID projectId, boolean includeDeleted, Pageable pageable) {
        return repository.findByParams(ownerId, projectId, includeDeleted, pageable);
    }
}
