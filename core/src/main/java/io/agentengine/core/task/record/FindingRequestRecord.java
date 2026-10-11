package io.agentengine.core.task.record;

public record FindingRequestRecord(String name, String taskId, String blockId, String requirement, String file, String signature, String severity) {
}
