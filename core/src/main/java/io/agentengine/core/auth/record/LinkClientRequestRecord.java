package io.agentengine.core.auth.record;

import jakarta.validation.constraints.*;

public record LinkClientRequestRecord(@NotBlank String clientId, @NotBlank String name) {
}
