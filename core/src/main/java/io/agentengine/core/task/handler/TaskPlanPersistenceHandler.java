package io.agentengine.core.task.handler;

import io.agentengine.core.common.enums.ResourceKind;
import io.agentengine.core.common.handler.AbstractResourcePersistenceHandler;
import io.agentengine.core.task.domain.TaskPlan;
import io.agentengine.core.task.repository.TaskPlanRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class TaskPlanPersistenceHandler extends AbstractResourcePersistenceHandler<TaskPlan> {
    private final TaskPlanRepository repository;

    public TaskPlanPersistenceHandler(TaskPlanRepository repository) {
        super(ResourceKind.PLAN, TaskPlan.class, repository);
        this.repository = repository;
    }

    @Override
    public Optional<TaskPlan> findByOwnerIdAndId(UUID ownerId, UUID id) {
        return repository.findByIdAndOwnerIdAndDeletedFalse(id, ownerId);
    }

    @Override
    public List<TaskPlan> list(UUID ownerId, UUID projectId, boolean includeDeleted, Pageable pageable) {
        return repository.findByParams(ownerId, projectId, includeDeleted, pageable);
    }
}
