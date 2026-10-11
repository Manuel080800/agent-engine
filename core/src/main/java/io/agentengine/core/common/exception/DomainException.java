package io.agentengine.core.common.exception;

import io.agentengine.core.common.enums.DomainError;

public class DomainException extends RuntimeException {
    private final DomainError error;

    public DomainException(DomainError error) {
        this(error, "Request rejected");
    }

    public DomainException(DomainError error, String message) {
        super(message);
        this.error = error;
    }

    public DomainError getError() {
        return error;
    }
}
