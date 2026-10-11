package io.agentengine.core.common.factory;

import io.agentengine.core.auth.domain.LocalClient;
import io.agentengine.core.common.domain.VersionedResource;
import io.agentengine.core.common.enums.ResourceKind;
import io.agentengine.core.execution.domain.Checkpoint;
import io.agentengine.core.memory.domain.Memory;
import io.agentengine.core.memory.domain.MemoryProposal;
import io.agentengine.core.model.domain.Model;
import io.agentengine.core.model.domain.ModelPolicy;
import io.agentengine.core.profile.domain.Profile;
import io.agentengine.core.project.domain.Project;
import io.agentengine.core.rule.domain.Rule;
import io.agentengine.core.skill.domain.Skill;
import io.agentengine.core.task.domain.ReviewFinding;
import io.agentengine.core.task.domain.Task;
import io.agentengine.core.task.domain.TaskBlock;
import io.agentengine.core.task.domain.TaskPlan;

public final class ResourceEntityFactory {
    private ResourceEntityFactory() {
    }

    public static VersionedResource create(ResourceKind kind) {
        return switch (kind) {
            case PROJECT -> new Project();
            case PROFILE -> new Profile();
            case RULE -> new Rule();
            case SKILL -> new Skill();
            case MODEL -> new Model();
            case MODEL_POLICY -> new ModelPolicy();
            case MEMORY -> new Memory();
            case MEMORY_PROPOSAL -> new MemoryProposal();
            case TASK -> new Task();
            case PLAN -> new TaskPlan();
            case BLOCK -> new TaskBlock();
            case CHECKPOINT -> new Checkpoint();
            case REVIEW_FINDING -> new ReviewFinding();
            case LOCAL_CLIENT -> new LocalClient();
        };
    }
}
