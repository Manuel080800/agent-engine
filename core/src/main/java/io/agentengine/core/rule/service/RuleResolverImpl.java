package io.agentengine.core.rule.service;

import io.agentengine.core.common.record.ResourceResponseRecord;

import java.time.Instant;
import java.util.*;

import org.springframework.stereotype.Service;

@Service
public class RuleResolverImpl implements RuleResolver {
    private static final Map<String, Integer> PRIORITY = Map.of("user", 0, "personal-project", 1, "global", 2, "repository", 3, "skill", 4, "default", 5);

    @Override
    public List<ResourceResponseRecord> resolve(List<ResourceResponseRecord> rules) {
        var ordered = rules.stream().filter(r -> !Boolean.FALSE.equals(r.data().get("enabled"))).filter(r -> r.data().get("expiresAt") == null || Instant.parse(r.data().get("expiresAt").toString()).isAfter(Instant.now())).sorted(Comparator.<ResourceResponseRecord>comparingInt(r -> PRIORITY.getOrDefault(r.data().get("scope"), 5)).thenComparing(r -> r.id().toString())).toList();
        var resolved = new LinkedHashMap<String, ResourceResponseRecord>();

        for (var rule : ordered) {
            var key = rule.data().getOrDefault("name", rule.id().toString()) + ":" + rule.data().getOrDefault("module", "");
            var previous = resolved.putIfAbsent(key, rule);

            if (Objects.nonNull(previous) && Objects.equals(previous.data().get("scope"), rule.data().get("scope")) && Boolean.TRUE.equals(previous.data().get("required")) && Boolean.TRUE.equals(rule.data().get("required")) && !Objects.equals(previous.data().get("text"), rule.data().get("text"))) {
                throw new IllegalArgumentException("Contradictory mandatory rules at the same priority");
            }
        }

        return List.copyOf(resolved.values());
    }
}
