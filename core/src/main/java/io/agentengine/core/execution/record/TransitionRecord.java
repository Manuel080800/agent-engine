package io.agentengine.core.execution.record;

import java.util.*;

public record TransitionRecord(UUID eventId, UUID taskId, UUID blockId, long expectedRevision, long localSequence, String to, EvidenceRecord evidence, String nextAction, UUID consentId, UUID delegationId, String modelId, String effort, UUID findingId) {
}
