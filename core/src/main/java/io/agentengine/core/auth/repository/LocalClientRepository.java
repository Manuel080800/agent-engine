package io.agentengine.core.auth.repository;

import io.agentengine.core.auth.domain.LocalClient;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LocalClientRepository extends JpaRepository<LocalClient, UUID> {
    Optional<LocalClient> findByIdAndOwnerIdAndDeletedFalse(UUID id, UUID ownerId);

    @Query("SELECT resource FROM LocalClient resource WHERE resource.ownerId = :ownerId AND (:projectId IS NULL OR resource.projectId = :projectId) AND (:includeDeleted = true OR resource.deleted = false) ORDER BY resource.updatedAt DESC, resource.id ASC")
    List<LocalClient> findByParams(@Param("ownerId") UUID ownerId, @Param("projectId") UUID projectId, @Param("includeDeleted") boolean includeDeleted, Pageable pageable);
}
