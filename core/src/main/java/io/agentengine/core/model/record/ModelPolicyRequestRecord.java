package io.agentengine.core.model.record;

import jakarta.validation.constraints.*;

public record ModelPolicyRequestRecord(@NotBlank String name, @NotBlank String role, @NotBlank String modelId, @NotBlank String effort) {
}
