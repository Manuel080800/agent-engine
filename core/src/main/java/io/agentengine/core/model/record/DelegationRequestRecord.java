package io.agentengine.core.model.record;

import java.util.UUID;

public record DelegationRequestRecord(UUID taskId, UUID blockId, UUID delegationId, UUID consentId, long expectedRevision, String role) {
}
