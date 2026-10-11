package io.agentengine.core.auth.repository;

import io.agentengine.core.auth.domain.UserInvitation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserInvitationRepository extends JpaRepository<UserInvitation, UUID> {

}
