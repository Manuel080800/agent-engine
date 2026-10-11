package io.agentengine.core.task.workflow;

import io.agentengine.core.task.enums.TaskState;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class WorkflowTransitionsTest {
    @Test
    void completedTasksCannotResumeOrSuspend() {
        // A completed task remains terminal even when suspension is requested.

        for (var state : TaskState.values()) {
            assertThrows(IllegalArgumentException.class, () -> WorkflowTransitions.requireTransition(TaskState.COMPLETED, state));
        }
    }

    @Test
    void executionCannotSkipProgressiveVerification() {
        // Execution must enter verification before final review or completion.
        assertThrows(IllegalArgumentException.class, () -> WorkflowTransitions.requireTransition(TaskState.EXECUTING, TaskState.COMPLETED));
        assertThrows(IllegalArgumentException.class, () -> WorkflowTransitions.requireTransition(TaskState.EXECUTING, TaskState.FINAL_REVIEW));
        assertDoesNotThrow(() -> WorkflowTransitions.requireTransition(TaskState.EXECUTING, TaskState.VERIFYING));
    }

    @Test
    void differentFindingsCannotEvadeGlobalBudget() {
        // Six accumulated corrections block another attempt regardless of finding or
        // block count.
        assertThrows(IllegalArgumentException.class, () -> new CorrectionBudget(0, 0, 6, 0).requireAnotherAttempt(false));
        assertThrows(IllegalArgumentException.class, () -> new CorrectionBudget(2, 0, 0, 0).requireAnotherAttempt(false));
        assertThrows(IllegalArgumentException.class, () -> new CorrectionBudget(0, 3, 0, 0).requireAnotherAttempt(false));
        assertThrows(IllegalArgumentException.class, () -> new CorrectionBudget(0, 0, 0, 2).requireAnotherAttempt(true));
    }

}
