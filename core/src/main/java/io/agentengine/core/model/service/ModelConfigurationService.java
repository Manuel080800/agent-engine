package io.agentengine.core.model.service;

import io.agentengine.core.model.record.ModelConfigurationRecord;
import io.agentengine.core.model.record.ModelSelectionRecord;
import java.util.UUID;
import java.util.List;

public interface ModelConfigurationService {
    List<ModelConfigurationRecord> listByProjectId(UUID projectId);

    ModelConfigurationRecord requireAvailableByModelIdAndEffortAndProjectId(String modelId, String effort, UUID projectId);

    ModelSelectionRecord selectByRoleAndProjectId(String role, UUID projectId);
}
