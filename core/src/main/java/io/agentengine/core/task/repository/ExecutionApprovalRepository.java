package io.agentengine.core.task.repository;

import io.agentengine.core.task.domain.ExecutionApproval;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionApprovalRepository extends JpaRepository<ExecutionApproval, UUID> {
    boolean existsByOwnerIdAndTaskIdAndPlanIdAndPlanVersionAndScopeHashAndRulesHashAndRevokedFalse(UUID ownerId, UUID taskId, UUID planId, long planVersion, String scopeHash, String rulesHash);
}
