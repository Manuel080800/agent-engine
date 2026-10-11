package io.agentengine.core.common.handler;

import io.agentengine.core.common.enums.DomainError;
import io.agentengine.core.common.enums.ResourceKind;
import io.agentengine.core.common.exception.DomainException;

import java.util.Objects;

public final class ResourceKindHandler {
    private ResourceKindHandler() {
    }

    public static ResourceKind resolve(String name) {
        if (Objects.isNull(name)) {
            throw new DomainException(DomainError.NOT_FOUND, "Unknown resource kind");
        }

        return switch (name) {
            case "projects" -> ResourceKind.PROJECT;
            case "profiles" -> ResourceKind.PROFILE;
            case "rules" -> ResourceKind.RULE;
            case "skills" -> ResourceKind.SKILL;
            case "models" -> ResourceKind.MODEL;
            case "model-policies" -> ResourceKind.MODEL_POLICY;
            case "memories" -> ResourceKind.MEMORY;
            case "memory-proposals" -> ResourceKind.MEMORY_PROPOSAL;
            case "tasks" -> ResourceKind.TASK;
            case "plans" -> ResourceKind.PLAN;
            case "blocks" -> ResourceKind.BLOCK;
            case "checkpoints" -> ResourceKind.CHECKPOINT;
            case "reviews" -> ResourceKind.REVIEW_FINDING;
            case "local-clients" -> ResourceKind.LOCAL_CLIENT;
            default -> throw new DomainException(DomainError.NOT_FOUND, "Unknown resource kind");
        };
    }
}
