package io.agentengine.core.task.handler;

import io.agentengine.core.common.enums.ResourceKind;
import io.agentengine.core.common.handler.AbstractResourcePersistenceHandler;
import io.agentengine.core.task.domain.ReviewFinding;
import io.agentengine.core.task.repository.ReviewFindingRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class ReviewFindingPersistenceHandler extends AbstractResourcePersistenceHandler<ReviewFinding> {
    private final ReviewFindingRepository repository;

    public ReviewFindingPersistenceHandler(ReviewFindingRepository repository) {
        super(ResourceKind.REVIEW_FINDING, ReviewFinding.class, repository);
        this.repository = repository;
    }

    @Override
    public Optional<ReviewFinding> findByOwnerIdAndId(UUID ownerId, UUID id) {
        return repository.findByIdAndOwnerIdAndDeletedFalse(id, ownerId);
    }

    @Override
    public List<ReviewFinding> list(UUID ownerId, UUID projectId, boolean includeDeleted, Pageable pageable) {
        return repository.findByParams(ownerId, projectId, includeDeleted, pageable);
    }
}
