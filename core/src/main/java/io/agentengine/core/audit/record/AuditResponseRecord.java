package io.agentengine.core.audit.record;

import java.time.Instant;
import java.util.UUID;

public record AuditResponseRecord(String operation, UUID resourceId, Instant createdAt) {
}
