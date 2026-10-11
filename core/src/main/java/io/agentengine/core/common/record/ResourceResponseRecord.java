package io.agentengine.core.common.record;

import java.util.Map;
import java.util.UUID;

public record ResourceResponseRecord(UUID id, UUID projectId, long version, boolean deleted, Map<String, Object> data) {
}
