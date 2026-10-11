package io.agentengine.core.model.record;

import java.util.List;

public record ModelConfigurationRecord(String modelId, List<String> efforts, int priority, boolean requiresVerification) {
}
