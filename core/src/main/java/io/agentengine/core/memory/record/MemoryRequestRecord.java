package io.agentengine.core.memory.record;

import jakarta.validation.constraints.*;

public record MemoryRequestRecord(@NotBlank String name, @NotBlank @Size(max = 16000) String text, @NotBlank String type, String source, String trust, String expiresAt) {
}
