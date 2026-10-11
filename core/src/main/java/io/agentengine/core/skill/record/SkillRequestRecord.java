package io.agentengine.core.skill.record;

import jakarta.validation.constraints.*;

public record SkillRequestRecord(@NotBlank @Size(max = 200) String name, @NotBlank @Size(max = 16000) String text, Boolean enabled) {
}
