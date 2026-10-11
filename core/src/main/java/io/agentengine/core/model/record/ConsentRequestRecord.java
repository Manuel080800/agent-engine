package io.agentengine.core.model.record;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConsentRequestRecord(@NotNull UUID blockId, @NotNull UUID delegationId, String modelId, String effort) {
}
