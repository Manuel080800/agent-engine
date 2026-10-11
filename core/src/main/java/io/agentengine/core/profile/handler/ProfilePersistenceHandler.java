package io.agentengine.core.profile.handler;

import io.agentengine.core.common.enums.ResourceKind;
import io.agentengine.core.common.handler.AbstractResourcePersistenceHandler;
import io.agentengine.core.profile.domain.Profile;
import io.agentengine.core.profile.repository.ProfileRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class ProfilePersistenceHandler extends AbstractResourcePersistenceHandler<Profile> {
    private final ProfileRepository repository;

    public ProfilePersistenceHandler(ProfileRepository repository) {
        super(ResourceKind.PROFILE, Profile.class, repository);
        this.repository = repository;
    }

    @Override
    public Optional<Profile> findByOwnerIdAndId(UUID ownerId, UUID id) {
        return repository.findByIdAndOwnerIdAndDeletedFalse(id, ownerId);
    }

    @Override
    public List<Profile> list(UUID ownerId, UUID projectId, boolean includeDeleted, Pageable pageable) {
        return repository.findByParams(ownerId, projectId, includeDeleted, pageable);
    }
}
