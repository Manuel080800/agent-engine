package io.agentengine.core.task.workflow;

public record CorrectionBudget(int finding, int block, int task, int finalRounds) {
    public void requireAnotherAttempt(boolean finalReview) {
        if (finding >= 2 || block >= 3 || task >= 6 || (finalReview && finalRounds >= 2)) {
            throw new IllegalArgumentException("Correction budget exhausted");
        }
    }
}
