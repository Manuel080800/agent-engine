package io.agentengine.core.execution.repository;

import io.agentengine.core.execution.domain.ExecutionEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionEventRepository extends JpaRepository<ExecutionEvent, UUID> {
    java.util.Optional<ExecutionEvent> findByIdAndOwnerId(UUID id, UUID ownerId);
}
