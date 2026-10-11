package io.agentengine.core.task.repository;

import io.agentengine.core.task.domain.ReviewFinding;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewFindingRepository extends JpaRepository<ReviewFinding, UUID> {
    Optional<ReviewFinding> findByIdAndOwnerIdAndDeletedFalse(UUID id, UUID ownerId);

    @Query("SELECT resource FROM ReviewFinding resource WHERE resource.ownerId = :ownerId AND (:projectId IS NULL OR resource.projectId = :projectId) AND (:includeDeleted = true OR resource.deleted = false) ORDER BY resource.updatedAt DESC, resource.id ASC")
    List<ReviewFinding> findByParams(@Param("ownerId") UUID ownerId, @Param("projectId") UUID projectId, @Param("includeDeleted") boolean includeDeleted, Pageable pageable);
}
