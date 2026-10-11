package io.agentengine.core.project.handler;

import io.agentengine.core.common.enums.ResourceKind;
import io.agentengine.core.common.handler.AbstractResourcePersistenceHandler;
import io.agentengine.core.project.domain.Project;
import io.agentengine.core.project.repository.ProjectRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class ProjectPersistenceHandler extends AbstractResourcePersistenceHandler<Project> {
    private final ProjectRepository repository;

    public ProjectPersistenceHandler(ProjectRepository repository) {
        super(ResourceKind.PROJECT, Project.class, repository);
        this.repository = repository;
    }

    @Override
    public Optional<Project> findByOwnerIdAndId(UUID ownerId, UUID id) {
        return repository.findByIdAndOwnerIdAndDeletedFalse(id, ownerId);
    }

    @Override
    public List<Project> list(UUID ownerId, UUID projectId, boolean includeDeleted, Pageable pageable) {
        return repository.findByParams(ownerId, projectId, includeDeleted, pageable);
    }
}
