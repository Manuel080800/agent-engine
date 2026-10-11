package io.agentengine.core.task.handler;

import io.agentengine.core.common.enums.ResourceKind;
import io.agentengine.core.common.handler.AbstractResourcePersistenceHandler;
import io.agentengine.core.task.domain.Task;
import io.agentengine.core.task.repository.TaskRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class TaskPersistenceHandler extends AbstractResourcePersistenceHandler<Task> {
    private final TaskRepository repository;

    public TaskPersistenceHandler(TaskRepository repository) {
        super(ResourceKind.TASK, Task.class, repository);
        this.repository = repository;
    }

    @Override
    public Optional<Task> findByOwnerIdAndId(UUID ownerId, UUID id) {
        return repository.findByIdAndOwnerIdAndDeletedFalse(id, ownerId);
    }

    @Override
    public List<Task> list(UUID ownerId, UUID projectId, boolean includeDeleted, Pageable pageable) {
        return repository.findByParams(ownerId, projectId, includeDeleted, pageable);
    }
}
