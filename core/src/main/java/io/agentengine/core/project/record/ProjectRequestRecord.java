package io.agentengine.core.project.record;

import jakarta.validation.constraints.*;

public record ProjectRequestRecord(@NotBlank @Size(max = 200) String name, @Size(max = 4000) String description, @Size(max = 2000) String reference) {
}
