package io.agentengine.core.memory.handler;

import io.agentengine.core.common.enums.ResourceKind;
import io.agentengine.core.common.handler.AbstractResourcePersistenceHandler;
import io.agentengine.core.memory.domain.MemoryProposal;
import io.agentengine.core.memory.repository.MemoryProposalRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class MemoryProposalPersistenceHandler extends AbstractResourcePersistenceHandler<MemoryProposal> {
    private final MemoryProposalRepository repository;

    public MemoryProposalPersistenceHandler(MemoryProposalRepository repository) {
        super(ResourceKind.MEMORY_PROPOSAL, MemoryProposal.class, repository);
        this.repository = repository;
    }

    @Override
    public Optional<MemoryProposal> findByOwnerIdAndId(UUID ownerId, UUID id) {
        return repository.findByIdAndOwnerIdAndDeletedFalse(id, ownerId);
    }

    @Override
    public List<MemoryProposal> list(UUID ownerId, UUID projectId, boolean includeDeleted, Pageable pageable) {
        return repository.findByParams(ownerId, projectId, includeDeleted, pageable);
    }
}
