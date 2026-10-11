package io.agentengine.core.common.handler;

import io.agentengine.core.common.util.JsonCodec;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class ResourceValidationHandler {
    private static final Map<String, Set<String>> FIELDS = Map.ofEntries(Map.entry("projects", Set.of("name", "description", "reference")), Map.entry("profiles", Set.of("name", "description", "active")), Map.entry("rules", Set.of("name", "text", "scope", "module", "required", "priority", "source", "expiresAt", "enabled")), Map.entry("skills", Set.of("name", "text", "enabled")), Map.entry("models", Set.of("name", "modelId", "efforts", "priority", "enabled", "requiresVerification")), Map.entry("model-policies", Set.of("name", "role", "modelId", "effort")), Map.entry("memories", Set.of("name", "text", "type", "source", "trust", "expiresAt")), Map.entry("memory-proposals", Set.of("name", "text", "type", "source", "justification", "taskId", "status")), Map.entry("tasks", Set.of("name", "objective", "level", "state", "planId", "scopeHash", "rulesHash", "corrections", "finalRounds", "activeDelegation", "skillsSnapshot")), Map.entry("plans", Set.of("name", "taskId", "original", "hash", "scope", "acceptanceCriteria", "amendmentOf")), Map.entry("blocks", Set.of("name", "taskId", "order", "scope", "criteria", "state", "corrections")), Map.entry("checkpoints", Set.of("name", "taskId", "blockId", "state", "nextAction", "planVersion", "rulesHash", "skillsHash")), Map.entry("reviews", Set.of("name", "taskId", "blockId", "requirement", "file", "signature", "severity", "status", "attempts")), Map.entry("local-clients", Set.of("name", "clientId", "enabled")));

    private final JsonCodec json;

    public ResourceValidationHandler(JsonCodec json) {
        this.json = json;
    }

    public void validate(String kind, Map<String, Object> data) {
        if (!FIELDS.containsKey(kind) || Objects.isNull(data) || !FIELDS.get(kind).containsAll(data.keySet())) {
            throw new IllegalArgumentException("Unrecognized resource fields");
        }

        var encoded = json.encode(data);

        if (encoded.length() > 32768 || encoded.matches("(?s).*(-----BEGIN .*PRIVATE" + " KEY|sk-proj-[A-Za-z0-9]+|(?i:password|access_token|refresh_token)\\s*[=:]\\s*[^\\s]+).*")) {
            throw new IllegalArgumentException("Content exceeds policy limits or contains credential material");
        }

        if (kind.equals("model-policies")) {
            if (!Set.of("analyzer", "planner", "executor", "tester", "reviewer", "corrector", "final-reviewer").contains(data.get("role"))) {
                throw new IllegalArgumentException("Unknown agent role");
            }

            if (!(data.get("modelId") instanceof String modelId) || modelId.isBlank() || !(data.get("effort") instanceof String effort) || effort.isBlank()) {
                throw new IllegalArgumentException("Model and effort required");
            }
        }

        if (kind.equals("rules")) {
            if (!Set.of("user", "personal-project", "global", "repository", "skill", "default").contains(data.get("scope"))) {
                throw new IllegalArgumentException("Unknown rule scope");
            }

            if (!(data.get("text") instanceof String text) || text.isBlank()) {
                throw new IllegalArgumentException("Rule text required");
            }
        }

        if (kind.equals("models")) {
            if (!(data.get("modelId") instanceof String modelId) || modelId.isBlank() || !(data.get("efforts") instanceof List<?> efforts) || efforts.isEmpty() || efforts.stream().anyMatch(effort -> !(effort instanceof String text) || text.isBlank())) {
                throw new IllegalArgumentException("Model and valid efforts required");
            }

            for (var flag : List.of("enabled", "requiresVerification")) {
                if (data.containsKey(flag) && !(data.get(flag) instanceof Boolean)) {
                    throw new IllegalArgumentException("Model flags must be boolean");
                }
            }
        }
    }

}
