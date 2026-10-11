package io.agentengine.core.user.record;

import java.time.Instant;
import java.util.UUID;

public record UserAccountRecord(UUID id, String email, boolean administrator, boolean enabled, Instant createdAt) {
}
