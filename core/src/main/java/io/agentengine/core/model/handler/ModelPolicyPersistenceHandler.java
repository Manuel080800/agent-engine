package io.agentengine.core.model.handler;

import io.agentengine.core.common.enums.ResourceKind;
import io.agentengine.core.common.handler.AbstractResourcePersistenceHandler;
import io.agentengine.core.model.domain.ModelPolicy;
import io.agentengine.core.model.repository.ModelPolicyRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class ModelPolicyPersistenceHandler extends AbstractResourcePersistenceHandler<ModelPolicy> {
    private final ModelPolicyRepository repository;

    public ModelPolicyPersistenceHandler(ModelPolicyRepository repository) {
        super(ResourceKind.MODEL_POLICY, ModelPolicy.class, repository);
        this.repository = repository;
    }

    @Override
    public Optional<ModelPolicy> findByOwnerIdAndId(UUID ownerId, UUID id) {
        return repository.findByIdAndOwnerIdAndDeletedFalse(id, ownerId);
    }

    @Override
    public List<ModelPolicy> list(UUID ownerId, UUID projectId, boolean includeDeleted, Pageable pageable) {
        return repository.findByParams(ownerId, projectId, includeDeleted, pageable);
    }
}
