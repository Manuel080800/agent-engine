package io.agentengine.core.model.record;

import jakarta.validation.constraints.*;

import java.util.List;

public record ModelRequestRecord(@NotBlank String name, @NotBlank String modelId, @NotEmpty List<String> efforts, Integer priority, Boolean enabled, Boolean requiresVerification) {
}
