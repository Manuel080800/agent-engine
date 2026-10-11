package io.agentengine.core.task.record;

public record PlanRequestRecord(String name, String taskId, String original, java.util.List<String> scope, java.util.List<String> acceptanceCriteria, String amendmentOf) {
}
