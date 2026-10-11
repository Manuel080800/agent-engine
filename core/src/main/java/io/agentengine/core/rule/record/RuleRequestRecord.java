package io.agentengine.core.rule.record;

import jakarta.validation.constraints.*;

public record RuleRequestRecord(@NotBlank @Size(max = 200) String name, @NotBlank @Size(max = 16000) String text, @NotBlank String scope, String module, Boolean required, Integer priority, String source, String expiresAt, Boolean enabled) {
}
