package io.agentengine.core.common.record;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ResourceWriteRecord<T>(UUID projectId, Long version, @Valid @NotNull T data) {
}
