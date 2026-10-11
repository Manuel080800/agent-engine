package io.agentengine.core.skill.handler;

import io.agentengine.core.common.enums.ResourceKind;
import io.agentengine.core.common.handler.AbstractResourcePersistenceHandler;
import io.agentengine.core.skill.domain.Skill;
import io.agentengine.core.skill.repository.SkillRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class SkillPersistenceHandler extends AbstractResourcePersistenceHandler<Skill> {
    private final SkillRepository repository;

    public SkillPersistenceHandler(SkillRepository repository) {
        super(ResourceKind.SKILL, Skill.class, repository);
        this.repository = repository;
    }

    @Override
    public Optional<Skill> findByOwnerIdAndId(UUID ownerId, UUID id) {
        return repository.findByIdAndOwnerIdAndDeletedFalse(id, ownerId);
    }

    @Override
    public List<Skill> list(UUID ownerId, UUID projectId, boolean includeDeleted, Pageable pageable) {
        return repository.findByParams(ownerId, projectId, includeDeleted, pageable);
    }
}
