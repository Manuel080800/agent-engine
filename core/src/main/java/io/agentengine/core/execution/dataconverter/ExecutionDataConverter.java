package io.agentengine.core.execution.dataconverter;

import io.agentengine.core.common.util.JsonCodec;
import io.agentengine.core.execution.domain.ExecutionEvent;
import io.agentengine.core.execution.domain.ExecutionEvidence;
import io.agentengine.core.execution.record.TransitionRecord;
import io.agentengine.core.model.domain.ModelConsent;
import io.agentengine.core.task.domain.ExecutionApproval;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class ExecutionDataConverter {
    private final JsonCodec json;

    public ExecutionDataConverter(JsonCodec json) {
        this.json = json;
    }

    public ExecutionApproval toApprovalEntity(UUID id, UUID owner, UUID task, UUID plan, long version, Object scope, String rules) {
        var entity = new ExecutionApproval();
        entity.setId(id);
        entity.setOwnerId(owner);
        entity.setTaskId(task);
        entity.setPlanId(plan);
        entity.setPlanVersion(version);
        entity.setScopeHash(scope.toString());
        entity.setRulesHash(rules);
        return entity;
    }

    public ModelConsent toConsentEntity(UUID id, UUID owner, UUID task, UUID block, UUID delegation, UUID plan, long version, String model, String effort, Instant expires) {
        var entity = new ModelConsent();
        entity.setId(id);
        entity.setOwnerId(owner);
        entity.setTaskId(task);
        entity.setBlockId(block);
        entity.setDelegationId(delegation);
        entity.setPlanId(plan);
        entity.setPlanVersion(version);
        entity.setModelId(model);
        entity.setEffort(effort);
        entity.setExpiresAt(expires);
        return entity;
    }

    public ExecutionEvent toEventEntity(UUID owner, String client, String hash, TransitionRecord dto) {
        var entity = new ExecutionEvent();
        entity.setId(dto.eventId());
        entity.setOwnerId(owner);
        entity.setTaskId(dto.taskId());
        entity.setClientId(client);
        entity.setLocalSequence(dto.localSequence());
        entity.setEventType(dto.to());
        entity.setPayloadHash(hash);
        entity.setData(json.fields(dto));
        return entity;
    }

    public ExecutionEvidence toEvidenceEntity(UUID owner, TransitionRecord dto) {
        var entity = new ExecutionEvidence();
        entity.setId(UUID.randomUUID());
        entity.setOwnerId(owner);
        entity.setTaskId(dto.taskId());
        entity.setBlockId(dto.blockId());
        entity.setEventId(dto.eventId());
        entity.setKind(dto.evidence().kind());
        entity.setData(json.fields(dto.evidence()));
        return entity;
    }
}
