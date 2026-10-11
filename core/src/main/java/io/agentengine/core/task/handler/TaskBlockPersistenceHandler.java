package io.agentengine.core.task.handler;

import io.agentengine.core.common.enums.ResourceKind;
import io.agentengine.core.common.handler.AbstractResourcePersistenceHandler;
import io.agentengine.core.task.domain.TaskBlock;
import io.agentengine.core.task.repository.TaskBlockRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class TaskBlockPersistenceHandler extends AbstractResourcePersistenceHandler<TaskBlock> {
    private final TaskBlockRepository repository;

    public TaskBlockPersistenceHandler(TaskBlockRepository repository) {
        super(ResourceKind.BLOCK, TaskBlock.class, repository);
        this.repository = repository;
    }

    @Override
    public Optional<TaskBlock> findByOwnerIdAndId(UUID ownerId, UUID id) {
        return repository.findByIdAndOwnerIdAndDeletedFalse(id, ownerId);
    }

    @Override
    public List<TaskBlock> list(UUID ownerId, UUID projectId, boolean includeDeleted, Pageable pageable) {
        return repository.findByParams(ownerId, projectId, includeDeleted, pageable);
    }
}
