package io.agentengine.core.model.repository;

import io.agentengine.core.model.domain.ModelConsent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModelConsentRepository extends JpaRepository<ModelConsent, UUID> {

}
