package io.agentengine.core.auth.record;

import jakarta.validation.constraints.*;

public record RegistrationRequestRecord(@Email @NotBlank String email, @NotBlank @Size(min = 12, max = 100) String password, @NotBlank String token) {
}
