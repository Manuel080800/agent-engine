package io.agentengine.core.profile.repository;

import io.agentengine.core.profile.domain.Profile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {
    Optional<Profile> findByIdAndOwnerIdAndDeletedFalse(UUID id, UUID ownerId);

    @Query("SELECT resource FROM Profile resource WHERE resource.ownerId = :ownerId AND (:projectId IS NULL OR resource.projectId = :projectId) AND (:includeDeleted = true OR resource.deleted = false) ORDER BY resource.updatedAt DESC, resource.id ASC")
    List<Profile> findByParams(@Param("ownerId") UUID ownerId, @Param("projectId") UUID projectId, @Param("includeDeleted") boolean includeDeleted, Pageable pageable);
}
