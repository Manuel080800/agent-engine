package io.agentengine.core.rule.handler;

import io.agentengine.core.common.enums.ResourceKind;
import io.agentengine.core.common.handler.AbstractResourcePersistenceHandler;
import io.agentengine.core.rule.domain.Rule;
import io.agentengine.core.rule.repository.RuleRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class RulePersistenceHandler extends AbstractResourcePersistenceHandler<Rule> {
    private final RuleRepository repository;

    public RulePersistenceHandler(RuleRepository repository) {
        super(ResourceKind.RULE, Rule.class, repository);
        this.repository = repository;
    }

    @Override
    public Optional<Rule> findByOwnerIdAndId(UUID ownerId, UUID id) {
        return repository.findByIdAndOwnerIdAndDeletedFalse(id, ownerId);
    }

    @Override
    public List<Rule> list(UUID ownerId, UUID projectId, boolean includeDeleted, Pageable pageable) {
        return repository.findByParams(ownerId, projectId, includeDeleted, pageable);
    }
}
