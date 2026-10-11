package io.agentengine.core.task.workflow;

import io.agentengine.core.task.enums.TaskState;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class WorkflowTransitions {
    private static final Map<TaskState, Set<TaskState>> NEXT = createTransitions();

    private WorkflowTransitions() {
    }

    public static void requireTransition(TaskState from, TaskState to) {
        var terminal = from == TaskState.COMPLETED;
        var interruption = to == TaskState.SUSPENDED || to == TaskState.BLOCKED;
        var allowed = interruption || NEXT.get(from).contains(to);

        if (terminal || from == to || !allowed) {
            throw new IllegalArgumentException("Invalid workflow transition");
        }
    }

    private static Map<TaskState, Set<TaskState>> createTransitions() {
        var transitions = new EnumMap<TaskState, Set<TaskState>>(TaskState.class);
        transitions.put(TaskState.RECEIVED, Set.of(TaskState.CONTEXT_READY));
        transitions.put(TaskState.CONTEXT_READY, Set.of(TaskState.ANALYZING));
        transitions.put(TaskState.ANALYZING, Set.of(TaskState.PLANNED));
        transitions.put(TaskState.PLANNED, Set.of(TaskState.WAITING_APPROVAL, TaskState.EXECUTING));
        transitions.put(TaskState.WAITING_APPROVAL, Set.of(TaskState.EXECUTING));
        transitions.put(TaskState.EXECUTING, Set.of(TaskState.VERIFYING));
        transitions.put(TaskState.VERIFYING, Set.of(TaskState.CORRECTING, TaskState.EXECUTING, TaskState.FINAL_REVIEW));
        transitions.put(TaskState.CORRECTING, Set.of(TaskState.VERIFYING));
        transitions.put(TaskState.FINAL_REVIEW, Set.of(TaskState.FINAL_CORRECTING, TaskState.COMPLETED));
        transitions.put(TaskState.FINAL_CORRECTING, Set.of(TaskState.FINAL_REVIEW));
        transitions.put(TaskState.SUSPENDED, Set.of(TaskState.CONTEXT_READY));
        transitions.put(TaskState.BLOCKED, Set.of(TaskState.CONTEXT_READY));
        transitions.put(TaskState.COMPLETED, Set.of());
        return Map.copyOf(transitions);
    }
}
