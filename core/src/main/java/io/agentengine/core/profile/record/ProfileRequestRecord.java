package io.agentengine.core.profile.record;

import jakarta.validation.constraints.*;

public record ProfileRequestRecord(@NotBlank @Size(max = 200) String name, @Size(max = 4000) String description, Boolean active) {
}
