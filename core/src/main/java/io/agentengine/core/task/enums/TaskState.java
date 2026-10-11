package io.agentengine.core.task.enums;

public enum TaskState {
    RECEIVED,
    CONTEXT_READY,
    ANALYZING,
    PLANNED,
    WAITING_APPROVAL,
    EXECUTING,
    VERIFYING,
    CORRECTING,
    FINAL_REVIEW,
    FINAL_CORRECTING,
    SUSPENDED,
    BLOCKED,
    COMPLETED
}
