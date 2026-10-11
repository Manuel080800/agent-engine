package io.agentengine.core.task.record;

public record BlockRequestRecord(String name, String taskId, Integer order, java.util.List<String> scope, java.util.List<String> criteria) {
}
