package io.agentengine.core.model.service;

import io.agentengine.core.common.enums.DomainError;
import io.agentengine.core.common.exception.DomainException;
import io.agentengine.core.common.record.*;
import io.agentengine.core.common.security.IdentitySecurity;
import io.agentengine.core.common.service.ResourceService;
import io.agentengine.core.model.record.DelegationRequestRecord;
import io.agentengine.core.task.repository.dao.TaskDAO;

import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DelegationServiceImpl implements DelegationService {
    private final ModelConfigurationService models;
    private final ResourceService resources;
    private final TaskDAO taskDAO;

    public DelegationServiceImpl(ResourceService resources, TaskDAO taskDAO, ModelConfigurationService models) {
        this.models = models;
        this.resources = resources;
        this.taskDAO = taskDAO;
    }

    @Transactional
    @Override
    public Map<String, Object> start(DelegationRequestRecord request) {
        IdentitySecurity.coordinator();

        if (Objects.isNull(request.delegationId()) || !Set.of("analyzer", "planner", "executor", "tester", "reviewer", "corrector", "final-reviewer").contains(request.role())) {
            throw new IllegalArgumentException("Registered role and delegation identifier required");
        }

        var task = resources.get("tasks", request.taskId(), true);

        if (task.version() != request.expectedRevision() || task.data().get("activeDelegation") != null || !Set.of("EXECUTING", "VERIFYING", "CORRECTING", "FINAL_REVIEW", "FINAL_CORRECTING").contains(task.data().get("state"))) {
            throw new DomainException(DomainError.CONFLICT, "Delegation version/state conflict");
        }

        var block = resources.get("blocks", request.blockId(), false);

        if (!task.id().toString().equals(block.data().get("taskId"))) {
            throw new IllegalArgumentException("Block relationship mismatch");
        }

        var selection = models.selectByRoleAndProjectId(request.role(), task.projectId());
        var model = selection.model().modelId();
        var effort = selection.effort();
        var plan = resources.get("plans", UUID.fromString(task.data().get("planId").toString()), false);

        if (selection.model().requiresVerification()) {
            var consumed = taskDAO.consumeConsent(request.consentId(), IdentitySecurity.owner(), task.id(), block.id(), request.delegationId(), plan.id(), plan.version(), model, effort);

            if (consumed != 1) {
                throw new DomainException(DomainError.FORBIDDEN, "Individual model verification unavailable");
            }
        }

        var data = new HashMap<>(task.data());
        data.put("activeDelegation", request.delegationId().toString());
        var updated = resources.update("tasks", task.id(), new ResourceWriteRecord<>(task.projectId(), task.version(), data));
        resources.audit("START_DELEGATION_" + request.role(), task.id());
        return Map.of("delegationId", request.delegationId(), "modelId", model, "effort", effort, "revision", updated.version(), "scope", block.data().get("scope"), "criteria", block.data().get("criteria"), "planId", plan.id(), "planVersion", plan.version());
    }

    @Transactional
    @Override
    public ResourceResponseRecord finish(UUID taskId, UUID delegationId, long revision) {
        IdentitySecurity.coordinator();
        var task = resources.get("tasks", taskId, true);

        if (task.version() != revision || !delegationId.toString().equals(task.data().get("activeDelegation"))) {
            throw new DomainException(DomainError.CONFLICT, "Active delegation mismatch");
        }

        var data = new HashMap<>(task.data());
        data.remove("activeDelegation");
        resources.audit("FINISH_DELEGATION", task.id());
        return resources.update("tasks", task.id(), new ResourceWriteRecord<>(task.projectId(), revision, data));
    }
}
