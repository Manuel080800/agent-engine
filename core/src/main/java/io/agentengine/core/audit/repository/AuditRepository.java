package io.agentengine.core.audit.repository;

import io.agentengine.core.audit.domain.AuditEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<AuditEntry, UUID> {
    List<AuditEntry> findByOwnerIdOrderByCreatedAtDescIdAsc(UUID ownerId, Pageable pageable);
}
