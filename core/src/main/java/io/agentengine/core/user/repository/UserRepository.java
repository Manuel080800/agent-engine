package io.agentengine.core.user.repository;

import io.agentengine.core.user.domain.UserAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByEmailAndEnabledTrue(String email);

    Optional<UserAccount> findByIdAndEnabledTrue(UUID id);
}
