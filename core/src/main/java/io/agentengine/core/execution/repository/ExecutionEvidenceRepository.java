package io.agentengine.core.execution.repository;

import io.agentengine.core.execution.domain.ExecutionEvidence;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionEvidenceRepository extends JpaRepository<ExecutionEvidence, UUID> {

}
