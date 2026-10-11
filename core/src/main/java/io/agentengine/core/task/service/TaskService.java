package io.agentengine.core.task.service;

import io.agentengine.core.common.record.ResourceResponseRecord;
import io.agentengine.core.common.record.ResourceWriteRecord;
import io.agentengine.core.execution.record.*;

import java.util.*;

public interface TaskService {
    ResourceResponseRecord create(ResourceWriteRecord<Map<String, Object>> write);

    ResourceResponseRecord createChild(String kind, ResourceWriteRecord<Map<String, Object>> write);

    ResourceResponseRecord propose(ResourceWriteRecord<Map<String, Object>> write);

    Map<String, Object> context(UUID project, UUID taskId);

    UUID approve(UUID taskId, long version);

    UUID consent(UUID taskId, UUID blockId, UUID delegationId, String model, String effort);

    ResourceResponseRecord transition(TransitionRecord event);

    ResourceResponseRecord decideProposal(UUID id, long version, String decision, String editedText);
}
