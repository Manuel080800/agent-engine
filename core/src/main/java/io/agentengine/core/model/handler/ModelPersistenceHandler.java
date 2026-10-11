package io.agentengine.core.model.handler;

import io.agentengine.core.common.enums.ResourceKind;
import io.agentengine.core.common.handler.AbstractResourcePersistenceHandler;
import io.agentengine.core.model.domain.Model;
import io.agentengine.core.model.repository.ModelRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class ModelPersistenceHandler extends AbstractResourcePersistenceHandler<Model> {
    private final ModelRepository repository;

    public ModelPersistenceHandler(ModelRepository repository) {
        super(ResourceKind.MODEL, Model.class, repository);
        this.repository = repository;
    }

    @Override
    public Optional<Model> findByOwnerIdAndId(UUID ownerId, UUID id) {
        return repository.findByIdAndOwnerIdAndDeletedFalse(id, ownerId);
    }

    @Override
    public List<Model> list(UUID ownerId, UUID projectId, boolean includeDeleted, Pageable pageable) {
        return repository.findByParams(ownerId, projectId, includeDeleted, pageable);
    }
}
